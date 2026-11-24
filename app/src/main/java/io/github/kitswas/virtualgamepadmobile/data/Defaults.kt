package io.github.kitswas.virtualgamepadmobile.data

val defaultColorScheme = ColorScheme.SYSTEM
val defaultBaseColor = BaseColor.BLUE
val defaultGamepadStyle = GamepadStyle.PLAYSTATION
const val defaultPollingDelay = 80 // in milliseconds
const val defaultHapticFeedbackEnabled = true // vibrations
const val defaultSaveConnectionCredentials = false
const val defaultFullScreenEnabled = true
const val defaultStickSnap = true
const val defaultDpadOrthogonal = true

// Default button configurations with offsets matching the original layout
// Note: Offset values are multipliers of baseDp (heightDp in landscape)
// These will be applied as: offsetX * baseDp or offsetY * baseDp at runtime
val defaultButtonConfigs = mapOf(
    // Left stick upper left, D-pad lower centre-left, right stick lower centre-right, face buttons upper right
    ButtonComponent.LEFT_ANALOG_STICK to ButtonConfig(
        component = ButtonComponent.LEFT_ANALOG_STICK,
        offsetX = 0.04f,
        offsetY = 0.1f,
        anchor = ButtonAnchor.CENTER_LEFT
    ),
    ButtonComponent.RIGHT_ANALOG_STICK to ButtonConfig(
        component = ButtonComponent.RIGHT_ANALOG_STICK,
        offsetX = -0.5f,
        anchor = ButtonAnchor.BOTTOM_RIGHT
    ),
    ButtonComponent.DPAD to ButtonConfig(
        component = ButtonComponent.DPAD,
        offsetX = 0.5f,
        anchor = ButtonAnchor.BOTTOM_LEFT
    ),
    ButtonComponent.FACE_BUTTONS to ButtonConfig(
        component = ButtonComponent.FACE_BUTTONS,
        offsetY = 0.38f,
        anchor = ButtonAnchor.TOP_RIGHT
    ),
    // Triggers in the top corners with the bumpers just under them
    ButtonComponent.LEFT_TRIGGER to ButtonConfig(
        component = ButtonComponent.LEFT_TRIGGER,
        offsetX = 0.14f,
        anchor = ButtonAnchor.TOP_LEFT
    ),
    ButtonComponent.RIGHT_TRIGGER to ButtonConfig(
        component = ButtonComponent.RIGHT_TRIGGER,
        offsetX = -0.14f,
        anchor = ButtonAnchor.TOP_RIGHT
    ),
    ButtonComponent.LEFT_SHOULDER to ButtonConfig(
        component = ButtonComponent.LEFT_SHOULDER,
        offsetX = 0.13f,
        offsetY = 0.25f,
        anchor = ButtonAnchor.TOP_LEFT
    ),
    ButtonComponent.RIGHT_SHOULDER to ButtonConfig(
        component = ButtonComponent.RIGHT_SHOULDER,
        offsetX = -0.13f,
        offsetY = 0.25f,
        anchor = ButtonAnchor.TOP_RIGHT
    ),
    // Small system buttons grouped in the middle
    ButtonComponent.SELECT_BUTTON to ButtonConfig(
        component = ButtonComponent.SELECT_BUTTON,
        offsetX = -0.42f,
        offsetY = 0.32f,
        anchor = ButtonAnchor.TOP_CENTER
    ),
    ButtonComponent.START_BUTTON to ButtonConfig(
        component = ButtonComponent.START_BUTTON,
        offsetX = 0.42f,
        offsetY = 0.32f,
        anchor = ButtonAnchor.TOP_CENTER
    ),
    // Back sits aside so the connection status can own the top centre
    ButtonComponent.BACK_BUTTON to ButtonConfig(
        component = ButtonComponent.BACK_BUTTON,
        offsetX = -0.3f,
        anchor = ButtonAnchor.TOP_CENTER
    )
)
