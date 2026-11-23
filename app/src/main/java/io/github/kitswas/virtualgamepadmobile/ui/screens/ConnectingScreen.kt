package io.github.kitswas.virtualgamepadmobile.ui.screens

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.kitswas.virtualgamepadmobile.R
import io.github.kitswas.virtualgamepadmobile.data.PreviewBase
import io.github.kitswas.virtualgamepadmobile.data.PreviewHeightDp
import io.github.kitswas.virtualgamepadmobile.data.PreviewWidthDp
import io.github.kitswas.virtualgamepadmobile.network.ConnectionViewModel
import io.github.kitswas.virtualgamepadmobile.ui.theme.SuccessGreen
import kotlinx.coroutines.launch

@Composable
fun ConnectingScreen(
    onNavigateToGamepad: () -> Unit,
    onNavigateBack: () -> Unit,
    connectionViewModel: ConnectionViewModel?,
    ipAddress: String,
    port: String
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDiagnosticsDialog by rememberSaveable { mutableStateOf(false) }

    // Get the current connection state
    val connectionState by connectionViewModel?.uiState?.collectAsState()
        ?: remember { mutableStateOf(null) }

    val connectErrorParamsStr = stringResource(R.string.connect_error_params)
    val connectingFailedMsg =
        connectionState?.error?.let { stringResource(R.string.connecting_failed, it) }

    // Initiate connection when entering screen
    LaunchedEffect(ipAddress, port) {
        try {
            Log.d("ConnectingScreen", "Initiating connection to $ipAddress:$port")
            connectionViewModel?.connect(ipAddress, port.toInt())
        } catch (e: Exception) {
            Log.e("ConnectingScreen", "Failed to initiate connection: ${e.message}")
            snackbarHostState.showSnackbar(
                message = connectErrorParamsStr + ": ${e.message}",
                duration = SnackbarDuration.Short
            )
            onNavigateBack()
        }
    }

    // Watch for connection state changes
    LaunchedEffect(
        connectionState?.connected,
        connectionState?.error,
        connectionState?.isConnecting
    ) {
        connectionState?.let { state ->
            if (state.connected) {
                // Connection successful, navigate to gamepad
                Log.d("ConnectingScreen", "Connection successful, navigating to gamepad")
                onNavigateToGamepad()
            } else if (!state.isConnecting && state.error != null) {
                // Connection failed, show error and stay on screen
                Log.d("ConnectingScreen", "Connection failed: ${state.error}")
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = connectingFailedMsg ?: "",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    // Handle back navigation to cancel connection
    BackHandler {
        // Cancel any pending connection
        Log.d("ConnectingScreen", "Back pressed, canceling connection")
        connectionViewModel?.disconnect()
        onNavigateBack()
    }

    // UI for connecting screen
    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (connectionState?.connected == true) {
                Text(stringResource(R.string.connecting_success))
            } else if (connectionState?.isConnecting == true) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.connecting_status, ipAddress, port))
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(shape = MaterialTheme.shapes.small, onClick = {
                    connectionViewModel?.disconnect()
                    onNavigateBack()
                }) { Text(stringResource(R.string.connecting_cancel)) }
            } else {
                // Show error message if there is one
                connectionState?.error?.let { error ->
                    var showDetails by rememberSaveable { mutableStateOf(false) }
                    val kind = errorKind(error)
                    Column(
                        modifier = Modifier
                            .widthIn(max = 480.dp)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(
                                when (kind) {
                                    ErrorKind.REFUSED -> R.string.error_headline_refused
                                    ErrorKind.TIMEOUT -> R.string.error_headline_timeout
                                    ErrorKind.UNREACHABLE -> R.string.error_headline_unreachable
                                    ErrorKind.CLOSED -> R.string.error_headline_closed
                                    ErrorKind.OTHER -> R.string.error_headline_generic
                                }
                            ),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(
                                when (kind) {
                                    ErrorKind.REFUSED -> R.string.error_body_refused
                                    ErrorKind.TIMEOUT -> R.string.error_body_timeout
                                    ErrorKind.UNREACHABLE -> R.string.error_body_unreachable
                                    ErrorKind.CLOSED -> R.string.error_body_closed
                                    ErrorKind.OTHER -> R.string.error_body_generic
                                },
                                ipAddress, port
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            onClick = { connectionViewModel?.connect(ipAddress, port.toInt()) }
                        ) { Text(stringResource(R.string.error_try_again)) }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            onClick = onNavigateBack
                        ) { Text(stringResource(R.string.error_edit_connection)) }
                        TextButton(
                            shape = MaterialTheme.shapes.small,
                            onClick = {
                                connectionViewModel?.runDiagnostics(context)
                                showDiagnosticsDialog = true
                            }
                        ) { Text(stringResource(R.string.connecting_diagnostics)) }
                        TextButton(
                            shape = MaterialTheme.shapes.small,
                            onClick = { showDetails = !showDetails }
                        ) {
                            Text(
                                stringResource(
                                    if (showDetails) R.string.error_hide_details
                                    else R.string.error_show_details
                                )
                            )
                        }
                        if (showDetails) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                } ?: run {
                    // No error but also not connecting - initial state
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.connecting_preparing))
                }
            }
        }

        if (showDiagnosticsDialog) {
            DiagnosticsDialog(
                connectionState = connectionState,
                onDismiss = {
                    showDiagnosticsDialog = false
                    connectionViewModel?.clearDiagnostics()
                }
            )
        }
    }
}

@Composable
fun DiagnosticsDialog(
    connectionState: io.github.kitswas.virtualgamepadmobile.network.ConnectionState?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.diagnostics_title))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (connectionState?.isRunningDiagnostics == true) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(stringResource(R.string.diagnostics_running))
                    }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(connectionState?.diagnosticResults ?: emptyList()) { result ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = if (result.isPassed) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (result.isPassed) SuccessGreen else MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Column {
                                Text(
                                    text = result.message,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(shape = MaterialTheme.shapes.small, onClick = onDismiss) {
                Text(stringResource(R.string.diagnostics_close))
            }
        }
    )
}

@Preview(
    widthDp = PreviewWidthDp,
    heightDp = PreviewHeightDp,
)
@Composable
private fun DiagnosticsDialogPreview() {
    PreviewBase {
        val serverIP = "192.0.2.1" // Use TEST-NET-1 IP for preview
        val clientIP = "192.0.2.5" // Another TEST-NET-1 IP for client
        val port = 12345
        val sampleState = io.github.kitswas.virtualgamepadmobile.network.ConnectionState(
            connected = false,
            ipAddress = serverIP,
            port = port,
            error = null,
            isConnecting = false,
            isRunningDiagnostics = true,
            diagnosticResults = listOf(
                io.github.kitswas.virtualgamepadmobile.network.NetworkDiagnostics.DiagnosticResult(
                    io.github.kitswas.virtualgamepadmobile.network.NetworkDiagnostics.DiagnosticStep.WIFI,
                    true,
                    "Device connected to Wi-Fi"
                ),
                io.github.kitswas.virtualgamepadmobile.network.NetworkDiagnostics.DiagnosticResult(
                    io.github.kitswas.virtualgamepadmobile.network.NetworkDiagnostics.DiagnosticStep.IP,
                    true,
                    "Local IP: $clientIP"
                ),
                io.github.kitswas.virtualgamepadmobile.network.NetworkDiagnostics.DiagnosticResult(
                    io.github.kitswas.virtualgamepadmobile.network.NetworkDiagnostics.DiagnosticStep.PING,
                    false,
                    "Ping to server failed",
                    "Timeout"
                )
            )
        )

        DiagnosticsDialog(
            connectionState = sampleState,
            onDismiss = {}
        )
    }
}

private enum class ErrorKind { REFUSED, TIMEOUT, UNREACHABLE, CLOSED, OTHER }

private fun errorKind(error: String): ErrorKind {
    val text = error.lowercase()
    return when {
        "refused" in text || "econnrefused" in text -> ErrorKind.REFUSED
        "closed" in text -> ErrorKind.CLOSED
        "unreachable" in text || "no route" in text || "enetunreach" in text -> ErrorKind.UNREACHABLE
        "timed out" in text || "timeout" in text || "etimedout" in text -> ErrorKind.TIMEOUT
        else -> ErrorKind.OTHER
    }
}
