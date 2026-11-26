package io.github.horse3903.airdeck.ui.composables

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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import io.github.kitswas.VGP_Data_Exchange.GamepadReading
import io.github.horse3903.airdeck.data.ButtonAnchor
import io.github.horse3903.airdeck.data.ButtonComponent
import io.github.horse3903.airdeck.data.ButtonConfig

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

/** What the connection badge and the settings button show and do on the play screen. */
class TopControls(
    val status: LinkStatus,
    val slot: Int?,
    val onBadgeClick: () -> Unit,
    val onGearClick: () -> Unit,
)

@Composable
fun DrawGamepad(
    widthDp: Int,
    heightDp: Int,
    gamepadState: GamepadReading,
    buttonConfigs: Map<ButtonComponent, ButtonConfig>,
    editing: Boolean = false,
    onComponentBounds: ((ButtonComponent, androidx.compose.ui.geometry.Rect) -> Unit)? = null,
    selectedComponent: ButtonComponent? = null,
    onBack: (() -> Unit)? = null,
    showBack: Boolean = true,
    glowSlot: Int? = null,
    topControls: TopControls? = null,
) {
    // Assuming Landscape orientation
    val baseDp = heightDp

    val deadZonePadding = baseDp / 18

    // Helper function to get config for a component
    val sizeFactor = LocalAppPrefs.current.controlSize / 100f
    val tiltTarget = LocalAppPrefs.current.tiltTarget
    fun getConfig(component: ButtonComponent) =
        (buttonConfigs[component] ?: ButtonConfig.default(component)).let { if (sizeFactor == 1f) it else it.copy(scale = it.scale * sizeFactor) }.let {
            // The stick that tilt steering takes over is hidden while playing
            val tilted = !editing && ((tiltTarget == 1 && component == ButtonComponent.LEFT_ANALOG_STICK) || (tiltTarget == 2 && component == ButtonComponent.RIGHT_ANALOG_STICK))
            if (tilted) it.copy(visible = false) else it
        }

    // Helper function to render a button component
    @Composable
    fun RenderComponent(
        component: ButtonComponent,
        content: @Composable (ButtonConfig, Modifier) -> Unit
    ) {
        val config = getConfig(component)
        if (config.visible || editing) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = config.anchor.toAlignment()
            ) {
                EditableComponent(
                    component, config, baseDp, editing, onComponentBounds,
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
                .alpha(LocalAppPrefs.current.opacity / 100f)
        ) {
            topControls?.let { top ->
                RenderComponent(ButtonComponent.CONNECTION_BADGE) { config, mod ->
                    ConnectionBadge(
                        status = top.status,
                        slot = top.slot,
                        onClick = top.onBadgeClick,
                        height = (baseDp / 8 * config.scale).dp,
                        modifier = mod,
                    )
                }
                RenderComponent(ButtonComponent.SETTINGS_BUTTON) { config, mod ->
                    GearButton(
                        onClick = top.onGearClick,
                        height = (baseDp / 8 * config.scale).dp,
                        modifier = mod,
                    )
                }
            }

            // Left Analog Stick
            RenderComponent(ButtonComponent.LEFT_ANALOG_STICK) { config, mod ->
                AnalogStick(
                    modifier = mod,
                    outerCircleWidth = (baseDp / 10 * config.scale).dp,
                    innerCircleRadius = (baseDp / 14 * config.scale).dp,
                    gamepadState = gamepadState,
                    type = AnalogStickType.LEFT,
                    floating = LocalAppPrefs.current.stickLeftFloat,
                    areaW = config.areaW, areaH = config.areaH, areaRound = config.areaRound, showArea = editing,
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
                    floating = LocalAppPrefs.current.stickRightFloat,
                    areaW = config.areaW, areaH = config.areaH, areaRound = config.areaRound, showArea = editing,
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
                    mode = io.github.horse3903.airdeck.data.TriggerMode.offered[LocalAppPrefs.current.triggerLeft.coerceIn(0, 2)],
                )
            }

            // Right Trigger
            RenderComponent(ButtonComponent.RIGHT_TRIGGER) { config, mod ->
                Trigger(
                    modifier = mod,
                    type = TriggerType.RIGHT,
                    size = (baseDp / 6 * config.scale).dp,
                    gamepadState = gamepadState,
                    mode = io.github.horse3903.airdeck.data.TriggerMode.offered[LocalAppPrefs.current.triggerRight.coerceIn(0, 2)],
                )
            }

            // Central Buttons (LB, RB, Select, Start)
            CentralButtons(
                baseDp = baseDp,
                gamepadState = gamepadState,
                buttonConfigs = buttonConfigs,
                editing = editing,
                onComponentBounds = onComponentBounds,
                selectedComponent = selectedComponent,
                onBack = onBack,
                showBack = showBack,
            )
        }
    }
}
