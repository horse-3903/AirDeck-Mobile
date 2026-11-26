package io.github.horse3903.airdeck.ui.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
import io.github.horse3903.airdeck.data.defaultHapticFeedbackEnabled
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Everything about how the controller feels. Custom vibration lets the strength and length be chosen; turning it
 * off falls back to the phone's own touch feedback effects.
 */
data class HapticPrefs(
    val custom: Boolean = true,
    val intensity: Int = 70,
    val durationMs: Int = 20,
    val press: Boolean = true,
    val release: Boolean = true,
    val sticks: Boolean = false,
    val triggers: Boolean = true,
)

/**
 * Utility class for handling haptic feedback consistently across the app.
 */
object HapticUtils {
    private const val TAG = "HapticUtils"

    // Override flags to ensure haptic feedback is always triggered if enabled
    private const val OVERRIDE_FLAGS = HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING or
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING

    // Static property to control haptic feedback globally
    var isEnabled: Boolean = defaultHapticFeedbackEnabled
        set(value) {
            field = value
            Log.d(TAG, "Haptic feedback enabled: $value")
        }

    @Volatile
    var prefs: HapticPrefs = HapticPrefs()

    private fun vibrator(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    /** One short pulse at the chosen strength and length, scaled for the kind of event. */
    private fun pulse(view: View, durationScale: Float, strengthScale: Float) {
        val vibrator = vibrator(view.context) ?: return
        if (!vibrator.hasVibrator()) return
        val millis = (prefs.durationMs * durationScale).roundToLong().coerceAtLeast(3L)
        val amplitude = (255 * (prefs.intensity / 100f) * strengthScale).roundToInt().coerceIn(1, 255)
        val effect = if (vibrator.hasAmplitudeControl()) {
            VibrationEffect.createOneShot(millis, amplitude)
        } else {
            VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        vibrator.vibrate(effect)
    }

    /**
     * Follows the game's rumble. A phone has one motor, so the two strengths become the stronger of them.
     * Each update replaces the last and the buzz repeats until the game sends zero or the phone disconnects.
     */
    @Suppress("DEPRECATION")
    fun gameRumble(context: Context, large: Int, weak: Int) {
        val vibrator = vibrator(context) ?: return
        if (!vibrator.hasVibrator()) return
        val strength = maxOf(large, weak).coerceIn(0, 255)
        if (strength == 0) {
            rumbleStrength = 0
            vibrator.cancel()
            return
        }
        // A game that repeats the same command every frame should not restart the motor each time, but a touch
        // buzz can replace the repeating one, so it is started again after a second
        val now = android.os.SystemClock.elapsedRealtime()
        if (strength == rumbleStrength && now - rumbleStartedAt < 800L) return
        rumbleStrength = strength
        rumbleStartedAt = now
        // A game rumbles until it says stop, so this repeats until stopRumble or a zero arrives
        val effect = if (vibrator.hasAmplitudeControl()) {
            VibrationEffect.createWaveform(longArrayOf(1000L), intArrayOf(strength.coerceAtLeast(1)), 0)
        } else {
            VibrationEffect.createWaveform(longArrayOf(1000L), 0)
        }
        vibrator.vibrate(
            effect,
            android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_GAME).build()
        )
    }

    private var rumbleStrength = 0
    private var rumbleStartedAt = 0L

    /** A short buzz when the link to the PC comes up (shorter) or drops (longer). */
    fun connectionPulse(context: Context, connected: Boolean) {
        if (!isEnabled) return
        val vibrator = vibrator(context) ?: return
        if (!vibrator.hasVibrator()) return
        vibrator.vibrate(VibrationEffect.createOneShot(if (connected) 80L else 160L, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun stopRumble(context: Context) {
        rumbleStrength = 0
        vibrator(context)?.cancel()
    }

    private fun system(view: View, effect: Int, fallback: Int = effect) {
        view.performHapticFeedback(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) effect else fallback, OVERRIDE_FLAGS)
    }

    /**
     * Provides haptic feedback for gamepad button press events (buttons down).
     */
    fun performButtonPressFeedback(view: View) {
        if (!isEnabled || !prefs.press) return
        try {
            if (prefs.custom) pulse(view, 1f, 1f)
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS, OVERRIDE_FLAGS)
            else view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, OVERRIDE_FLAGS)
        } catch (e: Exception) {
            Log.e(TAG, "Error performing button press haptic feedback: ${e.message}")
        }
    }

    /**
     * Provides haptic feedback for gamepad button release events (buttons up).
     */
    fun performButtonReleaseFeedback(view: View) {
        if (!isEnabled || !prefs.release) return
        try {
            if (prefs.custom) pulse(view, 0.6f, 0.6f)
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_RELEASE, OVERRIDE_FLAGS)
            else view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, OVERRIDE_FLAGS)
        } catch (e: Exception) {
            Log.e(TAG, "Error performing button release haptic feedback: ${e.message}")
        }
    }

    /**
     * Provides haptic feedback when an analog stick gesture starts.
     */
    fun performGestureStartFeedback(view: View) {
        if (!isEnabled || !prefs.sticks) return
        try {
            if (prefs.custom) pulse(view, 1f, 0.8f)
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) view.performHapticFeedback(HapticFeedbackConstants.GESTURE_START, OVERRIDE_FLAGS)
            else view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK, OVERRIDE_FLAGS)
        } catch (e: Exception) {
            Log.e(TAG, "Error performing gesture start haptic feedback: ${e.message}")
        }
    }

    /**
     * Provides haptic feedback when analog stick is released and returns to center.
     */
    fun performGestureEndFeedback(view: View) {
        if (!isEnabled || !prefs.sticks) return
        try {
            if (prefs.custom) pulse(view, 0.6f, 0.5f)
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) view.performHapticFeedback(HapticFeedbackConstants.GESTURE_END, OVERRIDE_FLAGS)
            else view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, OVERRIDE_FLAGS)
        } catch (e: Exception) {
            Log.e(TAG, "Error performing gesture end haptic feedback: ${e.message}")
        }
    }

    /**
     * Provides subtle haptic feedback when the stick reaches its edge.
     *
     * @param normalizedDistance Distance from center (0-1 range)
     */
    fun performAnalogMovementFeedback(view: View, normalizedDistance: Float) {
        if (!isEnabled || !prefs.sticks) return
        if (normalizedDistance < 0.95f) return
        try {
            if (prefs.custom) pulse(view, 0.5f, 0.7f)
            else view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK, OVERRIDE_FLAGS)
        } catch (e: Exception) {
            Log.e(TAG, "Error performing analog movement haptic feedback: ${e.message}")
        }
    }

    /** A light tick as an analog trigger passes each quarter of its travel. */
    fun performTriggerTick(view: View) {
        if (!isEnabled || !prefs.triggers) return
        try {
            if (prefs.custom) pulse(view, 0.5f, 0.7f)
            else view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK, OVERRIDE_FLAGS)
        } catch (e: Exception) {
            Log.e(TAG, "Error performing trigger haptic feedback: ${e.message}")
        }
    }
}
