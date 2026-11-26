package io.github.horse3903.airdeck.data

import kotlin.math.pow

const val CURVE_LINEAR = 0
const val CURVE_PRECISE = 1
const val CURVE_QUICK = 2
const val CURVE_CUSTOM = 3

/** Three heights (0 to 100) at a quarter, half and three quarters of the way, packed into one number. */
fun packCurve(a: Int, b: Int, c: Int): Int = a.coerceIn(0, 100) or (b.coerceIn(0, 100) shl 7) or (c.coerceIn(0, 100) shl 14)

fun unpackCurve(p: Int): IntArray = intArrayOf(p and 127, (p shr 7) and 127, (p shr 14) and 127)

val DEFAULT_CURVE_POINTS = packCurve(25, 50, 75)

/** Shapes a 0 to 1 amount: [kind] is one of the CURVE constants, [points] only matters for custom. */
fun applyCurve(kind: Int, points: Int, v: Float): Float = when (kind) {
    CURVE_PRECISE -> v.pow(1.6f)
    CURVE_QUICK -> v.pow(0.7f)
    CURVE_CUSTOM -> {
        val p = unpackCurve(points)
        val ys = floatArrayOf(0f, p[0] / 100f, p[1] / 100f, p[2] / 100f, 1f)
        val x = v.coerceIn(0f, 1f) * 4f
        val i = x.toInt().coerceAtMost(3)
        ys[i] + (ys[i + 1] - ys[i]) * (x - i)
    }
    else -> v
}
