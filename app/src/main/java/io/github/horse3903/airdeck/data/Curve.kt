package io.github.horse3903.airdeck.data

import androidx.compose.runtime.Immutable
import kotlin.math.pow
import kotlin.math.sqrt

const val CURVE_LINEAR = 0
const val CURVE_PRECISE = 1
const val CURVE_QUICK = 2
const val CURVE_CUSTOM = 3

const val MAX_CURVE_POINTS = 10

/**
 * A response curve drawn by the player. The ends (0, 0) and (100, 100) are fixed; [points] are the ones in between,
 * as (how far the stick moves, what the game receives), both 0 to 100, sorted by the first number.
 */
@Immutable
data class Curve(val smooth: Boolean = true, val points: List<Pair<Int, Int>> = emptyList(), val free: Boolean = false) {
    fun encode(): String = (if (smooth) "s" else "l") + (if (free) "f" else "") + "|" + points.joinToString(",") { "${it.first}:${it.second}" }

    fun eval(v: Float): Float {
        val xs = ArrayList<Float>(points.size + 2).apply { add(0f); points.forEach { add(it.first.toFloat()) }; add(100f) }
        val ys = ArrayList<Float>(points.size + 2).apply { add(0f); points.forEach { add(it.second.toFloat()) }; add(100f) }
        val x = v.coerceIn(0f, 1f) * 100f
        var i = 0
        while (i < xs.size - 2 && x > xs[i + 1]) i++
        val h = xs[i + 1] - xs[i]
        if (h <= 0f) return (ys[i] / 100f).coerceIn(0f, 1f)
        if (!smooth) return ((ys[i] + (ys[i + 1] - ys[i]) * (x - xs[i]) / h) / 100f).coerceIn(0f, 1f)
        val n = xs.size - 1
        val d = FloatArray(n) { (ys[it + 1] - ys[it]) / (xs[it + 1] - xs[it]).coerceAtLeast(0.0001f) }
        val m = FloatArray(n + 1)
        m[0] = d[0]
        m[n] = d[n - 1]
        for (k in 1 until n) m[k] = if (d[k - 1] * d[k] <= 0f) 0f else (d[k - 1] + d[k]) / 2f
        for (k in 0 until n) {
            if (d[k] == 0f) { m[k] = 0f; m[k + 1] = 0f; continue }
            val a = m[k] / d[k]
            val b = m[k + 1] / d[k]
            val s = a * a + b * b
            if (s > 9f) {
                val t = 3f / sqrt(s)
                m[k] = t * a * d[k]
                m[k + 1] = t * b * d[k]
            }
        }
        val t = (x - xs[i]) / h
        val t2 = t * t
        val t3 = t2 * t
        val y = (2 * t3 - 3 * t2 + 1) * ys[i] + (t3 - 2 * t2 + t) * h * m[i] + (-2 * t3 + 3 * t2) * ys[i + 1] + (t3 - t2) * h * m[i + 1]
        return (y / 100f).coerceIn(0f, 1f)
    }

    /** Flattens the curve into whole numbers so a layout can override it: a count, then two points to a number. */
    fun toInts(prefix: String): Map<String, Int> {
        val out = linkedMapOf(prefix + "M" to (points.size or ((if (smooth) 1 else 0) shl 4) or ((if (free) 1 else 0) shl 5)))
        for (i in 0 until (MAX_CURVE_POINTS + 1) / 2) {
            val a = points.getOrNull(i * 2)
            val b = points.getOrNull(i * 2 + 1)
            out[prefix + i] = (a?.first ?: 0) or ((a?.second ?: 0) shl 7) or ((b?.first ?: 0) shl 14) or ((b?.second ?: 0) shl 21)
        }
        return out
    }

    /** How many times steeper than a straight line the curve is at [v] (0 to 1). Negative where output falls. */
    fun sensitivity(v: Float): Float {
        val h = 0.004f
        val a = (v - h).coerceAtLeast(0f)
        val b = (v + h).coerceAtMost(1f)
        return (eval(b) - eval(a)) / (b - a)
    }

    companion object {
        val Linear = Curve()

        fun decode(text: String?): Curve {
            val m = Regex("^([sl])(f?)\\|(.*)$").find(text ?: "") ?: return Linear
            val pts = m.groupValues[3].split(",").mapNotNull {
                val p = it.split(":")
                val x = p.getOrNull(0)?.toIntOrNull()
                val y = p.getOrNull(1)?.toIntOrNull()
                if (x != null && y != null && x in 1..99 && y in 0..100) x to y else null
            }
            return clean(m.groupValues[1] == "s", pts, m.groupValues[2] == "f")
        }

        /** Sorts the points, drops doubles and, unless [free], makes sure the output never falls as the input rises. */
        fun clean(smooth: Boolean, pts: List<Pair<Int, Int>>, free: Boolean = false): Curve {
            val out = ArrayList<Pair<Int, Int>>()
            for (p in pts.sortedBy { it.first }) if (out.isEmpty() || p.first > out.last().first) out.add(p.first.coerceIn(1, 99) to p.second.coerceIn(0, 100))
            if (!free) { var top = 0; for (i in out.indices) { top = maxOf(top, out[i].second); out[i] = out[i].first to top } }
            return Curve(smooth, out.take(MAX_CURVE_POINTS), free)
        }

        fun fromInts(prefix: String, b: Map<String, Int>): Curve? {
            val head = b[prefix + "M"] ?: return null
            val n = (head and 15).coerceAtMost(MAX_CURVE_POINTS)
            val pts = ArrayList<Pair<Int, Int>>()
            for (k in 0 until n) {
                val packed = b[prefix + (k / 2)] ?: 0
                val shift = if (k % 2 == 0) 0 else 14
                pts.add(((packed shr shift) and 127) to ((packed shr (shift + 7)) and 127))
            }
            return clean(head and 16 != 0, pts, head and 32 != 0)
        }

        fun keys(prefix: String): List<String> = listOf(prefix + "M") + (0 until (MAX_CURVE_POINTS + 1) / 2).map { prefix + it }

        fun sampled(f: (Float) -> Float): Curve = Curve(true, (1..9).map { it * 10 to (f(it / 10f) * 100f).toInt().coerceIn(0, 100) })
        fun fromKind(kind: Int): Curve = when (kind) { CURVE_PRECISE -> precise(); CURVE_QUICK -> quick(); else -> Linear }
        fun precise() = sampled { it.pow(1.6f) }
        fun quick() = sampled { it.pow(0.7f) }
    }
}

/** Shapes a 0 to 1 amount: [kind] is one of the CURVE constants, [custom] only matters for custom. */
fun applyCurve(kind: Int, custom: Curve, v: Float): Float = when (kind) {
    CURVE_PRECISE -> v.pow(1.6f)
    CURVE_QUICK -> v.pow(0.7f)
    CURVE_CUSTOM -> custom.eval(v)
    else -> v
}
