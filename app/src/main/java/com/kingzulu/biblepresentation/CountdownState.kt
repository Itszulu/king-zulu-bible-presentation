package com.kingzulu.biblepresentation

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object CountdownState {
    var minutes by mutableIntStateOf(5)
    var remaining by mutableLongStateOf(300L)
    var running by mutableStateOf(false)
    var endElapsed by mutableLongStateOf(0L)

    fun startOrResume() {
        endElapsed = SystemClock.elapsedRealtime() + remaining * 1000L
        running = true
    }

    fun pause() {
        refresh()
        running = false
    }

    fun reset() {
        running = false
        remaining = minutes * 60L
        endElapsed = 0L
    }

    fun adjustMinutes(delta: Int) {
        if (running) return
        minutes = (minutes + delta).coerceIn(1, 180)
        remaining = minutes * 60L
    }

    fun refresh() {
        if (!running) return
        remaining = ((endElapsed - SystemClock.elapsedRealtime() + 999L) / 1000L).coerceAtLeast(0L)
        if (remaining <= 0L) {
            remaining = 0L
            running = false
        }
    }

    fun slide() = PresentationSlide(
        text = "SERVICE BEGINS IN\n\n${formatCountdown(remaining)}",
        kind = "countdown"
    )
}
