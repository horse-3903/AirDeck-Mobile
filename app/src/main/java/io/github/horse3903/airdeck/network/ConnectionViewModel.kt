package io.github.horse3903.airdeck.network

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kitswas.VGP_Data_Exchange.GamepadReading
import io.github.horse3903.airdeck.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.LinkedBlockingQueue

class ConnectionViewModel(private val onConnectionSuccess: suspend (String, Int) -> Unit = { _, _ -> }) :
    ViewModel() {

    private val tag = this::class.java.simpleName
    private var clientSocket: Socket? = null // Internal storage for the socket

    /** Sent to the PC right after connecting so it can show this phone's name. */
    var deviceHello: String = ""
    var lastIpAddress: String = ""
        private set
    var lastPort: Int = -1
        private set

    // Replace gamepad state queue with a general command queue
    private val commandQueue = LinkedBlockingQueue<NetworkCommand>()

    // Job for the command processor
    private var commandProcessorJob: Job? = null

    /** What the game asked the controller to do: the large and small motor strengths, 0 to 255 each. */
    private val _rumble = MutableSharedFlow<IntArray>(extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val rumble: SharedFlow<IntArray> = _rumble

    // Expose screen UI state
    private val _uiState = MutableStateFlow(ConnectionState())
    val uiState: StateFlow<ConnectionState> = _uiState.asStateFlow()

    /**
     * Enqueues a network command for processing.
     * This is the core method for all network operations.
     *
     * @param command The network command to enqueue
     */
    private fun enqueueCommand(command: NetworkCommand) {
        // Don't enqueue send commands if disconnected or disconnecting
        // But always allow Connect and Disconnect commands
        if (command !is NetworkCommand.Connect &&
            command !is NetworkCommand.Disconnect &&
            (_uiState.value.error != null || !_uiState.value.connected)
        ) {
            Log.d(tag, "Skipping command enqueue while disconnected: $command")
            return
        }

        commandQueue.offer(command)

        // Start the command processor if not already running
        if (commandProcessorJob == null || commandProcessorJob?.isActive != true) {
            startCommandProcessor()
        }
    }

    /**
     * Enqueues a gamepad state for sending.
     * This is the public API for sending gamepad updates.
     *
     * @param gamepadState The gamepad state to enqueue
     */
    fun enqueueGamepadState(gamepadState: GamepadReading) {
        enqueueCommand(NetworkCommand.SendGamepadState(gamepadState))
    }

    /**
     * Enqueues a string to send.
     * This is primarily for testing purposes.
     *
     * @param string The string to send
     */
    @Suppress("unused")
    fun enqueueString(string: String) {
        enqueueCommand(NetworkCommand.SendString(string))
    }

    /**
     * Connect to a server at the given IP address and port.
     * This now enqueues a connection command.
     */
    fun connect(ipAddress: String, port: Int) {
        val current = _uiState.value
        // Already on this server or already dialling it: a second socket would show up on the PC as a second controller
        if ((current.connected || current.isConnecting) && current.ipAddress == ipAddress && current.port == port) {
            _uiState.update { it.copy(userStopped = false) }
            return
        }
        lastIpAddress = ipAddress
        lastPort = port
        _uiState.update {
            it.copy(
                isConnecting = true,
                ipAddress = ipAddress,
                port = port,
                error = null,
                connected = false,
                userStopped = false
            )
        }

        enqueueCommand(NetworkCommand.Connect(ipAddress, port))
    }

    /**
     * Disconnect from the server.
     * This now enqueues a disconnect command.
     */
    fun disconnect() {
        // Set at once, so a reconnect loop sees it before the disconnect itself is processed
        _uiState.update { it.copy(userStopped = true) }
        enqueueCommand(NetworkCommand.Disconnect)
    }

    /**
     * Starts the command processor job that executes network commands.
     * Uses a blocking queue implementation to efficiently wait for new commands.
     */
    private fun startCommandProcessor() {
        // Cancel existing job if any
        commandProcessorJob?.cancel()

        // Start new processor job
        commandProcessorJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    // This will block until a command is available
                    // Process based on command type
                    when (val command = commandQueue.take()) {
                        is NetworkCommand.Connect -> processConnectCommand(command)
                        is NetworkCommand.SendGamepadState -> processSendGamepadStateCommand(command)
                        is NetworkCommand.SendString -> processSendStringCommand(command)
                        is NetworkCommand.Disconnect -> {
                            processDisconnectCommand()
                            // Exit the loop after disconnect
                            break
                        }
                    }
                } catch (e: InterruptedException) {
                    // The coroutine was canceled, just exit the loop
                    Log.d(tag, "Command processor interrupted: ${e.message}")
                    break
                } catch (e: CancellationException) {
                    // The coroutine was canceled, just exit the loop
                    Log.d(tag, "Command processor canceled")
                    break
                } catch (e: Exception) {
                    // Catch any other exceptions to prevent the loop from crashing
                    Log.e(tag, "Command processor error: ${e.message}", e)
                    _uiState.update {
                        it.copy(
                            error = "Unexpected error: ${e.message ?: "Unknown"}",
                            connected = false
                        )
                    }

                    // Force disconnect on fatal errors
                    try {
                        processDisconnectCommand()
                    } catch (e2: Exception) {
                        Log.e(tag, "Error during emergency disconnect: ${e2.message}")
                    }
                    break
                }
            }

            Log.d(tag, "Command processor stopped")
        }
    }

    /**
     * Process a connect command.
     */
    private fun processConnectCommand(command: NetworkCommand.Connect) {
        if (_uiState.value.userStopped) return
        // A repeated request while already connected there changes nothing
        if (clientSocket != null && _uiState.value.connected &&
            _uiState.value.ipAddress == command.ipAddress && _uiState.value.port == command.port
        ) return
        // Never leave an earlier socket open: the PC would keep it as an extra controller
        clientSocket?.let { old ->
            clientSocket = null
            try {
                old.close()
            } catch (_: IOException) {
            }
        }
        try {
            // Create and configure socket
            val socket = Socket()
            socket.tcpNoDelay = true
            socket.setPerformancePreferences(1, 2, 0)
            socket.trafficClass = 0x10 // IPTOS_LOWDELAY
            // Set timeout to 5 seconds
            val timeout = 5000 // in milliseconds

            try {
                socket.connect(java.net.InetSocketAddress(command.ipAddress, command.port), timeout)
                clientSocket = socket // Store the connected socket
                if (deviceHello.isNotEmpty()) {
                    try {
                        socket.getOutputStream().write(deviceHello.toByteArray())
                    } catch (e: IOException) {
                        Log.w(tag, "Could not introduce this device: ${e.message}")
                    }
                }
                startReader(socket)

                _uiState.update {
                    it.copy(
                        connected = true,
                        isConnecting = false,
                        error = null
                    )
                }
                Log.d(tag, "Connected: $clientSocket")

                // Invoke callback on successful connection
                viewModelScope.launch {
                    try {
                        onConnectionSuccess(command.ipAddress, command.port)
                    } catch (e: Exception) {
                        Log.e(tag, "Error saving connection credentials: ${e.message}", e)
                    }
                }
            } catch (e: IOException) {
                Log.e(tag, "Connection failed: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        connected = false,
                        isConnecting = false,
                        error = e.message ?: "Unknown connection error"
                    )
                }
                socket.close() // Ensure socket is closed on error
            }
        } catch (e: Exception) {
            Log.e(tag, "Connect error: ${e.message}", e)
            _uiState.update {
                it.copy(
                    connected = false,
                    isConnecting = false,
                    error = "Connect error: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }

    /**
     * Reads what the PC sends back: currently only which controller slot this phone got.
     * When the PC closes the connection the state is marked as lost.
     */
    private fun startReader(socket: Socket) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val reader = socket.getInputStream().bufferedReader()
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.startsWith("VGPRUMBLE:")) {
                        val parts = line.removePrefix("VGPRUMBLE:").trim().split(",")
                        val large = parts.getOrNull(0)?.toIntOrNull() ?: 0
                        val weak = parts.getOrNull(1)?.toIntOrNull() ?: 0
                        _rumble.tryEmit(intArrayOf(large.coerceIn(0, 255), weak.coerceIn(0, 255)))
                    } else if (line.startsWith("VGPSLOT:")) {
                        line.removePrefix("VGPSLOT:").trim().toIntOrNull()?.let { slot ->
                            _uiState.update { it.copy(controllerSlot = slot) }
                        }
                    }
                }
            } catch (e: IOException) {
                Log.d(tag, "Reader stopped: ${e.message}")
            }
            _rumble.tryEmit(intArrayOf(0, 0))
            if (clientSocket === socket) {
                clientSocket = null
                commandQueue.clear()
                try {
                    socket.close()
                } catch (_: IOException) {
                }
                _uiState.update {
                    it.copy(connected = false, error = "The PC closed the connection")
                }
            }
        }
    }

    /**
     * Process a send gamepad state command.
     */
    private fun processSendGamepadStateCommand(command: NetworkCommand.SendGamepadState) {
        if (!_uiState.value.connected || clientSocket == null) {
            // Skip sending if we're not connected
            return
        }

        try {
            command.gamepadState.marshal(clientSocket!!.outputStream, null)
        } catch (e: IOException) {
            Log.e(tag, "Error sending gamepad state: ${e.message}", e)

            // Update UI with error and disconnect when socket errors occur
            _uiState.update {
                it.copy(
                    error = "Connection lost: ${e.message ?: "Unknown error"}",
                    connected = false  // Mark as disconnected immediately
                )
            }

            // Handle disconnection immediately instead of queuing
            if (e is SocketException) {
                Log.d(tag, "Socket exception detected, forcing disconnect")
                clientSocket?.close()
                clientSocket = null
                commandQueue.clear()

                // Throw exception to break out of the command processor loop
                throw e
            } else {
                // For other IO exceptions, try graceful disconnect
                commandQueue.clear()  // Clear other pending commands
                commandQueue.offer(NetworkCommand.Disconnect)  // Make sure we disconnect properly
            }
        }
    }

    /**
     * Process a send string command.
     */
    private fun processSendStringCommand(command: NetworkCommand.SendString) {
        if (!_uiState.value.connected || clientSocket == null) {
            // Skip sending if we're not connected
            return
        }

        try {
            clientSocket!!.outputStream.write(command.string.toByteArray())
        } catch (e: IOException) {
            Log.e(tag, "Error sending string: ${e.message}", e)

            // Update UI with error and disconnect when socket errors occur
            _uiState.update {
                it.copy(
                    error = "Connection lost: ${e.message ?: "Unknown error"}",
                    connected = false  // Mark as disconnected immediately
                )
            }

            // Handle disconnection immediately instead of queuing
            if (e is SocketException) {
                Log.d(tag, "Socket exception detected, forcing disconnect")
                clientSocket?.close()
                clientSocket = null
                commandQueue.clear()

                // Throw exception to break out of the command processor loop
                throw e
            } else {
                // For other IO exceptions, try graceful disconnect
                commandQueue.clear()  // Clear other pending commands
                commandQueue.offer(NetworkCommand.Disconnect)  // Make sure we disconnect properly
            }
        }
    }

    /**
     * Process a disconnect command.
     */
    private fun processDisconnectCommand() {
        val socket = clientSocket
        // Forget the socket first so the reader does not report this as a lost connection
        clientSocket = null
        try {
            socket?.close()
        } catch (e: IOException) {
            Log.e(tag, "Error closing socket: ${e.message}", e)
        } finally {
            clientSocket = null

            // Clear the queue in case there are pending commands
            commandQueue.clear()

            _uiState.update {
                ConnectionState(userStopped = it.userStopped) // Reset to initial state
            }

            Log.d(tag, "Disconnected")
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Ensure disconnection when ViewModel is cleared
        if (_uiState.value.connected || clientSocket != null) {
            Log.d(tag, "ViewModel cleared, ensuring disconnection.")
            // Force immediate disconnect rather than queueing
            try {
                clientSocket?.close()
            } catch (e: IOException) {
                Log.e(tag, "Error closing socket during cleanup: ${e.message}", e)
            } finally {
                clientSocket = null
                commandQueue.clear()
                commandProcessorJob?.cancel()
                commandProcessorJob = null
            }
        }
    }

    /**
     * Runs a series of network diagnostics to troubleshoot connection issues.
     * @param context Android context to access system services
     */
    fun runDiagnostics(context: Context) {
        val currentState = _uiState.value
        val ipAddress = currentState.ipAddress
        val port = currentState.port

        if (ipAddress.isBlank() || port == -1) {
            Log.e(tag, "Cannot run diagnostics: IP or Port missing")
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRunningDiagnostics = true,
                    diagnosticResults = emptyList()
                )
            }

            val diagnostics = NetworkDiagnostics(context)
            val results = mutableListOf<NetworkDiagnostics.DiagnosticResult>()

            // Step 1: Wi-Fi/Network
            val isWifiConnected = diagnostics.checkNetworkConnectivity()
            results.add(
                NetworkDiagnostics.DiagnosticResult(
                    NetworkDiagnostics.DiagnosticStep.WIFI,
                    isWifiConnected,
                    if (isWifiConnected) context.getString(R.string.diagnostics_pass_wifi)
                    else context.getString(R.string.diagnostics_fail_wifi)
                )
            )
            _uiState.update { it.copy(diagnosticResults = results.toList()) }
            if (!isWifiConnected) {
                _uiState.update { it.copy(isRunningDiagnostics = false) }
                return@launch
            }

            // Step 2: Local IP
            val localIp = diagnostics.getLocalIpAddress()
            val hasLocalIp = localIp != null
            results.add(
                NetworkDiagnostics.DiagnosticResult(
                    NetworkDiagnostics.DiagnosticStep.IP,
                    hasLocalIp,
                    if (hasLocalIp) context.getString(R.string.diagnostics_pass_ip, localIp)
                    else context.getString(R.string.diagnostics_fail_ip)
                )
            )
            _uiState.update { it.copy(diagnosticResults = results.toList()) }
            if (!hasLocalIp) {
                _uiState.update { it.copy(isRunningDiagnostics = false) }
                return@launch
            }

            // Step 3: Subnet check
            val sameSubnet = diagnostics.isSameSubnet(localIp, ipAddress)
            results.add(
                NetworkDiagnostics.DiagnosticResult(
                    NetworkDiagnostics.DiagnosticStep.SUBNET,
                    sameSubnet,
                    if (sameSubnet) context.getString(R.string.diagnostics_pass_subnet)
                    else context.getString(R.string.diagnostics_fail_subnet, localIp, ipAddress)
                )
            )
            _uiState.update { it.copy(diagnosticResults = results.toList()) }

            // Step 4: Ping
            val canPing = diagnostics.pingHost(ipAddress)
            results.add(
                NetworkDiagnostics.DiagnosticResult(
                    NetworkDiagnostics.DiagnosticStep.PING,
                    canPing,
                    if (canPing) context.getString(R.string.diagnostics_pass_ping)
                    else context.getString(R.string.diagnostics_fail_ping)
                )
            )
            _uiState.update { it.copy(diagnosticResults = results.toList()) }

            // Step 5: Port check
            val canConnectPort = diagnostics.checkPort(ipAddress, port)
            results.add(
                NetworkDiagnostics.DiagnosticResult(
                    NetworkDiagnostics.DiagnosticStep.PORT,
                    canConnectPort,
                    if (canConnectPort) context.getString(R.string.diagnostics_pass_port, port)
                    else context.getString(R.string.diagnostics_fail_port, port)
                )
            )
            _uiState.update {
                it.copy(
                    diagnosticResults = results.toList(),
                    isRunningDiagnostics = false
                )
            }
        }
    }

    /**
     * Clears the current diagnostic results.
     */
    fun clearDiagnostics() {
        _uiState.update { it.copy(diagnosticResults = emptyList(), isRunningDiagnostics = false) }
    }
}

