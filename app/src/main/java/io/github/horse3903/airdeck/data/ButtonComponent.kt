package io.github.horse3903.airdeck.data

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import io.github.horse3903.airdeck.R
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

// Centralized value ranges for button configuration
val SCALE_VALUE_RANGE = 0.5f..2f
val STRETCH_VALUE_RANGE = 0.5f..2f
val OFFSET_VALUE_RANGE = -0.5f..0.5f

/**
 * Anchor point for button positioning
 */
@Immutable
@Serializable
@Parcelize
enum class ButtonAnchor(@StringRes val nameRes: Int, val displayName: String) : Parcelable {
    TOP_LEFT(R.string.anchor_top_left, "Top Left"),
    TOP_CENTER(R.string.anchor_top_center, "Top Center"),
    TOP_RIGHT(R.string.anchor_top_right, "Top Right"),
    CENTER_LEFT(R.string.anchor_center_left, "Center Left"),
    CENTER(R.string.anchor_center, "Center"),
    CENTER_RIGHT(R.string.anchor_center_right, "Center Right"),
    BOTTOM_LEFT(R.string.anchor_bottom_left, "Bottom Left"),
    BOTTOM_CENTER(R.string.anchor_bottom_center, "Bottom Center"),
    BOTTOM_RIGHT(R.string.anchor_bottom_right, "Bottom Right");

    override fun toString(): String = displayName
}

/**
 * Represents all customizable button components in the gamepad
 */
@Immutable
@Serializable
@Parcelize
enum class ButtonComponent(
    @StringRes val nameRes: Int,
    val displayName: String,
    val defaultAnchor: ButtonAnchor
) : Parcelable {
    LEFT_ANALOG_STICK(R.string.component_left_analog, "Left Analog Stick", ButtonAnchor.TOP_LEFT),
    RIGHT_ANALOG_STICK(
        R.string.component_right_analog,
        "Right Analog Stick",
        ButtonAnchor.BOTTOM_RIGHT
    ),
    DPAD(R.string.component_dpad, "D-Pad", ButtonAnchor.BOTTOM_LEFT),
    FACE_BUTTONS(R.string.component_face_buttons, "Face Buttons (A/B/X/Y)", ButtonAnchor.TOP_RIGHT),
    LEFT_TRIGGER(R.string.component_left_trigger, "Left Trigger (LT)", ButtonAnchor.BOTTOM_LEFT),
    RIGHT_TRIGGER(
        R.string.component_right_trigger,
        "Right Trigger (RT)",
        ButtonAnchor.BOTTOM_RIGHT
    ),
    LEFT_SHOULDER(R.string.component_left_shoulder, "Left Shoulder (LB)", ButtonAnchor.TOP_CENTER),
    RIGHT_SHOULDER(
        R.string.component_right_shoulder,
        "Right Shoulder (RB)",
        ButtonAnchor.TOP_CENTER
    ),
    SELECT_BUTTON(R.string.component_select, "Select (View)", ButtonAnchor.TOP_CENTER),
    START_BUTTON(R.string.component_start, "Start (Menu)", ButtonAnchor.TOP_CENTER),
    BACK_BUTTON(R.string.component_back, "Back", ButtonAnchor.TOP_CENTER),
    CONNECTION_BADGE(R.string.component_connection, "Connection", ButtonAnchor.TOP_CENTER),
    SETTINGS_BUTTON(R.string.component_settings_button, "Settings", ButtonAnchor.TOP_CENTER);

    override fun toString(): String = displayName
}

/**
 * How a trigger turns touch input into a 0..1 analog value
 */
@Immutable
@Serializable
@Parcelize
enum class TriggerMode(@StringRes val nameRes: Int, @StringRes val descRes: Int) : Parcelable {
    DIGITAL(R.string.trigger_mode_digital, R.string.trigger_mode_digital_desc),
    SLIDE(R.string.trigger_mode_slide, R.string.trigger_mode_slide_desc),
    DRAG(R.string.trigger_mode_drag, R.string.trigger_mode_drag_desc),
    ZONES(R.string.trigger_mode_zones, R.string.trigger_mode_zones_desc),
    RAMP(R.string.trigger_mode_ramp, R.string.trigger_mode_ramp_desc),
    FORCE(R.string.trigger_mode_force, R.string.trigger_mode_force_desc),
    TILT(R.string.trigger_mode_tilt, R.string.trigger_mode_tilt_desc);

    companion object {
        // Slide, ramp, force and tilt are implemented but hidden from the picker
        val offered = listOf(DIGITAL, DRAG, ZONES)
    }
}

/**
 * Configuration for a single button component
 */
@Stable
@Serializable
@Parcelize
data class ButtonConfig(
    val component: ButtonComponent,
    val visible: Boolean = true,
    val scale: Float = 1.0f,
    /** Stretches the control on top of [scale]: 1 is its natural shape, below 1 narrower or shorter. */
    val widthScale: Float = 1.0f,
    val heightScale: Float = 1.0f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val anchor: ButtonAnchor = component.defaultAnchor,
    val triggerMode: TriggerMode = TriggerMode.DIGITAL
) : Parcelable {
    companion object {
        fun default(component: ButtonComponent) = defaultButtonConfigs[component] ?: ButtonConfig(component)
    }
}
