package com.kingzulu.biblepresentation

import android.app.Presentation
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.Display
import android.widget.FrameLayout
import android.widget.TextView
import android.graphics.Color
import android.view.Gravity
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider

class GoogleCastBridge(context: Context) {
    private val castContext by lazy { CastContext.getSharedInstance(context.applicationContext) }
    fun hasActiveSession(): Boolean = castContext.sessionManager.currentCastSession?.isConnected == true
    fun currentDeviceName(): String? = castContext.sessionManager.currentCastSession?.castDevice?.friendlyName
}

class KingZuluCastOptionsProvider : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions = CastOptions.Builder().setReceiverApplicationId("CC1AD845").build()
    override fun getAdditionalSessionProviders(context: Context): MutableList<SessionProvider>? = null
}

data class WiredDisplay(val id: Int, val name: String)

/** Detects Android secondary presentation displays such as USB-C -> HDMI. */
class WiredDisplayBridge(private val context: Context) {
    private val manager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    private var presentation: Presentation? = null

    fun available(): List<WiredDisplay> = manager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
        .filter { it.displayId != Display.DEFAULT_DISPLAY }
        .map { WiredDisplay(it.displayId, it.name.ifBlank { "External display" }) }

    fun show(displayId: Int): Boolean {
        val display = manager.displays.firstOrNull { it.displayId == displayId } ?: return false
        dismiss()
        return runCatching {
            presentation = KingZuluWiredPresentation(context, display).also { it.show() }
            true
        }.getOrDefault(false)
    }

    fun dismiss() { runCatching { presentation?.dismiss() }; presentation = null }
}

private class KingZuluWiredPresentation(context: Context, display: Display) : Presentation(context, display) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = FrameLayout(context).apply { setBackgroundColor(Color.BLACK) }
        val status = TextView(context).apply {
            text = "KING ZULU\nExternal Display Ready"
            setTextColor(Color.WHITE); textSize = 28f; gravity = Gravity.CENTER
        }
        root.addView(status, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        setContentView(root)
    }
}
