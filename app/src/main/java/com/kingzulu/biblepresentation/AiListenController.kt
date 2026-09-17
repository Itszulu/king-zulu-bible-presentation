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
    private val main=Handler(Looper.getMainLooper())
    private val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        // Nigerian English first. The Bible-aware matcher handles KJV vocabulary and phrase context.
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-NG")
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-NG")
        putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 8)
    }
    fun start(){
        if(!SpeechRecognizer.isRecognitionAvailable(context)){onError("Speech recognition is not available on this device");return}
        keepListening=true
        if(recognizer==null)recognizer=SpeechRecognizer.createSpeechRecognizer(context).also{it.setRecognitionListener(this)}
        recognizer?.startListening(intent);onListeningChanged(true)
    }
    fun stop(){keepListening=false;main.removeCallbacksAndMessages(null);recognizer?.stopListening();onListeningChanged(false)}
    fun destroy(){keepListening=false;main.removeCallbacksAndMessages(null);recognizer?.destroy();recognizer=null}
    private fun restart(){
        if(!keepListening)return
        // Avoid cancel/start races on some Samsung/Google recognizers.
        main.postDelayed({if(keepListening){runCatching{recognizer?.cancel()};runCatching{recognizer?.startListening(intent)};onListeningChanged(true)}},180)
    }
    override fun onReadyForSpeech(params:Bundle?){onListeningChanged(true)}
    override fun onBeginningOfSpeech()=Unit
    override fun onRmsChanged(rmsdB:Float)=Unit
    override fun onBufferReceived(buffer:ByteArray?)=Unit
    override fun onEndOfSpeech()=Unit // keep UI in listening mode while the recognizer cycles
    override fun onError(error:Int){if(keepListening)restart()else onListeningChanged(false)}
    override fun onResults(results:Bundle?){
        val candidates=results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.map{it.trim()}?.filter{it.isNotBlank()}?.distinct().orEmpty()
        if(candidates.isNotEmpty()){
            onFinalCandidates?.invoke(candidates)
            onFinalTranscript(candidates.first()) // backwards-compatible transcript display
        }
        restart()
    }
    override fun onPartialResults(partialResults:Bundle?){
        val text=partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
        if(!text.isNullOrBlank())onPartialTranscript(text)
    }
    override fun onEvent(eventType:Int,params:Bundle?)=Unit
}
