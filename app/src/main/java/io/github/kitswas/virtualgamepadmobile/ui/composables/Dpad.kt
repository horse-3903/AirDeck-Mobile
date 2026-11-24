package io.github.kitswas.virtualgamepadmobile.ui.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.kitswas.VGP_Data_Exchange.GameButtons
import io.github.kitswas.VGP_Data_Exchange.GamepadReading
import io.github.kitswas.virtualgamepadmobile.data.GamepadStyle
import io.github.kitswas.virtualgamepadmobile.ui.utils.HapticUtils
import kotlin.math.abs
import kotlin.math.hypot

enum class DpadButtonType {
    UP, DOWN, LEFT, RIGHT
}

private fun dpadBit(type: DpadButtonType) = when (type) {
    DpadButtonType.UP -> GameButtons.DPadUp.value
    DpadButtonType.DOWN -> GameButtons.DPadDown.value
    DpadButtonType.LEFT -> GameButtons.DPadLeft.value
    DpadButtonType.RIGHT -> GameButtons.DPadRight.value
}

private class DpadColors(light: Boolean) {
    val disc = if (light) Color(0xFFE2E2E8) else Color(0xFF0E0E12)
    val border = if (light) Color(0xFFC4C4CC) else Color(0xFF34343C)
    val arm = if (light) Color(0xFFCFCFD8) else Color(0xFF17171D)
    val armEdge = if (light) Color(0xFF8A8A96) else Color(0xFF34343D)
    val key = if (light) Color(0xFFB9B9C6) else Color(0xFF2A2A32)
    val keyEdge = if (light) Color(0xFF7D7D8A) else Color(0xFF4A4A55)
    val keyOn = if (light) Color(0xFF8E8E9C) else Color(0xFF6A6A78)
}

private const val CROSS = "M33 -0.20A53 53 0 0 1 67 -0.20V33H100.20A53 53 0 0 1 100.20 67H67V100.20A53 53 0 0 1 33 100.20V67H-0.20A53 53 0 0 1 -0.20 33H33Z"
private const val HL_UP = "M33 -0.2A53 53 0 0 1 67 -0.2V33H33Z"
private const val HL_RIGHT = "M100.2 33A53 53 0 0 1 100.2 67H67V33Z"
private const val HL_DOWN = "M67 100.2A53 53 0 0 1 33 100.2V67H67Z"
private const val HL_LEFT = "M-0.2 67A53 53 0 0 1 -0.2 33H33V67Z"
private const val KEY = "M8 5.6Q8 2.6 11 2.6H25Q28 2.6 28 5.6V24.5L18.7 33.8Q18 34.5 17.3 33.8L8 24.5Z"

private fun path(d: String): Path = PathParser().parsePathString(d).toPath()

/** One round base with a raised plus whose four arms light when pressed. */
private fun DrawScope.drawXboxDpad(colors: DpadColors, pressed: Set<DpadButtonType>) {
    val s = size.minDimension
    translate(s * 0.041f, s * 0.041f) {
        scale(s * 0.918f / 112f, pivot = Offset.Zero) {
            translate(6f, 6f) {
                drawCircle(colors.disc, radius = 49.2f, center = Offset(50f, 50f))
                drawCircle(colors.border, radius = 49.2f, center = Offset(50f, 50f), style = Stroke(1.4f))
                val cross = path(CROSS)
                drawPath(cross, colors.arm)
                drawPath(cross, colors.armEdge, style = Stroke(1.6f, join = StrokeJoin.Round))
                val white = Color.White.copy(alpha = 0.22f)
                if (DpadButtonType.UP in pressed) drawPath(path(HL_UP), white)
                if (DpadButtonType.RIGHT in pressed) drawPath(path(HL_RIGHT), white)
                if (DpadButtonType.DOWN in pressed) drawPath(path(HL_DOWN), white)
                if (DpadButtonType.LEFT in pressed) drawPath(path(HL_LEFT), white)
            }
        }
    }
    // Four small arrow heads on the diagonals
    val t = s * 0.06f
    val tri = Path().apply {
        moveTo(t / 2f, 0f); lineTo(t, t); lineTo(0f, t); close()
    }
    for ((x, y, angle) in listOf(
        Triple(0.20f, 0.20f, -45f), Triple(0.74f, 0.20f, 45f),
        Triple(0.20f, 0.74f, -135f), Triple(0.74f, 0.74f, 135f)
    )) {
        translate(s * x, s * y) {
            rotate(angle, pivot = Offset(t / 2f, t / 2f)) { drawPath(tri, colors.border) }
        }
    }
}

/** A round base with four separate keys that point at the centre. */
private fun DrawScope.drawPlayStationDpad(colors: DpadColors, pressed: Set<DpadButtonType>) {
    val s = size.minDimension
    scale(s / 100f, pivot = Offset.Zero) {
        scale(0.84f, pivot = Offset(50f, 50f)) {
            drawCircle(colors.disc, radius = 49f, center = Offset(50f, 50f))
            drawCircle(colors.border, radius = 49f, center = Offset(50f, 50f), style = Stroke(1.5f))
            for (d in listOf(
                "M50 2.5L53.5 7.5Q50 6.2 46.5 7.5Z", "M50 97.5L53.5 92.5Q50 93.8 46.5 92.5Z",
                "M2.5 50L7.5 46.5Q6.2 50 7.5 53.5Z", "M97.5 50L92.5 46.5Q93.8 50 92.5 53.5Z"
            )) drawPath(path(d), colors.border)
            val key = path(KEY)
            for ((type, x, y, rot) in listOf(
                Quad(DpadButtonType.UP, 31f, 9.5f, 0f), Quad(DpadButtonType.RIGHT, 52.5f, 31f, 90f),
                Quad(DpadButtonType.DOWN, 31f, 52.5f, 180f), Quad(DpadButtonType.LEFT, 9.5f, 31f, -90f)
            )) {
                translate(x, y) {
                    scale(1.0556f, pivot = Offset.Zero) {
                        rotate(rot, pivot = Offset(18f, 18f)) {
                            drawPath(key, if (type in pressed) colors.keyOn else colors.key)
                            drawPath(key, colors.keyEdge, style = Stroke(1.2f, join = StrokeJoin.Round))
                        }
                    }
                }
            }
        }
    }
}

private data class Quad(val type: DpadButtonType, val x: Float, val y: Float, val rot: Float)

/**
 * The directional pad. Each touch on it picks its direction from where the finger is, and sliding switches
 * direction. By default several directions can be held at once: a finger near a diagonal holds two, and two
 * fingers hold two. In single mode only one of up, down, left, right is ever held.
 */
@Composable
fun Dpad(
    modifier: Modifier = Modifier,
    size: Dp = 360.dp,
    gamepadState: GamepadReading,
) {
    val single = LocalDpadSingle.current
    val style = LocalGamepadStyle.current
    val light = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val colors = remember(light) { DpadColors(light) }
    val view = LocalView.current
    var active by remember { mutableStateOf<Set<DpadButtonType>>(emptySet()) }

    fun setDirections(next: Set<DpadButtonType>) {
        if (next == active) return
        for (gone in active - next) {
            gamepadState.ButtonsDown = gamepadState.ButtonsDown and dpadBit(gone).inv()
            gamepadState.ButtonsUp = gamepadState.ButtonsUp or dpadBit(gone)
        }
        for (added in next - active) {
            gamepadState.ButtonsDown = gamepadState.ButtonsDown or dpadBit(added)
        }
        if (next.isNotEmpty()) HapticUtils.performButtonPressFeedback(view) else HapticUtils.performButtonReleaseFeedback(view)
        active = next
    }

    Box(
        modifier = modifier
            .size(size)
            .pointerInput(single) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    fun pick(position: Offset): Set<DpadButtonType> {
                        val dx = position.x - this@pointerInput.size.width / 2f
                        val dy = position.y - this@pointerInput.size.height / 2f
                        if (hypot(dx, dy) < this@pointerInput.size.width * 0.07f) return emptySet()
                        val ax = abs(dx)
                        val ay = abs(dy)
                        if (!single) {
                            val result = mutableSetOf<DpadButtonType>()
                            val d = hypot(dx, dy)
                            if (dy / d < -0.38f) result += DpadButtonType.UP
                            if (dy / d > 0.38f) result += DpadButtonType.DOWN
                            if (dx / d < -0.38f) result += DpadButtonType.LEFT
                            if (dx / d > 0.38f) result += DpadButtonType.RIGHT
                            return result
                        }
                        val current = active.singleOrNull()
                        val horizontal = when (current) {
                            DpadButtonType.LEFT, DpadButtonType.RIGHT -> ay <= ax * 1.25f
                            DpadButtonType.UP, DpadButtonType.DOWN -> ax > ay * 1.25f
                            null -> ax > ay
                        }
                        return setOf(
                            if (horizontal) {
                                if (dx < 0) DpadButtonType.LEFT else DpadButtonType.RIGHT
                            } else {
                                if (dy < 0) DpadButtonType.UP else DpadButtonType.DOWN
                            }
                        )
                    }
                    down.consume()
                    setDirections(pick(down.position))
                    // Every finger on the pad adds its direction, so two fingers hold two directions
                    var firstId = down.id
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        event.changes.forEach { it.consume() }
                        var pressed = event.changes.filter { it.pressed }
                        if (single) pressed = pressed.filter { it.id == firstId }.ifEmpty { pressed.take(1) }
                        if (single && pressed.isNotEmpty()) firstId = pressed.first().id
                        val union = pressed.flatMap { pick(it.position) }.toSet()
                        setDirections(union)
                    } while (event.changes.any { it.pressed })
                    setDirections(emptySet())
                }
            }
    ) {
        Canvas(Modifier.size(size)) {
            if (style == GamepadStyle.XBOX) drawXboxDpad(colors, active) else drawPlayStationDpad(colors, active)
        }
    }
}

@Preview(showBackground = false)
@Composable
fun DpadPreview() {
    Dpad(
        gamepadState = GamepadReading(),
    )
}
