package com.kingzulu.biblepresentation

import android.app.Activity
import android.content.Context
import android.view.WindowManager

/** Central policy for keeping operator/audience display sessions awake during services. */
object DisplayWakePolicy {
    private const val PREFS = "king_zulu_display_settings"
    private const val KEEP_AWAKE = "keep_audience_awake"

    fun enabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEEP_AWAKE, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEEP_AWAKE, enabled).apply()
    }

    fun apply(activity: Activity, activePresentationSession: Boolean) {
        val keepAwake = activePresentationSession && enabled(activity)
        if (keepAwake) activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
