package io.github.kitswas.virtualgamepadmobile.ui.composables

import androidx.compose.foundation.shape.RoundedCornerShape
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs
import kotlin.math.hypot
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import io.github.kitswas.VGP_Data_Exchange.GameButtons
import io.github.kitswas.VGP_Data_Exchange.GamepadReading
import io.github.kitswas.virtualgamepadmobile.R
import io.github.kitswas.virtualgamepadmobile.ui.theme.darken
import io.github.kitswas.virtualgamepadmobile.ui.utils.HapticUtils

enum class DpadButtonType {
    UP, DOWN, LEFT, RIGHT
}

// Only the outer corners are rounded so the four arms read as one cross
private fun armShape(type: DpadButtonType): RoundedCornerShape {
    val r = 14.dp
    return when (type) {
        DpadButtonType.UP -> RoundedCornerShape(topStart = r, topEnd = r)
        DpadButtonType.DOWN -> RoundedCornerShape(bottomStart = r, bottomEnd = r)
        DpadButtonType.LEFT -> RoundedCornerShape(topStart = r, bottomStart = r)
        DpadButtonType.RIGHT -> RoundedCornerShape(topEnd = r, bottomEnd = r)
    }
}

@Composable
fun DpadButton(
    type: DpadButtonType,
    modifier: Modifier = Modifier,
    foregroundColour: Color = MaterialTheme.colorScheme.primary,
    backgroundColour: Color = darken(MaterialTheme.colorScheme.primary, 0.8f),
    size: Dp,
    gamepadState: GamepadReading,
    forcePressed: Boolean? = null,
) {
    val view = LocalView.current
    val rotation = when (type) {
        DpadButtonType.UP -> -90f
        DpadButtonType.DOWN -> 90f
        DpadButtonType.LEFT -> 180f
        DpadButtonType.RIGHT -> 0f
    }
    val gameButton = when (type) {
        DpadButtonType.UP -> GameButtons.DPadUp
        DpadButtonType.DOWN -> GameButtons.DPadDown
        DpadButtonType.LEFT -> GameButtons.DPadLeft
        DpadButtonType.RIGHT -> GameButtons.DPadRight
    }
    val interactionSource = remember { MutableInteractionSource() }
    val touchPressed by interactionSource.collectIsPressedAsState()
    val isPressed = forcePressed ?: touchPressed
    // See https://stackoverflow.com/a/69157877/8659747
    if (forcePressed == null && touchPressed) {
        Log.d("DPadButton ${type.name}", "Pressed")
        HapticUtils.performButtonPressFeedback(view)
        gamepadState.ButtonsDown = gamepadState.ButtonsDown or gameButton.value
        //Use if + DisposableEffect to wait for the press action is completed
        DisposableEffect(Unit) {
            onDispose {
                Log.d("DPadButton ${type.name}", "Released")
                HapticUtils.performButtonReleaseFeedback(view)
                gamepadState.ButtonsDown = gamepadState.ButtonsDown and gameButton.value.inv()
                gamepadState.ButtonsUp = gamepadState.ButtonsUp or gameButton.value
            }
        }
    }
    OutlinedIconButton(
        modifier = modifier
            .size(size)
            .padding(0.dp),
        onClick = {},
        shape = armShape(type),
        colors = IconButtonDefaults.outlinedIconButtonColors(
            containerColor = if (isPressed) PsStyle.BodyPressed else PsStyle.Body,
        ),
        border = BorderStroke(1.5.dp, if (isPressed) PsStyle.BorderPressed else PsStyle.Border),
        interactionSource = interactionSource,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_play_arrow),
            contentDescription = stringResource(R.string.content_desc_dpad_button, type.name),
            modifier = Modifier
                .rotate(rotation)
                .size(size * 0.5f),
            tint = PsStyle.Glyph
        )
    }
}

private fun dpadBit(type: DpadButtonType) = when (type) {
    DpadButtonType.UP -> GameButtons.DPadUp.value
    DpadButtonType.DOWN -> GameButtons.DPadDown.value
    DpadButtonType.LEFT -> GameButtons.DPadLeft.value
    DpadButtonType.RIGHT -> GameButtons.DPadRight.value
}

/**
 * A directional pad with up, down, left, and right buttons. In orthogonal mode one touch anywhere on the pad
 * gives exactly one direction (never a diagonal) and sliding the thumb switches direction.
 */
@Composable
fun Dpad(
    modifier: Modifier = Modifier,
    size: Dp = 360.dp,
    gamepadState: GamepadReading,
) {
    val orthogonal = LocalDpadOrthogonal.current
    val view = LocalView.current
    val buttonSize = size * 0.34f
    var active by remember { mutableStateOf<DpadButtonType?>(null) }

    fun setDirection(next: DpadButtonType?) {
        if (next == active) return
        active?.let {
            gamepadState.ButtonsDown = gamepadState.ButtonsDown and dpadBit(it).inv()
            gamepadState.ButtonsUp = gamepadState.ButtonsUp or dpadBit(it)
        }
        if (next != null) {
            HapticUtils.performButtonPressFeedback(view)
            gamepadState.ButtonsDown = gamepadState.ButtonsDown or dpadBit(next)
        } else {
            HapticUtils.performButtonReleaseFeedback(view)
        }
        active = next
    }

    val touchModifier = if (orthogonal) {
        Modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                fun pick(position: Offset): DpadButtonType? {
                    val dx = position.x - this@pointerInput.size.width / 2f
                    val dy = position.y - this@pointerInput.size.height / 2f
                    if (hypot(dx, dy) < this@pointerInput.size.width * 0.07f) return null
                    val ax = abs(dx)
                    val ay = abs(dy)
                    val horizontal = when (active) {
                        DpadButtonType.LEFT, DpadButtonType.RIGHT -> ay <= ax * 1.25f
                        DpadButtonType.UP, DpadButtonType.DOWN -> ax > ay * 1.25f
                        null -> ax > ay
                    }
                    return if (horizontal) {
                        if (dx < 0) DpadButtonType.LEFT else DpadButtonType.RIGHT
                    } else {
                        if (dy < 0) DpadButtonType.UP else DpadButtonType.DOWN
                    }
                }
                down.consume()
                setDirection(pick(down.position))
                do {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    change.consume()
                    if (change.pressed) setDirection(pick(change.position))
                } while (change.pressed)
                setDirection(null)
            }
        }
    } else {
        Modifier
    }

    Box(modifier = modifier.size(size).then(touchModifier), contentAlignment = Alignment.Center) {
        // Fills the middle of the cross
        Box(
            modifier = Modifier
                .size(buttonSize)
                .background(PsStyle.Body)
        )
        DpadButton(
            type = DpadButtonType.UP,
            modifier = Modifier.align(Alignment.TopCenter),
            size = buttonSize,
            gamepadState = gamepadState,
            forcePressed = if (orthogonal) active == DpadButtonType.UP else null,
        )
        DpadButton(
            type = DpadButtonType.DOWN,
            modifier = Modifier.align(Alignment.BottomCenter),
            size = buttonSize,
            gamepadState = gamepadState,
            forcePressed = if (orthogonal) active == DpadButtonType.DOWN else null,
        )
        DpadButton(
            type = DpadButtonType.LEFT,
            modifier = Modifier.align(Alignment.CenterStart),
            size = buttonSize,
            gamepadState = gamepadState,
            forcePressed = if (orthogonal) active == DpadButtonType.LEFT else null,
        )
        DpadButton(
            type = DpadButtonType.RIGHT,
            modifier = Modifier.align(Alignment.CenterEnd),
            size = buttonSize,
            gamepadState = gamepadState,
            forcePressed = if (orthogonal) active == DpadButtonType.RIGHT else null,
        )
    }
}

@Preview(showBackground = false)
@Composable
fun DpadPreview() {
    Dpad(
        gamepadState = GamepadReading(),
    )
}
