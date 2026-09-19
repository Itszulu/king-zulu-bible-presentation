package com.kingzulu.biblepresentation

import android.content.Context

/**
 * App-level owner for AI Listen.
 *
 * Navigation screens must observe this session rather than own/destroy the
 * SpeechRecognizer themselves. This keeps an explicitly started listening
 * session alive while the operator moves between Bible, Presentation, Songs,
 * Media and other King Zulu screens.
 */
object AiListenSession {
    @Volatile private var controller: AiListenController? = null
    @Volatile var listening: Boolean = false
        private set
    @Volatile var transcript: String = ""
        private set
    @Volatile var status: String = "Ready to listen"
        private set
    @Volatile var lastCandidates: List<String> = emptyList()
        private set

    private val listeners = linkedSetOf<() -> Unit>()

    @Synchronized
    fun observe(listener: () -> Unit): () -> Unit {
        listeners += listener
        return { synchronized(this) { listeners -= listener } }
    }

    @Synchronized private fun changed() = listeners.toList().forEach { it() }

    @Synchronized
    fun ensure(
        context: Context,
        onFinalTranscript: (String) -> Unit,
        onFinalCandidates: (List<String>) -> Unit
    ): AiListenController {
        controller?.let { existing ->
            // Screen navigation may recreate the Compose callback. Keep the speech
            // engine alive, but replay its latest recognition into the new screen.
            if (lastCandidates.isNotEmpty()) onFinalCandidates(lastCandidates)
            return existing
        }
        val app = context.applicationContext
        return AiListenController(
            context = app,
            onListeningChanged = { value -> listening = value; changed() },
            onPartialTranscript = { value -> transcript = value; status = "Listening…"; changed() },
            onFinalTranscript = { value -> transcript = value; onFinalTranscript(value); changed() },
            onError = { value -> status = value; changed() },
            onFinalCandidates = { values -> lastCandidates = values; onFinalCandidates(values); changed() }
        ).also { controller = it }
    }

    fun start(controller: AiListenController) {
        controller.start()
        listening = true
        status = "Listening…"
        changed()
    }

    fun stop() {
        controller?.stop()
        listening = false
        status = "Ready to listen"
        changed()
    }

    /** Only the application lifecycle should call this, never screen navigation. */
    fun shutdown() {
        controller?.destroy()
        controller = null
        listening = false
        status = "Ready to listen"
        changed()
    }
}
