package io.github.kitswas.virtualgamepadmobile.ui.composables

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.kitswas.VGP_Data_Exchange.GamepadReading
import io.github.kitswas.virtualgamepadmobile.data.ButtonAnchor
import io.github.kitswas.virtualgamepadmobile.data.ButtonComponent
import io.github.kitswas.virtualgamepadmobile.data.ButtonConfig

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

@Composable
fun DrawGamepad(
    widthDp: Int,
    heightDp: Int,
    gamepadState: GamepadReading,
    buttonConfigs: Map<ButtonComponent, ButtonConfig>,
    onComponentTap: ((ButtonComponent) -> Unit)? = null,
    onComponentDrag: ((ButtonComponent, Dp, Dp) -> Unit)? = null,
    selectedComponent: ButtonComponent? = null,
    onBack: (() -> Unit)? = null,
    glowSlot: Int? = null,
    topCenter: (@Composable androidx.compose.foundation.layout.BoxScope.(Dp) -> Unit)? = null,
) {
    // Assuming Landscape orientation
    val baseDp = heightDp

    val deadZonePadding = baseDp / 18

    // Helper function to get config for a component
    fun getConfig(component: ButtonComponent) =
        buttonConfigs[component] ?: ButtonConfig.default(component)

    // Helper function to render a button component
    @Composable
    fun RenderComponent(
        component: ButtonComponent,
        content: @Composable (ButtonConfig, Modifier) -> Unit
    ) {
        val config = getConfig(component)
        if (config.visible || onComponentTap != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
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

    // First we make a box that will contain the gamepad
    // And put padding around it so that it doesn't touch the edges of the screen
    Surface(color = MaterialTheme.colorScheme.background) {
        // The glow sits behind every control
        SlotGlow(slot = glowSlot)
        Box(
            modifier = Modifier
                .padding(deadZonePadding.dp)
                .fillMaxSize()
        ) {
            topCenter?.invoke(this, (baseDp * 0.079f).dp)

            // Left Analog Stick
            RenderComponent(ButtonComponent.LEFT_ANALOG_STICK) { config, mod ->
                AnalogStick(
                    modifier = mod,
                    outerCircleWidth = (baseDp / 10 * config.scale).dp,
                    innerCircleRadius = (baseDp / 14 * config.scale).dp,
                    gamepadState = gamepadState,
                    type = AnalogStickType.LEFT,
                )
            }

            // Right Analog Stick
            RenderComponent(ButtonComponent.RIGHT_ANALOG_STICK) { config, mod ->
                AnalogStick(
                    modifier = mod,
                    outerCircleWidth = (baseDp / 10 * config.scale).dp,
                    innerCircleRadius = (baseDp / 14 * config.scale).dp,
                    gamepadState = gamepadState,
                    type = AnalogStickType.RIGHT,
                )
            }

            // D-Pad
            RenderComponent(ButtonComponent.DPAD) { config, mod ->
                Dpad(
                    modifier = mod,
                    size = (0.45 * baseDp * config.scale).dp,
                    gamepadState = gamepadState,
                )
            }

            // Face Buttons
            RenderComponent(ButtonComponent.FACE_BUTTONS) { config, mod ->
                FaceButtons(
                    modifier = mod,
                    size = (0.45 * baseDp * config.scale).dp,
                    gamepadState = gamepadState,
                )
            }

            // Left Trigger
            RenderComponent(ButtonComponent.LEFT_TRIGGER) { config, mod ->
                Trigger(
                    modifier = mod,
                    type = TriggerType.LEFT,
                    size = (baseDp / 6 * config.scale).dp,
                    gamepadState = gamepadState,
                    mode = config.triggerMode,
                )
            }

            // Right Trigger
            RenderComponent(ButtonComponent.RIGHT_TRIGGER) { config, mod ->
                Trigger(
                    modifier = mod,
                    type = TriggerType.RIGHT,
                    size = (baseDp / 6 * config.scale).dp,
                    gamepadState = gamepadState,
                    mode = config.triggerMode,
                )
            }

            // Central Buttons (LB, RB, Select, Start)
            CentralButtons(
                baseDp = baseDp,
                gamepadState = gamepadState,
                buttonConfigs = buttonConfigs,
                onComponentTap = onComponentTap,
                onComponentDrag = onComponentDrag,
                selectedComponent = selectedComponent,
                onBack = onBack,
            )
        }
    }
}
