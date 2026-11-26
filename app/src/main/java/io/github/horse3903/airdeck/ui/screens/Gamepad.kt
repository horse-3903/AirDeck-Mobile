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
import io.github.horse3903.airdeck.ui.composables.TopControls
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
    val prefsNow by androidx.compose.runtime.rememberUpdatedState(prefs)
    val rootView = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.DisposableEffect(prefs.keepScreenOn) {
        rootView.keepScreenOn = prefs.keepScreenOn
        onDispose { rootView.keepScreenOn = false }
    }

    val isStopping = remember { mutableStateOf(false) }
    val leaving = remember { mutableStateOf(false) }

    val connectionState by connectionViewModel?.uiState?.collectAsState()
        ?: remember { mutableStateOf(null) }

    val stoppedByUser = connectionState?.userStopped == true

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

    // The game rumbling this controller shakes the phone
    LaunchedEffect(connectionViewModel, prefs.gameRumble) {
        if (!prefs.gameRumble) {
            io.github.horse3903.airdeck.ui.utils.HapticUtils.stopRumble(context)
            return@LaunchedEffect
        }
        connectionViewModel?.rumble?.collect { motors ->
            io.github.horse3903.airdeck.ui.utils.HapticUtils.gameRumble(context, motors[0], motors[1])
        }
    }
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { io.github.horse3903.airdeck.ui.utils.HapticUtils.stopRumble(context) }
    }

    // Tilt steering: the phone turned like a steering wheel adds to a stick
    androidx.compose.runtime.DisposableEffect(prefs.tiltTarget) {
        if (prefs.tiltTarget == 0) return@DisposableEffect onDispose {}
        val manager = context.getSystemService(android.content.Context.SENSOR_SERVICE) as? android.hardware.SensorManager
        val sensor = manager?.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)
        io.github.horse3903.airdeck.ui.utils.TiltSteering.reset()
        val listener = object : android.hardware.SensorEventListener {
            override fun onSensorChanged(e: android.hardware.SensorEvent) {
                val tilt = io.github.horse3903.airdeck.ui.utils.TiltSteering
                tilt.rangeDeg = prefsNow.tiltRange.toFloat()
                tilt.deadDeg = prefsNow.tiltDead.toFloat()
                tilt.invert = prefsNow.tiltInvert
                @Suppress("DEPRECATION")
                val rotation = (context.getSystemService(android.content.Context.WINDOW_SERVICE) as android.view.WindowManager).defaultDisplay.rotation
                tilt.onAccelerometer(e.values[0], e.values[1], rotation)
            }

            override fun onAccuracyChanged(s: android.hardware.Sensor?, a: Int) {}
        }
        if (manager != null && sensor != null) manager.registerListener(listener, sensor, android.hardware.SensorManager.SENSOR_DELAY_GAME)
        onDispose {
            manager?.unregisterListener(listener)
            io.github.horse3903.airdeck.ui.utils.TiltSteering.reset()
        }
    }

    // A short buzz when the link comes up or drops
    var wasConnected by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(connectionState?.connected) {
        val now = connectionState?.connected
        if (prefs.connectionHaptics && wasConnected != null && now != null && now != wasConnected) {
            io.github.horse3903.airdeck.ui.utils.HapticUtils.connectionPulse(context, now)
        }
        if (now != null) wasConnected = now
    }

    // Dim the screen after a pause, any touch brings it back
    val lastTouch = remember { mutableStateOf(android.os.SystemClock.elapsedRealtime()) }
    val dimmed = remember { mutableStateOf(false) }
    androidx.compose.runtime.DisposableEffect(prefs.dimAfterSeconds) {
        val window = context.findActivityOrNull()?.window
        if (window == null || prefs.dimAfterSeconds == 0) return@DisposableEffect onDispose {}
        val original = window.callback
        fun setBrightness(level: Float) {
            val attrs = window.attributes
            attrs.screenBrightness = level
            window.attributes = attrs
        }
        window.callback = object : android.view.Window.Callback by original {
            override fun dispatchTouchEvent(event: android.view.MotionEvent?): Boolean {
                lastTouch.value = android.os.SystemClock.elapsedRealtime()
                if (dimmed.value) {
                    dimmed.value = false
                    setBrightness(android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE)
                }
                return original.dispatchTouchEvent(event)
            }
        }
        onDispose {
            window.callback = original
            dimmed.value = false
            setBrightness(android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE)
        }
    }
    LaunchedEffect(prefs.dimAfterSeconds) {
        val window = context.findActivityOrNull()?.window ?: return@LaunchedEffect
        if (prefs.dimAfterSeconds == 0) return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(1000)
            if (!dimmed.value && android.os.SystemClock.elapsedRealtime() - lastTouch.value > prefs.dimAfterSeconds * 1000L) {
                dimmed.value = true
                val attrs = window.attributes
                attrs.screenBrightness = 0.02f
                window.attributes = attrs
            }
        }
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
            if (userDisconnected || stoppedByUser) ConnectionService.close(context)
            onNavigateBack()
        },
        glowSlot = if (connectionState?.connected == true) connectionState?.controllerSlot else null,
        topControls = TopControls(
            status = when {
                userDisconnected || stoppedByUser -> LinkStatus.DISCONNECTED
                connectionState?.connected == true -> LinkStatus.CONNECTED
                else -> LinkStatus.CONNECTING
            },
            slot = connectionState?.controllerSlot,
            onBadgeClick = {
                when {
                    // Tap again to reconnect
                    userDisconnected || stoppedByUser -> {
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
            onGearClick = { showQuickSettings = true },
        )
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
    LaunchedEffect(connected, leaving.value, userDisconnected, stoppedByUser) {
        if (connectionViewModel == null || leaving.value || userDisconnected || stoppedByUser || connected != false) return@LaunchedEffect
        if (!prefs.autoReconnect) {
            Toast.makeText(context, lostMessage, Toast.LENGTH_LONG).show()
            onNavigateBack()
            return@LaunchedEffect
        }
        var recovered = false
        val deadline = android.os.SystemClock.elapsedRealtime() + prefs.reconnectSeconds * 1000L
        while (android.os.SystemClock.elapsedRealtime() < deadline && !leaving.value && !userDisconnected && !recovered) {
            delay(1500)
            if (connectionViewModel.uiState.value.userStopped) return@LaunchedEffect
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
        if (!recovered && !leaving.value && !userDisconnected && !connectionViewModel.uiState.value.userStopped) {
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
            val tilt = if (prefsNow.tiltTarget != 0) io.github.horse3903.airdeck.ui.utils.TiltSteering.steer else 0f
            connectionViewModel.enqueueGamepadState(if (tilt == 0f) gamepadState else withTilt(gamepadState, tilt, prefsNow.tiltTarget))

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

private fun android.content.Context.findActivityOrNull(): android.app.Activity? {
    var current: android.content.Context = this
    while (current is android.content.ContextWrapper) {
        if (current is android.app.Activity) return current
        current = current.baseContext
    }
    return null
}

/** A copy of the reading with the tilt added to the chosen stick, so the touch sticks keep their own values. */
private fun withTilt(src: GamepadReading, steer: Float, target: Int): GamepadReading = GamepadReading().also {
    it.ButtonsDown = src.ButtonsDown
    it.ButtonsUp = src.ButtonsUp
    it.LeftTrigger = src.LeftTrigger
    it.RightTrigger = src.RightTrigger
    it.LeftThumbstickX = src.LeftThumbstickX
    it.LeftThumbstickY = src.LeftThumbstickY
    it.RightThumbstickX = src.RightThumbstickX
    it.RightThumbstickY = src.RightThumbstickY
    if (target == 1) it.LeftThumbstickX = (src.LeftThumbstickX + steer).coerceIn(-1f, 1f)
    else it.RightThumbstickX = (src.RightThumbstickX + steer).coerceIn(-1f, 1f)
}
