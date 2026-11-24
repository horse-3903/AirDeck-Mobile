package io.github.kitswas.virtualgamepadmobile.ui.composables

import io.github.kitswas.virtualgamepadmobile.data.GamepadStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.AbsoluteCutCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.kitswas.VGP_Data_Exchange.GamepadReading
import io.github.kitswas.virtualgamepadmobile.data.TriggerMode
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import io.github.kitswas.virtualgamepadmobile.R
import io.github.kitswas.virtualgamepadmobile.ui.utils.HapticUtils

// Round at the top like a real trigger, flatter where the finger rests
private val TriggerShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 8.dp, bottomEnd = 8.dp)

enum class TriggerType {
    LEFT, RIGHT
}

@Composable
fun Trigger(
    type: TriggerType,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    gamepadState: GamepadReading,
    mode: TriggerMode = TriggerMode.DIGITAL,
) {
    if (mode == TriggerMode.DIGITAL) DigitalTrigger(type, modifier, size, gamepadState)
    else AnalogTrigger(type, modifier, size, gamepadState, mode)
}

@Composable
private fun DigitalTrigger(
    type: TriggerType,
    modifier: Modifier,
    size: Dp,
    gamepadState: GamepadReading,
) {
    val view = LocalView.current

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    DisposableEffect(isPressed) {
        if (isPressed) {
            Log.d("TriggerButton", "Trigger ${type.name} pressed")
            when (type) {
                TriggerType.LEFT -> gamepadState.LeftTrigger = 1f
                TriggerType.RIGHT -> gamepadState.RightTrigger = 1f
            }
            HapticUtils.performButtonPressFeedback(view)
        }
        onDispose {
            Log.d("TriggerButton", "Trigger ${type.name} released")
            when (type) {
                TriggerType.LEFT -> gamepadState.LeftTrigger = 0f
                TriggerType.RIGHT -> gamepadState.RightTrigger = 0f
            }
            HapticUtils.performButtonReleaseFeedback(view)
        }
    }

    val label = when (type) {
        TriggerType.LEFT -> stringResource(R.string.button_lt)
        TriggerType.RIGHT -> stringResource(R.string.button_rt)
    }

    Box(
        modifier = modifier
            .width(size)
            .height(size * 1.25f)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = {},
            modifier = Modifier.fillMaxSize(),
            shape = TriggerShape,
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isPressed) PsStyle.BodyPressed else PsStyle.Body,
                contentColor = PsStyle.Glyph
            ),
            border = BorderStroke(1.5.dp, if (isPressed) PsStyle.BorderPressed else PsStyle.Border),
            interactionSource = interactionSource,
        ) {
            Text(
                text = when (LocalGamepadStyle.current) {
                    GamepadStyle.XBOX -> if (type == TriggerType.LEFT) "LT" else "RT"
                    GamepadStyle.PLAYSTATION -> if (type == TriggerType.LEFT) "L2" else "R2"
                },
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

@Composable
private fun AnalogTrigger(
    type: TriggerType,
    modifier: Modifier,
    size: Dp,
    gamepadState: GamepadReading,
    mode: TriggerMode,
) {
    val view = LocalView.current
    val context = LocalContext.current
    var level by remember { mutableFloatStateOf(0f) }
    var held by remember { mutableStateOf(false) }

    fun write(v: Float) {
        when (type) {
            TriggerType.LEFT -> gamepadState.LeftTrigger = v
            TriggerType.RIGHT -> gamepadState.RightTrigger = v
        }
    }

    // Small deadzone at the bottom, snap to full near the top, a tick at each quarter
    fun set(raw: Float) {
        val v = when {
            raw < 0.05f -> 0f
            raw >= 0.9f -> 1f
            else -> raw
        }
        if ((v * 4).toInt() > (level * 4).toInt()) HapticUtils.performTriggerTick(view)
        level = v
        write(v)
    }

    DisposableEffect(mode) {
        onDispose { write(0f) }
    }

    LaunchedEffect(held, mode) {
        if (mode == TriggerMode.RAMP && held) {
            animate(0f, 1f, animationSpec = tween(300, easing = LinearEasing)) { v, _ -> set(v) }
        }
    }

    DisposableEffect(held, mode) {
        var listener: SensorEventListener? = null
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        if (held && mode == TriggerMode.TILT && manager != null) {
            val sensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            var base: FloatArray? = null
            val smooth = FloatArray(3)
            listener = object : SensorEventListener {
                override fun onSensorChanged(e: SensorEvent) {
                    for (i in 0..2) smooth[i] += 0.3f * (e.values[i] - smooth[i])
                    val b = base ?: smooth.copyOf().also { base = it }
                    val dot = smooth[0] * b[0] + smooth[1] * b[1] + smooth[2] * b[2]
                    val na = Math.sqrt((smooth[0] * smooth[0] + smooth[1] * smooth[1] + smooth[2] * smooth[2]).toDouble())
                    val nb = Math.sqrt((b[0] * b[0] + b[1] * b[1] + b[2] * b[2]).toDouble())
                    if (na < 1e-3 || nb < 1e-3) return
                    val deg = Math.toDegrees(Math.acos((dot / (na * nb)).coerceIn(-1.0, 1.0)))
                    set((deg / 30.0).toFloat())
                }

                override fun onAccuracyChanged(s: Sensor?, a: Int) {}
            }
            manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
        onDispose { listener?.let { manager?.unregisterListener(it) } }
    }

    val shape = TriggerShape
    val label = when (LocalGamepadStyle.current) {
        GamepadStyle.XBOX -> if (type == TriggerType.LEFT) "LT" else "RT"
        GamepadStyle.PLAYSTATION -> if (type == TriggerType.LEFT) "L2" else "R2"
    }

    Box(
        modifier = modifier
            .width(size)
            .height(size * 2f)
            .clip(shape)
            .background(PsStyle.Body)
            .border(1.5.dp, if (held) PsStyle.BorderPressed else PsStyle.Border, shape)
            .pointerInput(mode) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    held = true
                    HapticUtils.performButtonPressFeedback(view)
                    val h = this.size.height.toFloat().coerceAtLeast(1f)
                    val y0 = down.position.y
                    fun handle(y: Float, pressure: Float) {
                        when (mode) {
                            TriggerMode.SLIDE -> set(0.1f + 0.9f * (1f - y / h).coerceIn(0f, 1f))
                            TriggerMode.DRAG -> set(0.2f + 0.8f * ((y0 - y) / (h * 0.6f)).coerceIn(0f, 1f))
                            TriggerMode.ZONES -> set(if (y < h / 2f) 1f else 0.5f)
                            TriggerMode.FORCE -> set((pressure * 1.6f).coerceIn(0.1f, 1f))
                            else -> {}
                        }
                    }
                    handle(down.position.y, down.pressure)
                    while (true) {
                        val ev = awaitPointerEvent()
                        val c = ev.changes.firstOrNull { it.id == down.id } ?: break
                        c.consume()
                        if (!c.pressed) break
                        handle(c.position.y, c.pressure)
                    }
                    held = false
                    set(0f)
                    HapticUtils.performButtonReleaseFeedback(view)
                }
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(level)
                .background(PsStyle.BorderPressed.copy(alpha = 0.55f))
        )
        if (mode == TriggerMode.ZONES) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(PsStyle.Border)
            )
        }
        Text(
            text = if (held) "$label  ${(level * 100).toInt()}%" else label,
            modifier = Modifier.align(Alignment.Center),
            color = PsStyle.Glyph,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

@Preview
@Composable
fun TriggerPreview() {
    Row {
        Trigger(
            type = TriggerType.LEFT,
            size = 64.dp,
            gamepadState = GamepadReading()
        )
        Trigger(
            type = TriggerType.RIGHT,
            size = 64.dp,
            gamepadState = GamepadReading()
        )
    }
}
