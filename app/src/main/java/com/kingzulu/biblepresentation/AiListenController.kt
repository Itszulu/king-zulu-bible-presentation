package com.kingzulu.biblepresentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class AiListenController(
    private val context: Context,
    private val onListeningChanged: (Boolean) -> Unit,
    private val onTranscript: (String) -> Unit,
    private val onError: (String) -> Unit
) : RecognitionListener {
    private var recognizer: SpeechRecognizer? = null
    private var keepListening = false

    private val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
    }

    fun start() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition is not available on this device")
            return
        }
        keepListening = true
        if (recognizer == null) recognizer = SpeechRecognizer.createSpeechRecognizer(context).also { it.setRecognitionListener(this) }
        recognizer?.startListening(intent)
        onListeningChanged(true)
    }

    fun stop() {
        keepListening = false
        recognizer?.stopListening()
        onListeningChanged(false)
    }

    fun destroy() {
        keepListening = false
        recognizer?.destroy()
        recognizer = null
    }

    private fun restart() {
        if (!keepListening) return
        recognizer?.cancel()
        recognizer?.startListening(intent)
        onListeningChanged(true)
    }

    override fun onReadyForSpeech(params: Bundle?) { onListeningChanged(true) }
    override fun onBeginningOfSpeech() = Unit
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() { onListeningChanged(false) }

    override fun onError(error: Int) {
        if (keepListening) restart() else onListeningChanged(false)
    }

    override fun onResults(results: Bundle?) {
        val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
        if (!text.isNullOrBlank()) onTranscript(text)
        restart()
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
        if (!text.isNullOrBlank()) onTranscript(text)
    }

    override fun onEvent(eventType: Int, params: Bundle?) = Unit
}
