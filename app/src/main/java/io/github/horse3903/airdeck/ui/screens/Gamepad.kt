package io.github.horse3903.airdeck.ui.screens

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Devices.TABLET
import androidx.compose.ui.tooling.preview.Preview
import io.github.kitswas.VGP_Data_Exchange.GamepadReading
import io.github.horse3903.airdeck.R
import io.github.horse3903.airdeck.data.PreviewBase
import io.github.horse3903.airdeck.data.PreviewHeightDp
import io.github.horse3903.airdeck.data.PreviewWidthDp
import io.github.horse3903.airdeck.data.SettingsRepository
import io.github.horse3903.airdeck.data.defaultButtonConfigs
import io.github.horse3903.airdeck.data.defaultPollingDelay
import io.github.horse3903.airdeck.network.ConnectionViewModel
import io.github.horse3903.airdeck.network.ConnectionService
import io.github.horse3903.airdeck.ui.composables.ConnectionBadge
import io.github.horse3903.airdeck.ui.composables.GearButton
import io.github.horse3903.airdeck.ui.composables.QuickSettingsDialog
import io.github.horse3903.airdeck.ui.composables.LinkStatus
import io.github.horse3903.airdeck.ui.composables.DrawGamepad
import io.github.horse3903.airdeck.ui.composables.DisconnectDialog
import io.github.horse3903.airdeck.ui.utils.findActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val tag = "GamePadScreen"

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun GamePad(
    connectionViewModel: ConnectionViewModel?,
    onNavigateBack: () -> Unit,
) {
    val gamepadState by rememberSaveable { mutableStateOf(GamepadReading()) }
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val pollingDelay =
        settingsRepository.pollingDelay.collectAsState(defaultPollingDelay).value.toLong()
    val buttonConfigs =
        settingsRepository.buttonConfigs.collectAsState(defaultButtonConfigs).value

    val configuration = LocalConfiguration.current

    val screenHeight = configuration.screenHeightDp
    val screenWidth = configuration.screenWidthDp

    val prefs = io.github.horse3903.airdeck.ui.composables.LocalAppPrefs.current
    val rootView = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.DisposableEffect(prefs.keepScreenOn) {
        rootView.keepScreenOn = prefs.keepScreenOn
        onDispose { rootView.keepScreenOn = false }
    }

    val isStopping = remember { mutableStateOf(false) }
    val leaving = remember { mutableStateOf(false) }

    val connectionState by connectionViewModel?.uiState?.collectAsState()
        ?: remember { mutableStateOf(null) }

    fun releaseEverything() {
        gamepadState.ButtonsUp = gamepadState.ButtonsDown
        gamepadState.ButtonsDown = 0
        gamepadState.LeftThumbstickX = 0F
        gamepadState.LeftThumbstickY = 0F
        gamepadState.RightThumbstickX = 0F
        gamepadState.RightThumbstickY = 0F
        gamepadState.LeftTrigger = 0F
        gamepadState.RightTrigger = 0F
        connectionViewModel?.enqueueGamepadState(gamepadState)
    }

    var showDisconnectConfirm by rememberSaveable { mutableStateOf(false) }
    var userDisconnected by rememberSaveable { mutableStateOf(false) }
    var showQuickSettings by rememberSaveable { mutableStateOf(false) }
    DrawGamepad(
        screenWidth, screenHeight, gamepadState, buttonConfigs,
        onBack = {
            // Back goes straight to the main page and the connection stays open
            leaving.value = true
            isStopping.value = true
            releaseEverything()
            if (userDisconnected) ConnectionService.close(context)
            onNavigateBack()
        },
        glowSlot = if (connectionState?.connected == true) connectionState?.controllerSlot else null,
        topCenter = { rowHeight ->
            ConnectionBadge(
                status = when {
                    userDisconnected -> LinkStatus.DISCONNECTED
                    connectionState?.connected == true -> LinkStatus.CONNECTED
                    else -> LinkStatus.CONNECTING
                },
                slot = connectionState?.controllerSlot,
                onClick = {
                    when {
                        // Tap again to reconnect
                        userDisconnected -> {
                            userDisconnected = false
                            connectionViewModel?.connect(connectionViewModel.lastIpAddress, connectionViewModel.lastPort)
                        }
                        connectionState?.connected == true ->
                            if (prefs.confirmDisconnect) {
                                showDisconnectConfirm = true
                            } else {
                                releaseEverything()
                                userDisconnected = true
                                connectionViewModel?.disconnect()
                            }
                        // Still trying to reconnect: stop
                        else -> {
                            userDisconnected = true
                            connectionViewModel?.disconnect()
                        }
                    }
                },
                height = rowHeight,
                modifier = androidx.compose.ui.Modifier
                    .align(androidx.compose.ui.Alignment.TopCenter)
                    .offset(y = (screenHeight * -0.0274f).dp)
            )
            GearButton(
                onClick = { showQuickSettings = true },
                height = rowHeight,
                modifier = androidx.compose.ui.Modifier
                    .align(androidx.compose.ui.Alignment.TopCenter)
                    .offset(x = (screenHeight * 0.14f).dp, y = (screenHeight * -0.0274f).dp)
            )
        }
    )
    if (showQuickSettings) {
        QuickSettingsDialog(settingsRepository = settingsRepository, onDismiss = { showQuickSettings = false })
    }
    if (showDisconnectConfirm) {
        DisconnectDialog(
            onCancel = { showDisconnectConfirm = false },
            onConfirm = {
                showDisconnectConfirm = false
                releaseEverything()
                userDisconnected = true
                connectionViewModel?.disconnect()
            }
        )
    }

    // A dropped connection is retried for a while, with the badge showing the hourglass, before giving up
    val connected = connectionState?.connected
    val lostMessage = stringResource(R.string.gamepad_connection_lost)
    LaunchedEffect(connected, leaving.value, userDisconnected) {
        if (connectionViewModel == null || leaving.value || userDisconnected || connected != false) return@LaunchedEffect
        if (!prefs.autoReconnect) {
            Toast.makeText(context, lostMessage, Toast.LENGTH_LONG).show()
            onNavigateBack()
            return@LaunchedEffect
        }
        var recovered = false
        val deadline = android.os.SystemClock.elapsedRealtime() + prefs.reconnectSeconds * 1000L
        while (android.os.SystemClock.elapsedRealtime() < deadline && !leaving.value && !userDisconnected && !recovered) {
            delay(1500)
            connectionViewModel.connect(connectionViewModel.lastIpAddress, connectionViewModel.lastPort)
            var waited = 0
            while (waited < 4000) {
                val state = connectionViewModel.uiState.value
                if (state.connected || state.error != null) break
                delay(200)
                waited += 200
            }
            recovered = connectionViewModel.uiState.value.connected
        }
        if (!recovered && !leaving.value && !userDisconnected) {
            Log.d(tag, lostMessage)
            Toast.makeText(context, lostMessage, Toast.LENGTH_LONG).show()
            onNavigateBack()
        }
    }

    val startAfter = 100L // in milliseconds

    // Send gamepad state updates periodically
    LaunchedEffect(gamepadState, pollingDelay) {
        delay(startAfter)

        // Start sending updates
        while (connectionViewModel != null && !isStopping.value) {
            // Queue the update in the ViewModel
            connectionViewModel.enqueueGamepadState(gamepadState)

            // Reset ButtonsUp after each update
            gamepadState.ButtonsUp = 0

            // Wait before next update
            delay(pollingDelay)
        }
    }
}

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
@Preview(
    name = "Design Preview (Light)",
    device = "spec:width=${PreviewWidthDp}dp,height=${PreviewHeightDp}dp,orientation=landscape,dpi=420",
)
@Preview(
    name = "Design Preview (Dark)",
    device = "spec:width=${PreviewWidthDp}dp,height=${PreviewHeightDp}dp,orientation=landscape,dpi=420",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Preview(
    name = "Phone - Landscape (Light)",
    device = "spec:width=411dp,height=891dp,orientation=landscape,dpi=420",
    showSystemUi = true
)
@Preview(
    name = "Phone - Landscape (Dark)",
    device = "spec:width=411dp,height=891dp,orientation=landscape,dpi=420",
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Preview(
    name = "Tablet (Light)",
    device = TABLET,
    showSystemUi = true
)
@Preview(
    name = "Tablet (Dark)",
    device = TABLET,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Preview(
    name = "Desktop (Light)",
    device = DESKTOP,
    showSystemUi = true
)
@Preview(
    name = "Desktop (Dark)",
    device = DESKTOP,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
annotation class MultiDevicePreview

@MultiDevicePreview
@Composable
fun GamePadPreview() {
    PreviewBase {
        GamePad(null) {}
    }
}
