package io.github.horse3903.airdeck.data

/** Everything about how the controller looks and behaves that can be tuned in Settings. */
data class AppPrefs(
    // appearance
    val opacity: Int = 100,
    val glowEnabled: Boolean = true,
    val glowStrength: Int = 100,
    // show the ring a stick moves inside, and scale every control together
    val stickRing: Boolean = true,
    val controlSize: Int = 100,
    // display: seconds of no touch before the screen dims, 0 is never
    val keepScreenOn: Boolean = true,
    val dimAfterSeconds: Int = 0,
    // sticks
    val deadZone: Int = 5,
    val sensitivity: Int = 100,
    // 0 linear, 1 precise (finer near the centre), 2 quick
    val stickCurve: Int = 0,
    val curvePoints: Int = DEFAULT_CURVE_POINTS,
    val tiltCurve: Int = 0,
    val tiltCurvePoints: Int = DEFAULT_CURVE_POINTS,
    val clickEnabled: Boolean = true,
    val clickHoldMs: Int = 450,
    val invertLeftY: Boolean = false,
    val invertRightY: Boolean = false,
    // tilt steering: 0 off, 1 left stick, 2 right stick, and how much tilt it takes
    val tiltTarget: Int = 0,
    val tiltRange: Int = 30,
    val tiltDead: Int = 3,
    val tiltInvert: Boolean = false,
    // trigger input per trigger (position in TriggerMode.offered) and whether each stick floats
    val triggerLeft: Int = 0,
    val triggerRight: Int = 0,
    val stickLeftFloat: Boolean = false,
    val stickRightFloat: Boolean = false,
    // d-pad: the share of the radius a direction needs before it counts, so lower makes diagonals easier
    val dpadDiagonal: Int = 38,
    // vibrate when the game rumbles this controller
    val gameRumble: Boolean = true,
    // a short buzz when the link comes up or drops
    val connectionHaptics: Boolean = true,
    // connection
    val autoReconnect: Boolean = true,
    val reconnectSeconds: Int = 120,
    val confirmDisconnect: Boolean = true,
)

/**
 * The settings a layout can override while it is in use. The values are whole numbers: on is 1, off is 0,
 * and a choice is its position in its list (curve: linear, precise, quick. tilt: off, left, right).
 * Keys: snap, dead, sens, curve, click, clickMs, invL, invR, curveP, tcurve, tcurveP, tilt, tiltRange, tiltDead, tiltInvert, dpadSingle, diag,
 * trigL, trigR, stickL, stickR, rumble, and the haptics keys hap, hapCustom, hapInt, hapMs, hapPress, hapRel, hapStick, hapTrig.
 */
fun AppPrefs.withBehaviour(b: Map<String, Int>): AppPrefs = if (b.isEmpty()) this else copy(
    deadZone = b["dead"] ?: deadZone,
    sensitivity = b["sens"] ?: sensitivity,
    stickCurve = b["curve"] ?: stickCurve,
    curvePoints = b["curveP"] ?: curvePoints,
    tiltCurve = b["tcurve"] ?: tiltCurve,
    tiltCurvePoints = b["tcurveP"] ?: tiltCurvePoints,
    clickEnabled = b["click"]?.let { it != 0 } ?: clickEnabled,
    clickHoldMs = b["clickMs"] ?: clickHoldMs,
    invertLeftY = b["invL"]?.let { it != 0 } ?: invertLeftY,
    invertRightY = b["invR"]?.let { it != 0 } ?: invertRightY,
    tiltTarget = b["tilt"] ?: tiltTarget,
    tiltRange = b["tiltRange"] ?: tiltRange,
    tiltDead = b["tiltDead"] ?: tiltDead,
    tiltInvert = b["tiltInvert"]?.let { it != 0 } ?: tiltInvert,
    dpadDiagonal = b["diag"] ?: dpadDiagonal,
    triggerLeft = b["trigL"] ?: triggerLeft,
    triggerRight = b["trigR"] ?: triggerRight,
    stickLeftFloat = b["stickL"]?.let { it != 0 } ?: stickLeftFloat,
    stickRightFloat = b["stickR"]?.let { it != 0 } ?: stickRightFloat,
    connectionHaptics = b["hapConn"]?.let { it != 0 } ?: connectionHaptics,
    gameRumble = b["rumble"]?.let { it != 0 } ?: gameRumble,
)

fun io.github.horse3903.airdeck.ui.utils.HapticPrefs.withBehaviour(b: Map<String, Int>) = if (b.isEmpty()) this else copy(
    custom = b["hapCustom"]?.let { it != 0 } ?: custom,
    intensity = b["hapInt"] ?: intensity,
    durationMs = b["hapMs"] ?: durationMs,
    press = b["hapPress"]?.let { it != 0 } ?: press,
    release = b["hapRel"]?.let { it != 0 } ?: release,
    sticks = b["hapStick"]?.let { it != 0 } ?: sticks,
    triggers = b["hapTrig"]?.let { it != 0 } ?: triggers,
)
