package io.github.horse3903.airdeck.network


data class ConnectionState(
    val connected: Boolean = false,
    val ipAddress: String = "",
    val port: Int = -1,
    val error: String? = null,
    val isConnecting: Boolean = false,
    /** The user chose to stop (button, badge or notification), so nothing should reconnect on its own. */
    val userStopped: Boolean = false,
    val controllerSlot: Int? = null,
    val isRunningDiagnostics: Boolean = false,
    val diagnosticResults: List<NetworkDiagnostics.DiagnosticResult> = emptyList()
)
