package io.github.kitswas.virtualgamepadmobile.data

/** Everything about how the controller looks and behaves that can be tuned in Settings. */
data class AppPrefs(
    // appearance
    val opacity: Int = 100,
    val glowEnabled: Boolean = true,
    val glowStrength: Int = 100,
    // display
    val keepScreenOn: Boolean = true,
    // sticks
    val deadZone: Int = 5,
    val sensitivity: Int = 100,
    val clickEnabled: Boolean = true,
    val clickHoldMs: Int = 450,
    val invertLeftY: Boolean = false,
    val invertRightY: Boolean = false,
    // d-pad: the share of the radius a direction needs before it counts, so lower makes diagonals easier
    val dpadDiagonal: Int = 38,
    // connection
    val autoReconnect: Boolean = true,
    val reconnectSeconds: Int = 120,
    val confirmDisconnect: Boolean = true,
)
