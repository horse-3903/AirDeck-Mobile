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
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
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
        FaceGlyph(type = type, xbox = isXbox, color = glyphColor, modifier = Modifier.size(size * 0.49f))
    }
}

/** The same stroke symbols and letters the browser controller draws, on a 24 unit grid. */
@Composable
private fun FaceGlyph(type: FaceButtonType, xbox: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        scale(this.size.minDimension / 24f, pivot = Offset.Zero) {
            if (xbox) {
                val stroke = Stroke(width = 2.6f, cap = StrokeCap.Square, join = StrokeJoin.Miter)
                val d = when (type) {
                    FaceButtonType.Y -> "M5.5 4 L12 13 L18.5 4 M12 13 V20.5"
                    FaceButtonType.X -> "M6.3 5.2 L17.7 18.8 M17.7 5.2 L6.3 18.8"
                    FaceButtonType.B -> "M7 4 V20 M7 4 H12 A4 4 0 0 1 12 12 H7 M7 12 H13 A4 4 0 0 1 13 20 H7"
                    FaceButtonType.A -> "M4.5 20.5 L12 3.5 L19.5 20.5 M7.8 14.5 H16.2"
                }
                drawPath(PathParser().parsePathString(d).toPath(), color, style = stroke)
            } else {
                val round = Stroke(width = 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                when (type) {
                    FaceButtonType.A -> drawPath(PathParser().parsePathString("M5 5 L19 19 M19 5 L5 19").toPath(), color, style = round)
                    FaceButtonType.B -> drawCircle(color, radius = 8f, center = Offset(12f, 12f), style = round)
                    FaceButtonType.X -> drawPath(PathParser().parsePathString("M4.5 4.5 H19.5 V19.5 H4.5 Z").toPath(), color, style = round)
                    FaceButtonType.Y -> drawPath(PathParser().parsePathString("M12 4 L21 19 H3 Z").toPath(), color, style = round)
                }
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
            size = size * 0.316f,
            gamepadState = gamepadState,
        )
        FaceButton(
            type = FaceButtonType.B,
            modifier = Modifier.align(Alignment.CenterEnd),
            size = size * 0.316f,
            gamepadState = gamepadState,
        )
        FaceButton(
            type = FaceButtonType.X,
            modifier = Modifier.align(Alignment.CenterStart),
            size = size * 0.316f,
            gamepadState = gamepadState,
        )
        FaceButton(
            type = FaceButtonType.Y,
            modifier = Modifier.align(Alignment.TopCenter),
            size = size * 0.316f,
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
