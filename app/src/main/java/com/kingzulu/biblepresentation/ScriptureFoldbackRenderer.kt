package com.kingzulu.biblepresentation

/**
 * Builds the private preacher/foldback Scripture view from the active reading session.
 * Audience output remains independent and receives only the current live verse.
 */
object ScriptureFoldbackRenderer {
    data class RenderModel(
        val currentReference: String,
        val currentText: String,
        val nextReference: String? = null,
        val nextText: String? = null,
        val currentWeight: Float = 1f,
        val nextWeight: Float = 0f,
        val endOfReading: Boolean = false,
        val endLabel: String? = null,
        val mode: ScriptureFoldbackMode = ScriptureFoldbackMode.CURRENT_NEXT,
        val teleprompter: List<Pair<String, String>> = emptyList()
    )

    fun render(): RenderModel? {
        val state = ScriptureReadingSession.currentState() ?: return null
        val current = state.current ?: return null
        val currentRef = current.reference.display()

        if (state.mode == ScriptureFoldbackMode.TELEPROMPTER) {
            val remaining = state.remaining
                .filter {
                    val n = it.reference.verseStart ?: 0
                    !state.insideDeclaredRange || n <= state.declaredEndVerse
                }
                .map { it.reference.display() to it.text }
            return RenderModel(
                currentReference = currentRef,
                currentText = current.text,
                mode = ScriptureFoldbackMode.TELEPROMPTER,
                teleprompter = remaining,
                endOfReading = state.atDeclaredEnd,
                endLabel = if (state.atDeclaredEnd) endLabel(state) else null
            )
        }

        // A declared reading must stop visually at its endpoint. Do not preview the next chapter
        // or a verse outside the declared range; give CURRENT the full foldback instead.
        if (state.atDeclaredEnd) {
            return RenderModel(
                currentReference = currentRef,
                currentText = current.text,
                currentWeight = 1f,
                nextWeight = 0f,
                endOfReading = true,
                endLabel = endLabel(state)
            )
        }

        val next = state.next?.takeIf {
            val n = it.reference.verseStart ?: 0
            !state.insideDeclaredRange || n <= state.declaredEndVerse
        }

        if (next == null) {
            return RenderModel(
                currentReference = currentRef,
                currentText = current.text,
                currentWeight = 1f,
                nextWeight = 0f
            )
        }

        // CURRENT remains dominant. Long verses get more room; NEXT is a clearly separated preview.
        val currentWords = current.text.trim().split(Regex("\\s+")).count { it.isNotBlank() }
        val currentWeight = when {
            currentWords >= 45 -> .82f
            currentWords >= 30 -> .78f
            else -> .74f
        }
        return RenderModel(
            currentReference = currentRef,
            currentText = current.text,
            nextReference = next.reference.display(),
            nextText = next.text,
            currentWeight = currentWeight,
            nextWeight = 1f - currentWeight
        )
    }

    /** Bridge for existing stage-display consumers. */
    fun stageState(base: StageDisplayState = StageDisplayState()): StageDisplayState {
        val model = render() ?: return base.copy(currentText = "", nextText = "")
        val current = buildString {
            append("CURRENT · ").append(model.currentReference).append('\n')
            append(model.currentText)
            model.endLabel?.let { append("\n\n").append(it) }
        }
        val next = if (model.nextText != null && model.nextReference != null) {
            "NEXT · ${model.nextReference}\n${model.nextText}"
        } else ""
        return base.copy(currentText = current, nextText = next)
    }

    private fun endLabel(state: ScriptureReadingState): String =
        "END OF READING · ${state.book} ${state.chapter}:${state.declaredStartVerse}-${state.declaredEndVerse}"
}
