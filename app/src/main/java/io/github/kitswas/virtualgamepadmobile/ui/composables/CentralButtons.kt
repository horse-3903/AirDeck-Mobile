package io.github.kitswas.virtualgamepadmobile.ui.composables

import io.github.kitswas.virtualgamepadmobile.data.GamepadStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.kitswas.VGP_Data_Exchange.GameButtons
import io.github.kitswas.VGP_Data_Exchange.GamepadReading
import io.github.kitswas.virtualgamepadmobile.R
import io.github.kitswas.virtualgamepadmobile.data.ButtonAnchor
import io.github.kitswas.virtualgamepadmobile.data.ButtonComponent
import io.github.kitswas.virtualgamepadmobile.data.ButtonConfig
import io.github.kitswas.virtualgamepadmobile.ui.utils.HapticUtils

/**
 * Convert ButtonAnchor to Compose Alignment
 */
private fun ButtonAnchor.toAlignment(): Alignment = when (this) {
    ButtonAnchor.TOP_LEFT -> Alignment.TopStart
    ButtonAnchor.TOP_CENTER -> Alignment.TopCenter
    ButtonAnchor.TOP_RIGHT -> Alignment.TopEnd
    ButtonAnchor.CENTER_LEFT -> Alignment.CenterStart
    ButtonAnchor.CENTER -> Alignment.Center
    ButtonAnchor.CENTER_RIGHT -> Alignment.CenterEnd
    ButtonAnchor.BOTTOM_LEFT -> Alignment.BottomStart
    ButtonAnchor.BOTTOM_CENTER -> Alignment.BottomCenter
    ButtonAnchor.BOTTOM_RIGHT -> Alignment.BottomEnd
}

enum class ShoulderButtonType {
    LEFT, RIGHT
}

enum class MenuButtonType {
    VIEW, MENU
}

@Composable
fun ShoulderButton(
    type: ShoulderButtonType,
    modifier: Modifier = Modifier,
    size: Dp,
    gamepadState: GamepadReading,
) {
    val view = LocalView.current
    val gameButton = when (type) {
        ShoulderButtonType.LEFT -> GameButtons.LeftShoulder
        ShoulderButtonType.RIGHT -> GameButtons.RightShoulder
    }
    val text = when (type) {
        ShoulderButtonType.LEFT -> stringResource(R.string.button_l_shoulder)
        ShoulderButtonType.RIGHT -> stringResource(R.string.button_r_shoulder)
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // See https://stackoverflow.com/a/69157877/8659747
    if (isPressed) {
        Log.d(gameButton.name, "Pressed")
        HapticUtils.performButtonPressFeedback(view)
        gamepadState.ButtonsDown = gamepadState.ButtonsDown or gameButton.value
        //Use if + DisposableEffect to wait for the press action is completed
        DisposableEffect(Unit) {
            onDispose {
                Log.d(gameButton.name, "Released")
                HapticUtils.performButtonReleaseFeedback(view)
                gamepadState.ButtonsDown =
                    gamepadState.ButtonsDown and gameButton.value.inv()
                gamepadState.ButtonsUp = gamepadState.ButtonsUp or gameButton.value
            }
        }
    }

    Button(
        shape = RoundedCornerShape(percent = 50),
        modifier = modifier
            .heightIn(min = size)
            .widthIn(min = size * 1.5f),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPressed) PsStyle.BodyPressed else PsStyle.Body,
            contentColor = PsStyle.Glyph
        ),
        border = BorderStroke(1.5.dp, if (isPressed) PsStyle.BorderPressed else PsStyle.Border),
        onClick = { },
        interactionSource = interactionSource,
    ) {
        Text(
            text = when (LocalGamepadStyle.current) {
                GamepadStyle.XBOX -> if (type == ShoulderButtonType.LEFT) "LB" else "RB"
                GamepadStyle.PLAYSTATION -> if (type == ShoulderButtonType.LEFT) "L1" else "R1"
            },
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

@Composable
fun BackButton(
    modifier: Modifier = Modifier,
    size: Dp,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    Button(
        shape = RoundedCornerShape(percent = 50),
        modifier = modifier
            .heightIn(min = size)
            .widthIn(min = size * 1.6f),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPressed) PsStyle.BodyPressed else PsStyle.Body,
            contentColor = PsStyle.Glyph
        ),
        border = BorderStroke(1.5.dp, if (isPressed) PsStyle.BorderPressed else PsStyle.Border),
        onClick = onClick,
        interactionSource = interactionSource,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_arrow_back),
            contentDescription = stringResource(R.string.back_button_label),
            modifier = Modifier.size(size / 2),
            tint = PsStyle.Glyph
        )
    }
}

@Composable
fun MenuButton(
    type: MenuButtonType,
    modifier: Modifier = Modifier,
    size: Dp,
    gamepadState: GamepadReading,
) {
    val view = LocalView.current
    val gameButton = when (type) {
        MenuButtonType.VIEW -> GameButtons.View
        MenuButtonType.MENU -> GameButtons.Menu
    }
    val iconPainter = when (type) {
        MenuButtonType.VIEW -> null
        MenuButtonType.MENU -> painterResource(R.drawable.ic_menu)
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // See https://stackoverflow.com/a/69157877/8659747
    if (isPressed) {
        Log.d(gameButton.name, "Pressed")
        HapticUtils.performButtonPressFeedback(view)
        gamepadState.ButtonsDown = gamepadState.ButtonsDown or gameButton.value
        //Use if + DisposableEffect to wait for the press action is completed
        DisposableEffect(Unit) {
            onDispose {
                Log.d(gameButton.name, "Released")
                HapticUtils.performButtonReleaseFeedback(view)
                gamepadState.ButtonsDown =
                    gamepadState.ButtonsDown and gameButton.value.inv()
                gamepadState.ButtonsUp = gamepadState.ButtonsUp or gameButton.value
            }
        }
    }

    OutlinedIconButton(
        modifier = modifier.size(size),
        colors = IconButtonDefaults.outlinedIconButtonColors(
            containerColor = if (isPressed) PsStyle.BodyPressed else PsStyle.Body,
        ),
        border = BorderStroke(1.5.dp, if (isPressed) PsStyle.BorderPressed else PsStyle.Border),
        onClick = { },
        interactionSource = interactionSource,
    ) {
        if (iconPainter != null) {
            Icon(
                painter = iconPainter,
                contentDescription = stringResource(R.string.content_desc_button, gameButton.name),
                modifier = Modifier.size(size / 2),
                tint = PsStyle.Glyph
            )
        } else {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.screenicon),
                contentDescription = stringResource(R.string.content_desc_button, gameButton.name),
                modifier = Modifier.size(size / 2),
                tint = PsStyle.Glyph
            )
        }
    }
}

/**
 * The central buttons section containing shoulder buttons (L/R bumpers) and menu buttons (View/Menu) for the gamepad.
 */
@Composable
fun CentralButtons(
    modifier: Modifier = Modifier,
    baseDp: Int,
    gamepadState: GamepadReading,
    buttonConfigs: Map<ButtonComponent, ButtonConfig>,
    onComponentTap: ((ButtonComponent) -> Unit)? = null,
    onComponentDrag: ((ButtonComponent, Dp, Dp) -> Unit)? = null,
    selectedComponent: ButtonComponent? = null,
    onBack: (() -> Unit)? = null,
) {
    // Helper function to get config for a component
    fun getConfig(component: ButtonComponent) =
        buttonConfigs[component] ?: ButtonConfig.default(component)

    // Helper function to render a button component with its anchor
    @Composable
    fun RenderButton(
        component: ButtonComponent,
        content: @Composable (ButtonConfig, Modifier) -> Unit
    ) {
        val config = getConfig(component)
        if (config.visible || onComponentTap != null) {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = config.anchor.toAlignment()
            ) {
                EditableComponent(
                    component, config, baseDp, onComponentTap, onComponentDrag,
                    selected = component == selectedComponent
                ) { mod ->
                    content(config, mod)
                }
            }
        }
    }

    // Left Shoulder button
    RenderButton(ButtonComponent.LEFT_SHOULDER) { config, mod ->
        ShoulderButton(
            type = ShoulderButtonType.LEFT,
            modifier = mod,
            size = (baseDp / 8 * config.scale).dp,
            gamepadState = gamepadState,
        )
    }

    // Right Shoulder button
    RenderButton(ButtonComponent.RIGHT_SHOULDER) { config, mod ->
        ShoulderButton(
            type = ShoulderButtonType.RIGHT,
            modifier = mod,
            size = (baseDp / 8 * config.scale).dp,
            gamepadState = gamepadState,
        )
    }

    // Select button (View)
    RenderButton(ButtonComponent.SELECT_BUTTON) { config, mod ->
        MenuButton(
            type = MenuButtonType.VIEW,
            modifier = mod,
            size = (baseDp / 8 * config.scale).dp,
            gamepadState = gamepadState,
        )
    }

    // Start button (Menu)
    RenderButton(ButtonComponent.START_BUTTON) { config, mod ->
        MenuButton(
            type = MenuButtonType.MENU,
            modifier = mod,
            size = (baseDp / 8 * config.scale).dp,
            gamepadState = gamepadState,
        )
    }

    // Back button (leaves the gamepad, never sent to the PC)
    RenderButton(ButtonComponent.BACK_BUTTON) { config, mod ->
        BackButton(
            modifier = mod,
            size = (baseDp / 8 * config.scale).dp,
            onClick = { onBack?.invoke() },
        )
    }
}

@Preview(showBackground = false)
@Composable
fun CentralButtonsPreview() {
    CentralButtons(
        baseDp = 400,
        gamepadState = GamepadReading(),
        buttonConfigs = io.github.kitswas.virtualgamepadmobile.data.defaultButtonConfigs,
    )
}
