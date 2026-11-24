package io.github.kitswas.virtualgamepadmobile.data

val defaultColorScheme = ColorScheme.SYSTEM
val defaultBaseColor = BaseColor.BLUE
val defaultGamepadStyle = GamepadStyle.PLAYSTATION
const val defaultPollingDelay = 80 // in milliseconds
const val defaultHapticFeedbackEnabled = true // vibrations
const val defaultSaveConnectionCredentials = false
const val defaultFullScreenEnabled = true
const val defaultStickSnap = true
const val defaultDpadSingle = false

// Default button configurations with offsets matching the original layout
// Note: Offset values are multipliers of baseDp (heightDp in landscape)
// These will be applied as: offsetX * baseDp or offsetY * baseDp at runtime
val defaultButtonConfigs = mapOf(
    // Positions follow the browser controller (clusters centred at 16, 30, 70 and 84 percent of the width),
    // so the two versions look alike. Offsets are in units of the layout height.
    ButtonComponent.LEFT_ANALOG_STICK to ButtonConfig(
        component = ButtonComponent.LEFT_ANALOG_STICK,
        scale = 0.96f,
        offsetX = 0.176f,
        offsetY = 0.068f,
        anchor = ButtonAnchor.CENTER_LEFT
    ),
    ButtonComponent.RIGHT_ANALOG_STICK to ButtonConfig(
        component = ButtonComponent.RIGHT_ANALOG_STICK,
        scale = 0.96f,
        offsetX = 0.5f,
        anchor = ButtonAnchor.BOTTOM_CENTER
    ),
    ButtonComponent.DPAD to ButtonConfig(
        component = ButtonComponent.DPAD,
        scale = 0.752f,
        offsetX = -0.5f,
        anchor = ButtonAnchor.BOTTOM_CENTER
    ),
    ButtonComponent.FACE_BUTTONS to ButtonConfig(
        component = ButtonComponent.FACE_BUTTONS,
        scale = 0.803f,
        offsetX = -0.165f,
        offsetY = 0.396f,
        anchor = ButtonAnchor.TOP_RIGHT
    ),
    ButtonComponent.LEFT_TRIGGER to ButtonConfig(
        component = ButtonComponent.LEFT_TRIGGER,
        scale = 1.128f,
        offsetX = 0.2516f,
        offsetY = -0.0222f,
        anchor = ButtonAnchor.TOP_LEFT
    ),
    ButtonComponent.RIGHT_TRIGGER to ButtonConfig(
        component = ButtonComponent.RIGHT_TRIGGER,
        scale = 1.128f,
        offsetX = -0.2516f,
        offsetY = -0.0222f,
        anchor = ButtonAnchor.TOP_RIGHT
    ),
    ButtonComponent.LEFT_SHOULDER to ButtonConfig(
        component = ButtonComponent.LEFT_SHOULDER,
        scale = 0.92f,
        offsetX = 0.2516f,
        offsetY = 0.2495f,
        anchor = ButtonAnchor.TOP_LEFT
    ),
    ButtonComponent.RIGHT_SHOULDER to ButtonConfig(
        component = ButtonComponent.RIGHT_SHOULDER,
        scale = 0.92f,
        offsetX = -0.2516f,
        offsetY = 0.2495f,
        anchor = ButtonAnchor.TOP_RIGHT
    ),
    ButtonComponent.SELECT_BUTTON to ButtonConfig(
        component = ButtonComponent.SELECT_BUTTON,
        scale = 0.9f,
        offsetX = -0.471f,
        offsetY = 0.464f,
        anchor = ButtonAnchor.TOP_CENTER
    ),
    ButtonComponent.START_BUTTON to ButtonConfig(
        component = ButtonComponent.START_BUTTON,
        scale = 0.9f,
        offsetX = 0.471f,
        offsetY = 0.464f,
        anchor = ButtonAnchor.TOP_CENTER
    ),
    ButtonComponent.BACK_BUTTON to ButtonConfig(
        component = ButtonComponent.BACK_BUTTON,
        scale = 0.632f,
        offsetX = -0.14f,
        offsetY = -0.0274f,
        anchor = ButtonAnchor.TOP_CENTER
    )
)
