package com.kingzulu.biblepresentation

/** Presentation content is independent of how each destination renders it. */
enum class OutputKind { BROWSER, GOOGLE_CAST, EXTERNAL_DISPLAY, NDI }
enum class TemplateKind { FULL_SCREEN, LOWER_THIRD, TRANSPARENT_TEXT, STAGE_DISPLAY }

data class OutputProfile(
    val id: String,
    val name: String,
    val output: OutputKind,
    val scriptureTemplate: TemplateKind = TemplateKind.FULL_SCREEN,
    val songTemplate: TemplateKind = TemplateKind.FULL_SCREEN,
    val timerTemplate: TemplateKind = TemplateKind.FULL_SCREEN,
    val enabled: Boolean = true
)

data class RoutedPresentation(
    val slide: PresentationSlide?,
    val black: Boolean = false,
    val clear: Boolean = false
)

/**
 * Single live content state -> many independently styled outputs.
 * Transport implementations subscribe here instead of Bible/Songs knowing about TVs/NDI.
 */
class OutputRouter {
    private val profiles = linkedMapOf<String, OutputProfile>()

    init {
        profiles["browser"] = OutputProfile("browser", "Browser Display", OutputKind.BROWSER)
        profiles["cast"] = OutputProfile("cast", "Google Cast", OutputKind.GOOGLE_CAST)
        // Future professional video output defaults to transparent lower thirds.
        profiles["ndi"] = OutputProfile("ndi", "NDI", OutputKind.NDI, TemplateKind.LOWER_THIRD, TemplateKind.LOWER_THIRD, TemplateKind.LOWER_THIRD, false)
    }

    fun all(): List<OutputProfile> = profiles.values.toList()
    fun profile(id: String): OutputProfile? = profiles[id]
    fun save(profile: OutputProfile) { profiles[profile.id] = profile }

    fun templateFor(profile: OutputProfile, slide: PresentationSlide?): TemplateKind = when (slide?.kind?.lowercase()) {
        "song", "lyrics" -> profile.songTemplate
        "timer" -> profile.timerTemplate
        else -> profile.scriptureTemplate
    }
}
