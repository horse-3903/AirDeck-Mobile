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
import io.github.kitswas.virtualgamepadmobile.R
import io.github.kitswas.virtualgamepadmobile.ui.utils.HapticUtils

enum class TriggerType {
    LEFT, RIGHT
}

@Composable
fun Trigger(
    type: TriggerType,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
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
            shape = RoundedCornerShape(16.dp),
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
