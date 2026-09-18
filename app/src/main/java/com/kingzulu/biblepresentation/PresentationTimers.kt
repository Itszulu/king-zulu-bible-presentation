package com.kingzulu.biblepresentation

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Public countdown shown to the congregation. */
object CongregationTimer {
    var configuredSeconds by mutableLongStateOf(300L); private set
    var remainingSeconds by mutableLongStateOf(300L); private set
    var running by mutableStateOf(false); private set
    private var endElapsed = 0L
    private var prefs: SharedPreferences? = null

    fun initialize(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("king_zulu_timers", Context.MODE_PRIVATE)
        configuredSeconds = prefs?.getLong("public_configured", 300L) ?: 300L
        remainingSeconds = prefs?.getLong("public_remaining", configuredSeconds) ?: configuredSeconds
        running = prefs?.getBoolean("public_running", false) ?: false
        val savedWallEnd = prefs?.getLong("public_wall_end", 0L) ?: 0L
        if (running && savedWallEnd > 0L) {
            remainingSeconds = ((savedWallEnd - System.currentTimeMillis() + 999L) / 1000L).coerceAtLeast(0L)
            if (remainingSeconds == 0L) running = false else endElapsed = SystemClock.elapsedRealtime() + remainingSeconds * 1000L
        }
        persist()
    }

    fun setMinutes(minutes: Long) {
        if (running) return
        configuredSeconds = minutes.coerceIn(1L, 180L) * 60L
        remainingSeconds = configuredSeconds
        persist()
    }
    fun startOrResume() { if (!running && remainingSeconds > 0) { endElapsed = SystemClock.elapsedRealtime() + remainingSeconds * 1000L; running = true; persist() } }
    fun pause() { refresh(); running = false; persist() }
    fun reset() { running = false; remainingSeconds = configuredSeconds; endElapsed = 0L; persist() }
    fun refresh() {
        if (!running) return
        remainingSeconds = ((endElapsed - SystemClock.elapsedRealtime() + 999L) / 1000L).coerceAtLeast(0L)
        if (remainingSeconds == 0L) running = false
        persist()
    }
    fun display() = formatCountdown(remainingSeconds)
    fun slide() = PresentationSlide(text = "SERVICE BEGINS IN\n\n${display()}", kind = "countdown")
    private fun persist() {
        val wallEnd = if (running) System.currentTimeMillis() + remainingSeconds * 1000L else 0L
        prefs?.edit()?.putLong("public_configured", configuredSeconds)?.putLong("public_remaining", remainingSeconds)?.putBoolean("public_running", running)?.putLong("public_wall_end", wallEnd)?.apply()
    }
}

/** Private stage/foldback timer. Never belongs on the congregation bus. */
object FoldbackTimer {
    var configuredSeconds by mutableLongStateOf(1200L); private set
    var remainingSeconds by mutableLongStateOf(1200L); private set
    var running by mutableStateOf(false); private set
    var allowOvertime by mutableStateOf(true); private set
    private var endElapsed = 0L
    private var prefs: SharedPreferences? = null

    fun initialize(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("king_zulu_timers", Context.MODE_PRIVATE)
        configuredSeconds = prefs?.getLong("foldback_configured", 1200L) ?: 1200L
        remainingSeconds = prefs?.getLong("foldback_remaining", configuredSeconds) ?: configuredSeconds
        running = prefs?.getBoolean("foldback_running", false) ?: false
        allowOvertime = prefs?.getBoolean("foldback_overtime", true) ?: true
        val savedWallEnd = prefs?.getLong("foldback_wall_end", 0L) ?: 0L
        if (running && savedWallEnd > 0L) {
            val raw = (savedWallEnd - System.currentTimeMillis()) / 1000L
            remainingSeconds = if (allowOvertime) raw else raw.coerceAtLeast(0L)
            if (!allowOvertime && remainingSeconds == 0L) running = false
            endElapsed = SystemClock.elapsedRealtime() + remainingSeconds * 1000L
        }
        persist()
    }

    fun setMinutes(minutes: Long) { if (!running) { configuredSeconds = minutes.coerceIn(1L, 240L) * 60L; remainingSeconds = configuredSeconds; persist() } }
    fun setOvertime(enabled: Boolean) { allowOvertime = enabled; if (!enabled && remainingSeconds < 0) { remainingSeconds = 0; running = false }; persist() }
    fun startOrResume() { if (!running && (remainingSeconds > 0 || allowOvertime)) { endElapsed = SystemClock.elapsedRealtime() + remainingSeconds * 1000L; running = true; persist() } }
    fun pause() { refresh(); running = false; persist() }
    fun reset() { running = false; remainingSeconds = configuredSeconds; endElapsed = 0L; persist() }
    fun refresh() {
        if (!running) return
        val raw = (endElapsed - SystemClock.elapsedRealtime()) / 1000L
        remainingSeconds = if (allowOvertime) raw else raw.coerceAtLeast(0L)
        if (!allowOvertime && remainingSeconds == 0L) running = false
        persist()
    }
    fun display() = formatCountdown(remainingSeconds)
    private fun persist() {
        val wallEnd = if (running) System.currentTimeMillis() + remainingSeconds * 1000L else 0L
        prefs?.edit()?.putLong("foldback_configured", configuredSeconds)?.putLong("foldback_remaining", remainingSeconds)?.putBoolean("foldback_running", running)?.putBoolean("foldback_overtime", allowOvertime)?.putLong("foldback_wall_end", wallEnd)?.apply()
    }
}
