package com.kingzulu.biblepresentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class AiListenController(
    private val context: Context,
    private val onListeningChanged: (Boolean) -> Unit,
    private val onPartialTranscript: (String) -> Unit,
    private val onFinalTranscript: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onFinalCandidates: ((List<String>) -> Unit)? = null
) : RecognitionListener {
    private var recognizer: SpeechRecognizer? = null
    private var keepListening = false
    private val main = Handler(Looper.getMainLooper())
    private var lastFinal = ""
    private var lastFinalAt = 0L
    private val watchdog = Runnable { if (keepListening) restart(0L) }
    private val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-NG")
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-NG")
        putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 700L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 900L)
    }

    fun start() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) { onError("Speech recognition is not available on this device"); return }
        keepListening = true
        if (recognizer == null) recognizer = SpeechRecognizer.createSpeechRecognizer(context).also { it.setRecognitionListener(this) }
        runCatching { recognizer?.startListening(intent) }.onFailure { onError("Couldn't start AI Listen") }
        onListeningChanged(true)
    }
    fun stop() { keepListening = false; main.removeCallbacksAndMessages(null); recognizer?.stopListening(); onListeningChanged(false) }
    fun destroy() { keepListening = false; main.removeCallbacksAndMessages(null); recognizer?.destroy(); recognizer = null }
    private fun armWatchdog() { main.removeCallbacks(watchdog); if (keepListening) main.postDelayed(watchdog, 12_000L) }
    private fun restart(delay: Long = 260L) {
        if (!keepListening) return
        main.removeCallbacks(watchdog)
        main.postDelayed({ if (keepListening) {
            runCatching { recognizer?.cancel() }
            runCatching { recognizer?.startListening(intent) }.onFailure {
                runCatching { recognizer?.destroy() }
                recognizer = SpeechRecognizer.createSpeechRecognizer(context).also { it.setRecognitionListener(this) }
                runCatching { recognizer?.startListening(intent) }
            }
            armWatchdog()
            onListeningChanged(true)
        } }, delay)
    }
    override fun onReadyForSpeech(params: Bundle?) { armWatchdog(); onListeningChanged(true) }
    override fun onBeginningOfSpeech() { armWatchdog() }
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() { armWatchdog() }
    override fun onError(error: Int) {
        if (!keepListening) { onListeningChanged(false); return }
        // NO_MATCH and SPEECH_TIMEOUT are normal boundaries in a sermon; silently cycle.
        restart(if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) 650L else 300L)
    }
    override fun onResults(results: Bundle?) {
        main.removeCallbacks(watchdog)
        val candidates = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.map { it.trim().replace(Regex("\\s+"), " ") }
            ?.filter { it.length >= 2 }
            ?.distinct()
            .orEmpty()
        if (candidates.isNotEmpty()) {
            val now = System.currentTimeMillis()
            val primary = candidates.first()
            // Samsung/Google recognizers can emit the same final twice while cycling.
            if (!(primary.equals(lastFinal, true) && now - lastFinalAt < 1800)) {
                lastFinal = primary; lastFinalAt = now
                onFinalCandidates?.invoke(candidates)
                onFinalTranscript(primary)
            }
        }
        restart()
    }
    override fun onPartialResults(partialResults: Bundle?) {
        val candidates = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
        val text = candidates.maxByOrNull { it.length }?.trim()?.replace(Regex("\\s+"), " ")
        // Do not replace the UI with isolated recognizer noise such as one-character fragments.
        if (!text.isNullOrBlank() && (text.contains(' ') || text.length >= 4)) onPartialTranscript(text)
    }
    override fun onEvent(eventType: Int, params: Bundle?) = Unit
}
