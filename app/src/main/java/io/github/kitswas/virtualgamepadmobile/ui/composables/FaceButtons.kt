package io.github.kitswas.virtualgamepadmobile.ui.composables

import io.github.kitswas.virtualgamepadmobile.data.GamepadStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.Canvas
import android.util.Log
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import io.github.kitswas.VGP_Data_Exchange.GameButtons
import io.github.kitswas.VGP_Data_Exchange.GamepadReading
import io.github.kitswas.virtualgamepadmobile.R
import io.github.kitswas.virtualgamepadmobile.data.PreviewBase
import io.github.kitswas.virtualgamepadmobile.ui.theme.darken
import io.github.kitswas.virtualgamepadmobile.ui.theme.faceButtonTextStyle
import io.github.kitswas.virtualgamepadmobile.ui.theme.lighten
import io.github.kitswas.virtualgamepadmobile.ui.utils.HapticUtils


enum class FaceButtonType {
    A, B, X, Y
}

private val faceButtonColourMap = mapOf(
    FaceButtonType.A to Color(0xFF00FF00),
    FaceButtonType.B to Color(0xFFFF0000),
    FaceButtonType.X to Color(0xFF0055FF),
    FaceButtonType.Y to Color(0xFFFFFF00),
)

@Composable
fun FaceButton(
    type: FaceButtonType,
    modifier: Modifier = Modifier,
    foregroundColour: Color = lighten(faceButtonColourMap[type]!!, 0.2f),
    backgroundColour: Color = darken(faceButtonColourMap[type]!!, 0.8f),
    size: Dp,
    gamepadState: GamepadReading,
) {
    val view = LocalView.current
    val gameButton = when (type) {
        FaceButtonType.A -> GameButtons.A
        FaceButtonType.B -> GameButtons.B
        FaceButtonType.X -> GameButtons.X
        FaceButtonType.Y -> GameButtons.Y
    }
    val label = when (type) {
        FaceButtonType.A -> stringResource(R.string.button_a)
        FaceButtonType.B -> stringResource(R.string.button_b)
        FaceButtonType.X -> stringResource(R.string.button_x)
        FaceButtonType.Y -> stringResource(R.string.button_y)
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    // See https://stackoverflow.com/a/69157877/8659747
    if (isPressed) {
        Log.d("FaceButton ${type.name}", "Pressed")
        HapticUtils.performButtonPressFeedback(view)
        gamepadState.ButtonsDown = gamepadState.ButtonsDown or gameButton.value
        //Use if + DisposableEffect to wait for the press action is completed
        DisposableEffect(Unit) {
            onDispose {
                Log.d("FaceButton ${type.name}", "Released")
                HapticUtils.performButtonReleaseFeedback(view)
                gamepadState.ButtonsDown = gamepadState.ButtonsDown and gameButton.value.inv()
                gamepadState.ButtonsUp = gamepadState.ButtonsUp or gameButton.value
            }
        }
    }
    val isXbox = LocalGamepadStyle.current == GamepadStyle.XBOX
    val glyphColor = when (type) {
        FaceButtonType.A -> if (isXbox) PsStyle.XboxA else PsStyle.Cross
        FaceButtonType.B -> if (isXbox) PsStyle.XboxB else PsStyle.Circle
        FaceButtonType.X -> if (isXbox) PsStyle.XboxX else PsStyle.Square
        FaceButtonType.Y -> if (isXbox) PsStyle.XboxY else PsStyle.Triangle
    }
    OutlinedButton(
        shape = CircleShape,
        modifier = modifier
            .size(size)
            .padding(0.dp)
            .semantics { contentDescription = label },
        onClick = {},
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isPressed) glyphColor.copy(alpha = 0.22f) else PsStyle.Body,
        ),
        interactionSource = interactionSource,
        border = BorderStroke(1.5.dp, if (isPressed) glyphColor else PsStyle.Border),
    ) {
        if (isXbox) {
            Text(
                text = label,
                color = glyphColor,
                style = TextStyle(
                    fontSize = (size.value * 0.4f).sp,
                    fontWeight = FontWeight.Bold
                )
            )
        } else {
            FaceGlyph(type = type, color = glyphColor, modifier = Modifier.size(size * 0.4f))
        }
    }
}

@Composable
private fun FaceGlyph(type: FaceButtonType, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = this.size.minDimension
        val strokeWidth = w * 0.13f
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (type) {
            FaceButtonType.A -> {
                drawLine(color, Offset(w * 0.1f, w * 0.1f), Offset(w * 0.9f, w * 0.9f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.9f, w * 0.1f), Offset(w * 0.1f, w * 0.9f), strokeWidth, StrokeCap.Round)
            }

            FaceButtonType.B -> drawCircle(color, radius = w * 0.42f, style = stroke)
            FaceButtonType.X -> drawRoundRect(
                color,
                topLeft = Offset(w * 0.1f, w * 0.1f),
                size = Size(w * 0.8f, w * 0.8f),
                cornerRadius = CornerRadius(w * 0.08f),
                style = stroke
            )

            FaceButtonType.Y -> {
                val path = Path().apply {
                    moveTo(w * 0.5f, w * 0.08f)
                    lineTo(w * 0.94f, w * 0.88f)
                    lineTo(w * 0.06f, w * 0.88f)
                    close()
                }
                drawPath(path, color, style = stroke)
            }
        }
    }
}

/**
 * The A, B, X, Y buttons on a gamepad, also known as the face buttons.
 */
@Composable
fun FaceButtons(
    modifier: Modifier = Modifier,
    size: Dp = 360.dp,
    gamepadState: GamepadReading,
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        FaceButton(
            type = FaceButtonType.A,
            modifier = Modifier.align(Alignment.BottomCenter),
            size = 2 * size / 5,
            gamepadState = gamepadState,
        )
        FaceButton(
            type = FaceButtonType.B,
            modifier = Modifier.align(Alignment.CenterEnd),
            size = 2 * size / 5,
            gamepadState = gamepadState,
        )
        FaceButton(
            type = FaceButtonType.X,
            modifier = Modifier.align(Alignment.CenterStart),
            size = 2 * size / 5,
            gamepadState = gamepadState,
        )
        FaceButton(
            type = FaceButtonType.Y,
            modifier = Modifier.align(Alignment.TopCenter),
            size = 2 * size / 5,
            gamepadState = gamepadState,
        )
    }
}

@Preview(showBackground = false)
@Composable
fun FaceButtonsPreview() {
    PreviewBase {
        FaceButtons(
            gamepadState = GamepadReading(),
        )
    }
}
