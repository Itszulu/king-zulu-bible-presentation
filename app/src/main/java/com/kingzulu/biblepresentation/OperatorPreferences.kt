package com.kingzulu.biblepresentation

import android.content.Context

/** Persistent service-critical operator preferences shared by Bible, AI, Songs and Media. */
object OperatorPreferences {
    private const val PREFS = "king_zulu_operator"
    private const val ROUTE_ALL = "route_all_outputs"
    private const val AI_AUTO = "ai_auto_live"

    fun routeAllOutputs(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(ROUTE_ALL, true)

    fun setRouteAllOutputs(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(ROUTE_ALL, enabled).apply()
    }

    fun aiAutoLive(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(AI_AUTO, false)

    fun setAiAutoLive(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(AI_AUTO, enabled).apply()
    }
}
