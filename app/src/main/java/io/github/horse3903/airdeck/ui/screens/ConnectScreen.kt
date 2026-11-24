package io.github.horse3903.airdeck.ui.screens

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import android.content.Intent
import android.net.InetAddresses
import android.os.Build
import android.util.Log
import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import io.github.horse3903.airdeck.R
import io.github.horse3903.airdeck.data.PreviewBase
import io.github.horse3903.airdeck.data.PreviewHeightDp
import io.github.horse3903.airdeck.data.PreviewWidthDp
import io.github.horse3903.airdeck.ui.components.QRScanResult
import io.github.horse3903.airdeck.ui.components.rememberQRCodeScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

const val LOG_TAG = "ConnectMenu"

private fun getIP(qrCode: String): String {
    val splitTill = qrCode.lastIndexOf(":")
    if (splitTill == -1) return qrCode
    return qrCode.substring(0, splitTill)
}

private fun getPort(qrCode: String): String {
    val splitAt = qrCode.lastIndexOf(":")
    if (splitAt == -1) return qrCode
    return qrCode.substring(splitAt + 1)
}

@Suppress("DEPRECATION")
private fun validateIP(ipAddress: String): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        InetAddresses.isNumericAddress(ipAddress)
    } else {
        Patterns.IP_ADDRESS.matcher(ipAddress).matches()
    }
}

private fun validatePort(port: String): Boolean {
    val minPort = 1
    val maxPort = 65535
    return port.toIntOrNull().let { it != null && it in minPort..maxPort }
}

private fun processQRScanResult(
    result: QRScanResult,
    onNavigateToConnectingScreen: (String, String) -> Unit,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    context: android.content.Context
) {
    when (result) {
        is QRScanResult.Success -> {
            try {
                val qrCode = result.content
                Log.d(LOG_TAG, qrCode)
                val ipAddress = getIP(qrCode)
                val port = getPort(qrCode)

                if (validateIP(ipAddress) && validatePort(port)) {
                    // Navigate to the connecting screen with the IP and port
                    onNavigateToConnectingScreen(ipAddress, port)
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = context.getString(R.string.connect_qr_error_format),
                        )
                    }
                }
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = context.getString(
                            R.string.connect_qr_error_processing,
                            e.message ?: e.toString()
                        ),
                    )
                }
            }
        }

        is QRScanResult.Error -> {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = context.getString(R.string.connect_qr_error_scanning, result.message),
                )
            }
        }

        is QRScanResult.PermissionDenied -> {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = context.getString(R.string.connect_camera_permission_denied),
                )
            }
        }

        is QRScanResult.Cancelled -> { // User canceled the QR code scan
        }
    }
}

@Composable
fun ConnectMenu(
    onNavigateToConnectingScreen: (String, String) -> Unit,
    initialIp: String,
    initialPort: String
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val qrCodeScanner = rememberQRCodeScanner { result ->
        processQRScanResult(
            result,
            onNavigateToConnectingScreen = onNavigateToConnectingScreen,
            snackbarHostState = snackbarHostState,
            scope = scope,
            context = context
        )
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = maxOf(
                        32.dp,
                        (LocalConfiguration.current.screenWidthDp.dp - 480.dp) / 2
                    )
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            fun join(ip: String, port: String) = if (ip.isBlank()) "" else if (port.isBlank()) ip else "$ip:$port"
            var address by rememberSaveable { mutableStateOf(join(initialIp, initialPort)) }
            var touched by rememberSaveable { mutableStateOf(false) }
            val ipAddress = getIP(address.trim())
            val port = if (address.contains(":")) getPort(address.trim()) else ""
            val isIPValid = validateIP(ipAddress)
            val isPortValid = validatePort(port)

            // Update state when parameters arrive asynchronously from DataStore
            LaunchedEffect(initialIp, initialPort) {
                if (address.isBlank() && initialIp.isNotBlank()) {
                    address = join(initialIp, initialPort)
                }
            }

            val focusManager = LocalFocusManager.current
            val connectErrorIpStr = stringResource(R.string.connect_error_ip)
            val connectErrorPortStr = stringResource(R.string.connect_error_port)
            val connectErrorParamsStr = stringResource(R.string.connect_error_params)

            fun attemptToConnect() {
                if (isIPValid && isPortValid) {
                    // Navigate to the connecting screen with the IP and port
                    onNavigateToConnectingScreen(ipAddress, port)
                } else {
                    scope.launch {
                        val errorMessage = when {
                            !isIPValid -> connectErrorIpStr
                            !isPortValid -> connectErrorPortStr
                            else -> connectErrorParamsStr
                        }
                        snackbarHostState.showSnackbar(
                            message = errorMessage,
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.connect_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { qrCodeScanner() },
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
            ) {
                Text(text = stringResource(R.string.connect_scan_qr))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.connect_or_manual),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.connect_where),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            TextField(
                label = { Text(text = stringResource(R.string.connect_address_label)) },
                placeholder = { Text("192.168.0.3:52500") },
                value = address,
                onValueChange = {
                    address = it
                    touched = true
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Uri,
                    imeAction = ImeAction.Done
                ),
                singleLine = true,
                supportingText = {
                    if (touched && address.isNotBlank() && !(isIPValid && isPortValid)) {
                        Text(stringResource(R.string.connect_address_invalid))
                    }
                },
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        attemptToConnect()
                    }
                ),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                isError = touched && address.isNotBlank() && !(isIPValid && isPortValid)
            )

            Button(
                onClick = { attemptToConnect() },
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                enabled = isIPValid && isPortValid,
            ) {
                Text(text = stringResource(R.string.connect_button))
            }
        }
    }
}

@Preview(
    widthDp = PreviewWidthDp,
    heightDp = PreviewHeightDp,
)
@Composable
fun ConnectMenuPreview() {
    PreviewBase {
        ConnectMenu(
            onNavigateToConnectingScreen = { _, _ -> },
            initialIp = "",
            initialPort = "12345"
        )
    }
}
