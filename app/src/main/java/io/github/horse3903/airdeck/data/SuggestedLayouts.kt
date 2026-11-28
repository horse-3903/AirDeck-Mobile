package io.github.horse3903.airdeck.data

import io.github.horse3903.airdeck.data.ButtonAnchor.*
import io.github.horse3903.airdeck.data.ButtonComponent.*

/** A ready-made layout for a kind of game, with the behaviour that suits it. Using one saves a copy under its name. */
class SuggestedLayout(
    val name: String,
    val description: String,
    val buttons: Map<ButtonComponent, ButtonConfig>,
    val behaviour: Map<String, Int>,
)

private fun cfg(
    component: ButtonComponent, visible: Boolean, scale: Float, offsetX: Float, offsetY: Float, anchor: ButtonAnchor,
    triggerMode: TriggerMode = TriggerMode.DIGITAL, stickMode: StickMode = StickMode.FIXED,
    areaW: Float = 0f, areaH: Float = 0f, areaRound: Float = 1f,
) = ButtonConfig(
    component = component, visible = visible, scale = scale, offsetX = offsetX, offsetY = offsetY, anchor = anchor,
    triggerMode = triggerMode, stickMode = stickMode, areaW = areaW, areaH = areaH, areaRound = areaRound
)

val suggestedLayouts = listOf(
    SuggestedLayout(
        "Racing",
        "Big gas and brake on the right, tilt the phone to steer. Good for Forza and Mario Kart style games.",
        mapOf(
            LEFT_ANALOG_STICK to cfg(LEFT_ANALOG_STICK, true, 1.1f, 0.1768f, 0.1353f, CENTER_LEFT),
            RIGHT_ANALOG_STICK to cfg(RIGHT_ANALOG_STICK, false, 0.96f, 0.5f, 0f, BOTTOM_CENTER),
            DPAD to cfg(DPAD, false, 0.752f, -0.5f, 0f, BOTTOM_CENTER),
            FACE_BUTTONS to cfg(FACE_BUTTONS, true, 0.7f, 0.4384f, 0.0801f, TOP_CENTER),
            LEFT_TRIGGER to cfg(LEFT_TRIGGER, true, 1.4f, -0.4391f, 0.1578f, CENTER_RIGHT),
            RIGHT_TRIGGER to cfg(RIGHT_TRIGGER, true, 1.6f, -0.0278f, 0.1353f, CENTER_RIGHT),
            LEFT_SHOULDER to cfg(LEFT_SHOULDER, true, 0.92f, 0.119f, 0.135f, TOP_LEFT),
            RIGHT_SHOULDER to cfg(RIGHT_SHOULDER, true, 0.92f, -0.144f, 0.135f, TOP_RIGHT),
            SELECT_BUTTON to cfg(SELECT_BUTTON, true, 0.9f, -0.1704f, -0.1127f, CENTER),
            START_BUTTON to cfg(START_BUTTON, true, 0.9f, 0.1679f, -0.1127f, CENTER),
            BACK_BUTTON to cfg(BACK_BUTTON, true, 0.632f, -0.155f, -0.0274f, TOP_CENTER),
            CONNECTION_BADGE to cfg(CONNECTION_BADGE, true, 0.632f, 0f, -0.0274f, TOP_CENTER),
            SETTINGS_BUTTON to cfg(SETTINGS_BUTTON, true, 0.632f, 0.155f, -0.0274f, TOP_CENTER)
        ),
        mapOf("tilt" to 1, "tiltRange" to 35, "tiltDead" to 2, "tiltInvert" to 0, "tcurve" to 3, "trigL" to 2, "trigR" to 2, "rcurve" to 3, "dead" to 8, "snap" to 0, "click" to 0, "rumble" to 1, "hapCustom" to 1, "hapInt" to 75, "hapMs" to 25, "hapPress" to 1, "hapTrig" to 1, "hapStick" to 0) + Curve(true, listOf(25 to 10, 50 to 38, 75 to 76)).toInts("t") + Curve(true, listOf(25 to 12, 60 to 50, 85 to 86)).toInts("r")
    ),
    SuggestedLayout(
        "Platformer",
        "A large D-pad and big jump buttons, no sticks. Good for Pico Park and other side-scrollers.",
        mapOf(
            LEFT_ANALOG_STICK to cfg(LEFT_ANALOG_STICK, false, 0.96f, 0.176f, 0.068f, CENTER_LEFT),
            RIGHT_ANALOG_STICK to cfg(RIGHT_ANALOG_STICK, false, 0.96f, 0.5f, 0f, BOTTOM_CENTER),
            DPAD to cfg(DPAD, true, 1.15f, 0.1817f, 0.1353f, CENTER_LEFT),
            FACE_BUTTONS to cfg(FACE_BUTTONS, true, 1.15f, -0.1843f, 0.1353f, CENTER_RIGHT),
            LEFT_TRIGGER to cfg(LEFT_TRIGGER, true, 0.8f, 0.1258f, 0.1091f, TOP_LEFT),
            RIGHT_TRIGGER to cfg(RIGHT_TRIGGER, true, 0.8f, -0.1283f, 0.1091f, TOP_RIGHT),
            LEFT_SHOULDER to cfg(LEFT_SHOULDER, true, 0.9f, 0.4142f, 0.1362f, TOP_LEFT),
            RIGHT_SHOULDER to cfg(RIGHT_SHOULDER, true, 0.9f, -0.4167f, 0.1362f, TOP_RIGHT),
            SELECT_BUTTON to cfg(SELECT_BUTTON, true, 0.9f, -0.1478f, 0.0676f, CENTER),
            START_BUTTON to cfg(START_BUTTON, true, 0.9f, 0.1453f, 0.0676f, CENTER),
            BACK_BUTTON to cfg(BACK_BUTTON, true, 0.632f, -0.155f, -0.0274f, TOP_CENTER),
            CONNECTION_BADGE to cfg(CONNECTION_BADGE, true, 0.632f, 0f, -0.0274f, TOP_CENTER),
            SETTINGS_BUTTON to cfg(SETTINGS_BUTTON, true, 0.632f, 0.155f, -0.0274f, TOP_CENTER)
        ),
        mapOf("dpadSingle" to 1, "click" to 0, "trigL" to 0, "trigR" to 0, "rumble" to 1, "hapCustom" to 1, "hapInt" to 85, "hapMs" to 14, "hapPress" to 1, "hapRel" to 1)
    ),
    SuggestedLayout(
        "Shooter",
        "Floating left stick, a fine right stick for aiming and big triggers for fire.",
        mapOf(
            LEFT_ANALOG_STICK to cfg(LEFT_ANALOG_STICK, true, 1.0f, 0.1939f, -0.1263f, BOTTOM_LEFT, areaW = 3.2f, areaH = 2.6f, areaRound = 0.6f),
            RIGHT_ANALOG_STICK to cfg(RIGHT_ANALOG_STICK, true, 1.0f, 0.4384f, 0.2255f, CENTER, areaW = 2.4f, areaH = 2.4f, areaRound = 0.8f),
            DPAD to cfg(DPAD, true, 0.5f, -0.3056f, 0.2378f, TOP_CENTER),
            FACE_BUTTONS to cfg(FACE_BUTTONS, true, 0.65f, -0.0938f, 0.0451f, CENTER_RIGHT),
            LEFT_TRIGGER to cfg(LEFT_TRIGGER, true, 1.25f, 0.0883f, 0.0623f, TOP_LEFT),
            RIGHT_TRIGGER to cfg(RIGHT_TRIGGER, true, 1.25f, -0.0908f, 0.0623f, TOP_RIGHT),
            LEFT_SHOULDER to cfg(LEFT_SHOULDER, true, 0.92f, 0.4347f, 0.1124f, TOP_LEFT),
            RIGHT_SHOULDER to cfg(RIGHT_SHOULDER, true, 0.92f, -0.4597f, 0.1124f, TOP_RIGHT),
            SELECT_BUTTON to cfg(SELECT_BUTTON, true, 0.9f, -0.471f, 0.464f, TOP_CENTER),
            START_BUTTON to cfg(START_BUTTON, true, 0.9f, 0.471f, 0.464f, TOP_CENTER),
            BACK_BUTTON to cfg(BACK_BUTTON, true, 0.632f, -0.155f, -0.0274f, TOP_CENTER),
            CONNECTION_BADGE to cfg(CONNECTION_BADGE, true, 0.632f, 0f, -0.0274f, TOP_CENTER),
            SETTINGS_BUTTON to cfg(SETTINGS_BUTTON, true, 0.632f, 0.155f, -0.0274f, TOP_CENTER)
        ),
        mapOf("stickL" to 1, "stickR" to 0, "snap" to 1, "dead" to 6, "sens" to 110, "curve" to 3, "click" to 1, "clickMs" to 350, "invR" to 0, "trigL" to 1, "trigR" to 1, "rcurve" to 2, "rumble" to 1, "hapCustom" to 1, "hapInt" to 70, "hapMs" to 18, "hapPress" to 1, "hapTrig" to 1, "hapStick" to 0) + Curve(true, listOf(30 to 10, 60 to 38, 85 to 80)).toInts("c")
    ),
    SuggestedLayout(
        "Fighting",
        "D-pad on the left and six attack buttons in reach of the right thumb. Good for Street Fighter and Smash.",
        mapOf(
            LEFT_ANALOG_STICK to cfg(LEFT_ANALOG_STICK, false, 0.96f, 0.176f, 0.068f, CENTER_LEFT),
            RIGHT_ANALOG_STICK to cfg(RIGHT_ANALOG_STICK, false, 0.96f, 0.5f, 0f, BOTTOM_CENTER),
            DPAD to cfg(DPAD, true, 1.1f, 0.1705f, 0.1578f, CENTER_LEFT),
            FACE_BUTTONS to cfg(FACE_BUTTONS, true, 1.0f, 0.3257f, 0.1353f, CENTER),
            LEFT_TRIGGER to cfg(LEFT_TRIGGER, false, 1.128f, 0.2516f, -0.0222f, TOP_LEFT),
            RIGHT_TRIGGER to cfg(RIGHT_TRIGGER, true, 1.0f, -0.2018f, 0.1804f, CENTER_RIGHT),
            LEFT_SHOULDER to cfg(LEFT_SHOULDER, true, 0.9f, 0.01f, 0.1362f, TOP_CENTER),
            RIGHT_SHOULDER to cfg(RIGHT_SHOULDER, true, 1.0f, -0.2033f, -0.1804f, CENTER_RIGHT),
            SELECT_BUTTON to cfg(SELECT_BUTTON, true, 0.9f, -0.1478f, -0.1578f, CENTER),
            START_BUTTON to cfg(START_BUTTON, true, 0.9f, 0.1453f, -0.1578f, CENTER),
            BACK_BUTTON to cfg(BACK_BUTTON, true, 0.632f, -0.155f, -0.0274f, TOP_CENTER),
            CONNECTION_BADGE to cfg(CONNECTION_BADGE, true, 0.632f, 0f, -0.0274f, TOP_CENTER),
            SETTINGS_BUTTON to cfg(SETTINGS_BUTTON, true, 0.632f, 0.155f, -0.0274f, TOP_CENTER)
        ),
        mapOf("dpadSingle" to 0, "diag" to 30, "click" to 0, "trigL" to 0, "trigR" to 0, "rumble" to 1, "hapCustom" to 1, "hapInt" to 90, "hapMs" to 12, "hapPress" to 1, "hapRel" to 1)
    ),
    SuggestedLayout(
        "Party games",
        "Left stick, jump and big grab triggers. Good for Gang Beasts, Smash and other couch games.",
        mapOf(
            LEFT_ANALOG_STICK to cfg(LEFT_ANALOG_STICK, true, 1.1f, 0.1768f, 0.1578f, CENTER_LEFT, areaW = 2.2f, areaH = 2.2f, areaRound = 1.0f),
            RIGHT_ANALOG_STICK to cfg(RIGHT_ANALOG_STICK, false, 0.96f, 0.5f, 0f, BOTTOM_CENTER),
            DPAD to cfg(DPAD, false, 0.752f, -0.5f, 0f, BOTTOM_CENTER),
            FACE_BUTTONS to cfg(FACE_BUTTONS, true, 1.0f, -0.1729f, 0.1353f, CENTER_RIGHT),
            LEFT_TRIGGER to cfg(LEFT_TRIGGER, true, 1.2f, 0.1376f, 0.0675f, TOP_LEFT),
            RIGHT_TRIGGER to cfg(RIGHT_TRIGGER, true, 1.2f, -0.1401f, 0.0675f, TOP_RIGHT),
            LEFT_SHOULDER to cfg(LEFT_SHOULDER, true, 0.9f, 0.4142f, 0.1137f, TOP_LEFT),
            RIGHT_SHOULDER to cfg(RIGHT_SHOULDER, true, 0.9f, -0.4167f, 0.1137f, TOP_RIGHT),
            SELECT_BUTTON to cfg(SELECT_BUTTON, true, 0.9f, -0.471f, 0.464f, TOP_CENTER),
            START_BUTTON to cfg(START_BUTTON, true, 0.9f, 0.471f, 0.464f, TOP_CENTER),
            BACK_BUTTON to cfg(BACK_BUTTON, true, 0.632f, -0.155f, -0.0274f, TOP_CENTER),
            CONNECTION_BADGE to cfg(CONNECTION_BADGE, true, 0.632f, 0f, -0.0274f, TOP_CENTER),
            SETTINGS_BUTTON to cfg(SETTINGS_BUTTON, true, 0.632f, 0.155f, -0.0274f, TOP_CENTER)
        ),
        mapOf("click" to 0, "dead" to 10, "sens" to 100, "curve" to 0, "snap" to 1, "trigL" to 0, "trigR" to 0, "rumble" to 1, "hapCustom" to 1, "hapInt" to 60, "hapMs" to 20, "hapPress" to 1, "hapRel" to 0, "hapConn" to 1)
    )
)
