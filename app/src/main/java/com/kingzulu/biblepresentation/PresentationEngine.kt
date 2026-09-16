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
    val live: OutputState = OutputState()
) {
    fun preview(slide: PresentationSlide) = copy(preview = slide)
    fun goLive(): PreviewLiveState =
        if (live.locked || preview == null) this
        else copy(live = live.copy(slide = preview, black = false))
    fun clearSlide() = if (live.locked) this else copy(live = live.copy(slide = null))
    fun black() = if (live.locked) this else copy(live = live.copy(black = true))
    fun restore() = copy(live = live.copy(black = false))
    fun toggleLock() = copy(live = live.copy(locked = !live.locked))
}
