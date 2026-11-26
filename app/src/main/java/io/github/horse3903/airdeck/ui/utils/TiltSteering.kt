package io.github.horse3903.airdeck.ui.utils

import android.view.Surface
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * The phone held like a steering wheel: how far the screen has turned from the calibrated position becomes -1 to 1.
 * The accelerometer reading is turned into screen coordinates first, so it works with the screen either way round.
 */
object TiltSteering {
    @Volatile
    var steer: Float = 0f
        private set

    @Volatile var rangeDeg = 30f
    @Volatile var deadDeg = 3f
    @Volatile var invert = false

    private var sx = 0f
    private var sy = 0f
    private var angle = 0f
    private var base: Float? = null

    /** Forgets the neutral position, so the next reading becomes it. */
    fun reset() {
        base = null
        sx = 0f
        sy = 0f
        steer = 0f
    }

    /** Makes the way the phone is held right now the straight position. */
    fun calibrate() {
        base = angle
        steer = 0f
    }

    fun onAccelerometer(ax: Float, ay: Float, rotation: Int) {
        val x: Float
        val y: Float
        when (rotation) {
            Surface.ROTATION_90 -> { x = -ay; y = ax }
            Surface.ROTATION_180 -> { x = -ax; y = -ay }
            Surface.ROTATION_270 -> { x = ay; y = -ax }
            else -> { x = ax; y = ay }
        }
        sx += 0.35f * (x - sx)
        sy += 0.35f * (y - sy)
        if (hypot(sx, sy) < 1f) return
        angle = Math.toDegrees(atan2(-sx, sy).toDouble()).toFloat()
        val neutral = base ?: angle.also { base = it }
        var d = angle - neutral
        if (d > 180f) d -= 360f
        if (d < -180f) d += 360f
        val dead = deadDeg
        val range = maxOf(rangeDeg, dead + 1f)
        val magnitude = abs(d)
        var out = if (magnitude <= dead) 0f else minOf(1f, (magnitude - dead) / (range - dead)) * (if (d < 0f) -1f else 1f)
        if (invert) out = -out
        steer = out
    }
}
