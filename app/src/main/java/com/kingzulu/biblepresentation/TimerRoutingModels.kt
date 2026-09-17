package com.kingzulu.biblepresentation

/** Independent timer destinations. Audience and foldback timers can run simultaneously. */
enum class TimerRoute { AUDIENCE, FOLDBACK }

enum class TimerZeroAction { HOLD_AT_ZERO, CLEAR, SHOW_LOGO, PLAY_VIDEO, SHOW_MEDIA }

data class TimerZeroTrigger(
    val action: TimerZeroAction = TimerZeroAction.HOLD_AT_ZERO,
    /** Local imported media identifier/path chosen by the operator. */
    val mediaId: String? = null
)

data class RoutedTimerConfig(
    val route: TimerRoute,
    val durationSeconds: Long,
    val heading: String = "",
    val zeroTrigger: TimerZeroTrigger = TimerZeroTrigger()
)

/**
 * Keeps public service countdown and private foldback countdown conceptually separate.
 * Output transports (Browser/HDMI/NDI/etc.) subscribe to the route they are assigned.
 */
object TimerRoutingState {
    @Volatile var audience = RoutedTimerConfig(
        route = TimerRoute.AUDIENCE,
        durationSeconds = 5 * 60,
        heading = "SERVICE BEGINS IN"
    )
    @Volatile var foldback = RoutedTimerConfig(
        route = TimerRoute.FOLDBACK,
        durationSeconds = 15 * 60,
        heading = "TIME REMAINING"
    )

    fun configure(config: RoutedTimerConfig) {
        require(config.durationSeconds >= 0)
        when (config.route) {
            TimerRoute.AUDIENCE -> audience = config
            TimerRoute.FOLDBACK -> foldback = config
        }
    }
}
