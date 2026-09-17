package com.kingzulu.biblepresentation

/** Widgets that can be independently enabled on the private stage/foldback output. */
enum class FoldbackWidgetType { CLOCK, TIMER, CURRENT_ITEM, NEXT_ITEM, OPERATOR_MESSAGE }

data class FoldbackClockConfig(
    val enabled: Boolean = true,
    val use24Hour: Boolean = false,
    val showSeconds: Boolean = false,
    val showDate: Boolean = false,
    val label: String = ""
)

data class FoldbackLayoutConfig(
    val clock: FoldbackClockConfig = FoldbackClockConfig(),
    val enabledWidgets: Set<FoldbackWidgetType> = setOf(FoldbackWidgetType.CLOCK)
)

/** Persistent in-memory route state; UI/output renderer will bind this to operator preferences. */
object FoldbackWidgetState {
    @Volatile var layout: FoldbackLayoutConfig = FoldbackLayoutConfig()
        private set

    fun configureClock(config: FoldbackClockConfig) {
        layout = layout.copy(
            clock = config,
            enabledWidgets = if (config.enabled) layout.enabledWidgets + FoldbackWidgetType.CLOCK
            else layout.enabledWidgets - FoldbackWidgetType.CLOCK
        )
    }
}
