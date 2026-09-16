package com.kingzulu.biblepresentation

import android.os.CountDownTimer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** App-level countdown state. It is deliberately not owned by the Media composable,
 * so changing tabs or recreating that page does not stop/reset an active timer. */
object CountdownStore {
    var configuredMinutes by mutableLongStateOf(5L)
        private set
    var remainingSeconds by mutableLongStateOf(300L)
        private set
    var running by mutableStateOf(false)
        private set

    private var timer: CountDownTimer? = null

    fun setMinutes(minutes: Long) {
        if (running) return
        configuredMinutes = minutes.coerceIn(1L, 180L)
        remainingSeconds = configuredMinutes * 60L
    }

    fun startOrResume() {
        if (running || remainingSeconds <= 0L) return
        running = true
        timer?.cancel()
        timer = object : CountDownTimer(remainingSeconds * 1000L, 250L) {
            override fun onTick(millisUntilFinished: Long) {
                remainingSeconds = ((millisUntilFinished + 999L) / 1000L).coerceAtLeast(0L)
            }
            override fun onFinish() {
                remainingSeconds = 0L
                running = false
                timer = null
            }
        }.start()
    }

    fun pause() {
        if (!running) return
        timer?.cancel()
        timer = null
        running = false
    }

    fun reset() {
        timer?.cancel()
        timer = null
        running = false
        remainingSeconds = configuredMinutes * 60L
    }

    fun display(): String {
        val m = remainingSeconds / 60L
        val s = remainingSeconds % 60L
        return "%d:%02d".format(m, s)
    }

    fun slide() = PresentationSlide(
        text = "SERVICE BEGINS IN\n\n${display()}",
        kind = "countdown"
    )
}
