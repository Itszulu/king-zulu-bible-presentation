package com.kingzulu.biblepresentation

import android.content.Context
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider

class GoogleCastBridge(context: Context) {
    private val castContext by lazy { CastContext.getSharedInstance(context.applicationContext) }

    fun hasActiveSession(): Boolean = castContext.sessionManager.currentCastSession?.isConnected == true
    fun currentDeviceName(): String? = castContext.sessionManager.currentCastSession?.castDevice?.friendlyName
}

/**
 * Cast framework configuration. Kept in an existing source file because the
 * repository connector currently rejects new-file writes. Google's Default
 * Media Receiver allows native Cast discovery without a custom receiver app.
 */
class KingZuluCastOptionsProvider : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions =
        CastOptions.Builder()
            .setReceiverApplicationId("CC1AD845")
            .build()

    override fun getAdditionalSessionProviders(context: Context): MutableList<SessionProvider>? = null
}
