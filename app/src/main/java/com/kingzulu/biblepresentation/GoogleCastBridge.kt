package com.kingzulu.biblepresentation

import android.content.Context
import com.google.android.gms.cast.framework.CastContext

class GoogleCastBridge(context: Context) {
    private val castContext by lazy { CastContext.getSharedInstance(context) }

    fun hasActiveSession(): Boolean =
        castContext.sessionManager.currentCastSession?.isConnected == true

    fun currentDeviceName(): String? =
        castContext.sessionManager.currentCastSession?.castDevice?.friendlyName
}
