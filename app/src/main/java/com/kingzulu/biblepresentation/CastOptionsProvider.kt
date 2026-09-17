package com.kingzulu.biblepresentation

import android.content.Context
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider

/** Google Cast sender configuration for the registered King Zulu receiver. */
class CastOptionsProvider : OptionsProvider {
    companion object {
        const val RECEIVER_APPLICATION_ID = "86654938"
        const val PRESENTATION_NAMESPACE = "urn:x-cast:com.kingzulu.presentation"
    }

    override fun getCastOptions(context: Context): CastOptions =
        CastOptions.Builder()
            .setReceiverApplicationId(RECEIVER_APPLICATION_ID)
            .setStopReceiverApplicationWhenEndingSession(false)
            .build()

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? = null
}
