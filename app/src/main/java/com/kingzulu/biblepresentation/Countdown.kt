package com.kingzulu.biblepresentation

enum class TimerMode { DURATION, TO_CLOCK_TIME, TO_EVENT }

data class PresentationTimer(
    val name: String,
    val mode: TimerMode,
    val durationSeconds: Long = 0,
    val targetEpochMillis: Long? = null,
    val overflow: Boolean = false
) {
    fun remainingSeconds(nowMillis: Long = System.currentTimeMillis()): Long {
        val raw = when(mode) {
            TimerMode.DURATION -> durationSeconds
            TimerMode.TO_CLOCK_TIME, TimerMode.TO_EVENT ->
                ((targetEpochMillis ?: nowMillis) - nowMillis) / 1000
        }
        return if (overflow) raw else raw.coerceAtLeast(0)
    }
}
fun formatCountdown(totalSeconds: Long): String {
    val negative = totalSeconds < 0
    val safe = kotlin.math.abs(totalSeconds)
    val h = safe / 3600
    val m = (safe % 3600) / 60
    val s = safe % 60
    val value = if (h > 0) "%02d:%02d:%02d".format(h,m,s) else "%02d:%02d".format(m,s)
    return if (negative) "+$value" else value
}
