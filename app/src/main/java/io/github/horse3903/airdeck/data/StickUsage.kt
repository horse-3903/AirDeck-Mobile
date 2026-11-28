package io.github.horse3903.airdeck.data

/** How far something is pushed (after the dead zone, before the curve), sampled at 10 Hz into 50 bins. */
class Usage {
    val bins = IntArray(BINS)

    @Volatile var samples = 0
        private set
    private var last = 0L

    fun record(amount: Float) {
        val now = System.currentTimeMillis()
        if (now - last < 100) return
        last = now
        bins[(amount * BINS).toInt().coerceIn(0, BINS - 1)]++
        samples++
    }

    fun clear() {
        for (i in bins.indices) bins[i] = 0
        samples = 0
    }

    companion object { const val BINS = 50 }
}

/** Where the sticks and the triggers spend their time, kept in memory until cleared. */
object StickUsage {
    val stick = Usage()
    val trigger = Usage()
}
