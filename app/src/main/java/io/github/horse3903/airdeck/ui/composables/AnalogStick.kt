package io.github.horse3903.airdeck.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalViewConfiguration
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.hypot
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.kitswas.VGP_Data_Exchange.GameButtons
import io.github.horse3903.airdeck.data.AppPrefs
import io.github.kitswas.VGP_Data_Exchange.GamepadReading
import io.github.horse3903.airdeck.ui.theme.darken
import io.github.horse3903.airdeck.ui.theme.lighten
import io.github.horse3903.airdeck.ui.utils.HapticUtils
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class AnalogStickType {
    LEFT, RIGHT
}

private class AnalogStickUiState {
    var offsetX by mutableFloatStateOf(0f)
    var offsetY by mutableFloatStateOf(0f)
    var visualX by mutableFloatStateOf(0f)
    var visualY by mutableFloatStateOf(0f)
    var isPressed by mutableStateOf(false)
    // How far a floating stick's centre sits from the middle of its touch area
    var shiftX by mutableFloatStateOf(0f)
    var shiftY by mutableFloatStateOf(0f)
}

/** Dead zone, sensitivity and inversion applied to a stick position whose length is 0 to 1. */
private fun shaped(prefs: AppPrefs, type: AnalogStickType, x: Float, y: Float): Pair<Float, Float> {
    val length = sqrt(x * x + y * y)
    var nx = x
    var ny = y
    val dead = prefs.deadZone / 100f
    if (length < dead) {
        nx = 0f
        ny = 0f
    } else if (length > 0f) {
        val scaled = (((length - dead) / (1f - dead)) * (prefs.sensitivity / 100f)).coerceAtMost(1f)
        val curved = when (prefs.stickCurve) {
            1 -> scaled.pow(1.6f)
            2 -> scaled.pow(0.7f)
            else -> scaled
        }
        val factor = curved / length
        nx = x * factor
        ny = y * factor
    }
    val invert = if (type == AnalogStickType.LEFT) prefs.invertLeftY else prefs.invertRightY
    return nx to (if (invert) -ny else ny)
}

private fun updateThumbstickValues(
    type: AnalogStickType,
    gamepadState: GamepadReading,
    x: Float,
    y: Float,
) {
    when (type) {
        AnalogStickType.LEFT -> {
            gamepadState.LeftThumbstickX = x
            gamepadState.LeftThumbstickY = y
        }

        AnalogStickType.RIGHT -> {
            gamepadState.RightThumbstickX = x
            gamepadState.RightThumbstickY = y
        }
    }
}

private fun thumbstickButtonMask(type: AnalogStickType): Int = when (type) {
    AnalogStickType.LEFT -> GameButtons.LeftThumbstick.value
    AnalogStickType.RIGHT -> GameButtons.RightThumbstick.value
}

private fun resetAnalogStick(
    state: AnalogStickUiState,
    type: AnalogStickType,
    gamepadState: GamepadReading,
) {
    state.offsetX = 0f
    state.offsetY = 0f
    state.visualX = 0f
    state.visualY = 0f
    state.shiftX = 0f
    state.shiftY = 0f
    updateThumbstickValues(type, gamepadState, 0f, 0f)
}

private fun handleAnalogDrag(
    state: AnalogStickUiState,
    dragX: Float,
    dragY: Float,
    maxOffset: Float,
    type: AnalogStickType,
    gamepadState: GamepadReading,
    view: android.view.View,
    prefs: AppPrefs = AppPrefs(),
) {
    state.offsetX += dragX
    state.offsetY += dragY

    val magnitude = sqrt(state.offsetX * state.offsetX + state.offsetY * state.offsetY)
    val normalizedDistance = (magnitude / maxOffset).coerceIn(0f, 1f)
    if (normalizedDistance > 0.3f) {
        HapticUtils.performAnalogMovementFeedback(view, normalizedDistance)
    }

    if (magnitude <= 0f) {
        return
    }

    val scaleFactor = if (magnitude > maxOffset) maxOffset / magnitude else 1f
    state.visualX = state.offsetX * scaleFactor
    state.visualY = state.offsetY * scaleFactor
    val (sx, sy) = shaped(prefs, type, state.visualX / maxOffset, state.visualY / maxOffset)
    updateThumbstickValues(type, gamepadState, sx, sy)
}

private fun toggleThumbstickButton(
    state: AnalogStickUiState,
    type: AnalogStickType,
    gamepadState: GamepadReading,
    view: android.view.View,
) {
    val mask = thumbstickButtonMask(type)
    if (!state.isPressed) {
        state.isPressed = true
        gamepadState.ButtonsDown = gamepadState.ButtonsDown or mask
        HapticUtils.performButtonPressFeedback(view)
        return
    }

    state.isPressed = false
    gamepadState.ButtonsDown = gamepadState.ButtonsDown and mask.inv()
    gamepadState.ButtonsUp = gamepadState.ButtonsUp or mask
    HapticUtils.performGestureEndFeedback(view)
}

/**
 * An analog stick drawn like the browser controller's: a round base, a thin movement ring and a cap with a dot.
 * With snap on, touching anywhere inside the base puts the stick right there at once, sliding moves it, and
 * holding still for 450 ms presses the stick like a button.
 */
@Composable
fun AnalogStick(
    modifier: Modifier = Modifier,
    ringColor: Color = PsStyle.Border,
    ringWidth: Dp = 3.dp,
    outerCircleColor: Color = PsStyle.Body,
    outerCircleWidth: Dp = 4.dp,
    innerCircleColor: Color = PsStyle.StickCap,
    innerCircleRadius: Dp = 32.dp,
    gamepadState: GamepadReading,
    type: AnalogStickType,
    floating: Boolean = false,
) {
    val density = LocalDensity.current
    val view = LocalView.current
    val touchSlop = LocalViewConfiguration.current.touchSlop
    val prefs = LocalAppPrefs.current

    val state = remember { AnalogStickUiState() }
    val snap = LocalStickSnap.current
    val diameter = (innerCircleRadius + outerCircleWidth + ringWidth) * 2
    val diameterPx = with(density) { diameter.toPx() }
    // A floating stick listens in a wider circle than it draws, and its centre moves to where the thumb lands
    val region = if (floating) diameter * 1.9f else diameter
    val regionPx = with(density) { region.toPx() }
    val maxOffset = diameterPx * 0.28f
    val knobSize = diameter * 0.44f
    val pressedColor = PsStyle.Glyph

    Box(
        modifier = modifier
            .testTag("AnalogStick_${type.name}")
            .size(region)
            .then(
                if (!snap && !floating) Modifier else Modifier.pointerInput(type, floating) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val middle = Offset(size.width / 2f, size.height / 2f)
                        if (hypot(down.position.x - middle.x, down.position.y - middle.y) > (if (floating) regionPx else diameterPx) / 2f) return@awaitEachGesture
                        // Fixed sticks measure from the middle, floating ones from the first touch
                        val centre = if (floating) down.position else middle
                        down.consume()
                        if (floating) {
                            state.shiftX = centre.x - middle.x
                            state.shiftY = centre.y - middle.y
                        }
                        HapticUtils.performGestureStartFeedback(view)

                        fun place(position: Offset) {
                            var dx = position.x - centre.x
                            var dy = position.y - centre.y
                            val length = hypot(dx, dy)
                            if (length > maxOffset) {
                                dx *= maxOffset / length
                                dy *= maxOffset / length
                            }
                            state.offsetX = dx
                            state.offsetY = dy
                            state.visualX = dx
                            state.visualY = dy
                            val (sx, sy) = shaped(prefs, type, dx / maxOffset, dy / maxOffset)
                            updateThumbstickValues(type, gamepadState, sx, sy)
                            val normalized = (length / maxOffset).coerceIn(0f, 1f)
                            if (normalized > 0.3f) HapticUtils.performAnalogMovementFeedback(view, normalized)
                        }

                        try {
                            place(down.position)
                            var moved = false
                            var longPressed = false
                            val started = down.uptimeMillis
                            while (true) {
                                val wait = if (moved || longPressed) null else (prefs.clickHoldMs.toLong() - (android.os.SystemClock.uptimeMillis() - started)).coerceAtLeast(1L)
                                val event = if (wait == null) awaitPointerEvent() else withTimeoutOrNull(wait) { awaitPointerEvent() }
                                if (event == null) {
                                    longPressed = true
                                    if (prefs.clickEnabled) toggleThumbstickButton(state, type, gamepadState, view)
                                    continue
                                }
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                change.consume()
                                if (!change.pressed) break
                                if (hypot(change.position.x - down.position.x, change.position.y - down.position.y) > touchSlop) moved = true
                                place(change.position)
                            }
                        } finally {
                            resetAnalogStick(state, type, gamepadState)
                        }
                        HapticUtils.performGestureEndFeedback(view)
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (floating) {
            // The touch area, faint, so the player can see where the stick will appear
            Box(
                Modifier
                    .size(region)
                    .border(1.dp, ringColor.copy(alpha = 0.25f), CircleShape)
            )
        }
        Box(
            modifier = Modifier
                .size(diameter)
                .offset { IntOffset(state.shiftX.roundToInt(), state.shiftY.roundToInt()) },
            contentAlignment = Alignment.Center
        ) {
        // Base
        Box(
            Modifier
                .size(diameter)
                .clip(CircleShape)
                .background(outerCircleColor)
                .border(1.5.dp, if (state.isPressed) pressedColor else ringColor, CircleShape)
        )
        // Movement ring
        if (prefs.stickRing) Box(
            Modifier
                .size(diameter * 0.56f)
                .border(1.dp, ringColor.copy(alpha = 0.7f), CircleShape)
        )
        // Cap with its dot; a sibling of the base so it can move freely
        Box(
            modifier = Modifier
                .testTag("AnalogStick_${type.name}_Handle")
                .size(knobSize)
                .pointerInput(snap, floating) {
                    if (snap || floating) return@pointerInput
                    detectDragGestures(
                        onDragStart = { _ ->
                            HapticUtils.performGestureStartFeedback(view)
                        },
                        onDragEnd = {
                            resetAnalogStick(state, type, gamepadState)
                            HapticUtils.performGestureEndFeedback(view)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            handleAnalogDrag(
                                state = state,
                                dragX = dragAmount.x,
                                dragY = dragAmount.y,
                                maxOffset = maxOffset,
                                type = type,
                                gamepadState = gamepadState,
                                view = view,
                                prefs = prefs,
                            )
                        }
                    )
                }
                .pointerInput(snap, floating) {
                    if (snap || floating) return@pointerInput
                    detectTapGestures(
                        onLongPress = { _ ->
                            if (prefs.clickEnabled) toggleThumbstickButton(state, type, gamepadState, view)
                        }
                    )
                }
                .offset {
                    IntOffset(
                        state.visualX.roundToInt(),
                        state.visualY.roundToInt()
                    )
                }
                .clip(CircleShape)
                .background(if (state.isPressed) pressedColor else innerCircleColor)
                .border(1.5.dp, ringColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(knobSize * 0.32f)
                    .clip(CircleShape)
                    .background(ringColor)
            )
        }
        }
    }
}

@Preview(showBackground = false)
@Composable
fun AnalogStickPreview() {
    AnalogStick(
        gamepadState = GamepadReading(),
        type = AnalogStickType.LEFT,
    )
}
