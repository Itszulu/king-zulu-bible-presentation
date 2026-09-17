package com.kingzulu.biblepresentation

/** Widgets that can be independently enabled on the private stage/foldback output. */
enum class FoldbackWidgetType { CLOCK, TIMER, CURRENT_ITEM, NEXT_ITEM, OPERATOR_MESSAGE, ALERT }
enum class WidgetAnchor { TOP_LEFT, TOP_CENTER, TOP_RIGHT, CENTER_LEFT, CENTER_RIGHT, BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT }
enum class AlertDirection { LEFT_TO_RIGHT, RIGHT_TO_LEFT }

data class FoldbackClockConfig(
    val enabled: Boolean = true,
    val use24Hour: Boolean = false,
    val showSeconds: Boolean = false,
    val showDate: Boolean = false,
    val label: String = "",
    val anchor: WidgetAnchor = WidgetAnchor.TOP_RIGHT,
    /** Percentage of output width reserved for the widget, keeping it compact rather than full-screen. */
    val widthPercent: Int = 22,
    val marginPercent: Int = 3
)

data class SlidingAlertConfig(
    val enabled: Boolean = false,
    val message: String = "",
    val cycleCount: Int = 1,
    /** Approximate ticker travel speed; renderer maps this to pixels/second for each output size. */
    val speedPercent: Int = 50,
    val direction: AlertDirection = AlertDirection.RIGHT_TO_LEFT,
    val anchor: WidgetAnchor = WidgetAnchor.BOTTOM_CENTER,
    val fontSizeSp: Int = 30,
    val fontColorArgb: Long = 0xFFFFFFFF,
    val backgroundColorArgb: Long = 0xCC000000,
    val backgroundOpacityPercent: Int = 80,
    val paddingDp: Int = 12
)

data class FoldbackLayoutConfig(
    val clock: FoldbackClockConfig = FoldbackClockConfig(),
    val alert: SlidingAlertConfig = SlidingAlertConfig(),
    val enabledWidgets: Set<FoldbackWidgetType> = setOf(FoldbackWidgetType.CLOCK)
)

object FoldbackWidgetState {
    @Volatile var layout: FoldbackLayoutConfig = FoldbackLayoutConfig()
        private set

    fun configureClock(config: FoldbackClockConfig) {
        layout = layout.copy(
            clock = config,
            enabledWidgets = if (config.enabled) layout.enabledWidgets + FoldbackWidgetType.CLOCK else layout.enabledWidgets - FoldbackWidgetType.CLOCK
        )
    }

    fun configureAlert(config: SlidingAlertConfig) {
        layout = layout.copy(
            alert = config,
            enabledWidgets = if (config.enabled) layout.enabledWidgets + FoldbackWidgetType.ALERT else layout.enabledWidgets - FoldbackWidgetType.ALERT
        )
    }
}
