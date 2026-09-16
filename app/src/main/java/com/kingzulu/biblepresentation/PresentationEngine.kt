package com.kingzulu.biblepresentation

enum class OutputLayer { BACKGROUND, SLIDE, OVERLAY, AUDIO, TIMER }

data class OutputState(
    val background: BackgroundTheme? = null,
    val slide: PresentationSlide? = null,
    val overlayText: String? = null,
    val black: Boolean = false,
    val locked: Boolean = false
)

data class PreviewLiveState(
    val preview: PresentationSlide? = null,
    val live: OutputState = OutputState(),
    val history: List<OutputState> = emptyList()
) {
    fun preview(slide: PresentationSlide) = copy(preview = slide)

    private fun push(next: OutputState): PreviewLiveState {
        if (next == live) return this
        return copy(live = next, history = (history + live).takeLast(30))
    }

    fun goLive(): PreviewLiveState =
        if (live.locked || preview == null) this
        else push(live.copy(slide = preview, black = false))

    fun present(slide: PresentationSlide?): PreviewLiveState =
        if (live.locked) this else push(live.copy(slide = slide, black = false))

    fun clearSlide() = if (live.locked) this else push(live.copy(slide = null))
    fun black() = if (live.locked) this else push(live.copy(black = true))
    fun restore() = if (live.locked) this else push(live.copy(black = false))
    fun logo() = present(PresentationSlide(text = "KING ZULU", kind = "logo"))

    /** One-tap recovery after an accidental live action. */
    fun previous(): PreviewLiveState {
        if (live.locked || history.isEmpty()) return this
        return copy(live = history.last(), history = history.dropLast(1))
    }

    fun toggleLock() = copy(live = live.copy(locked = !live.locked))
}
