package com.kingzulu.biblepresentation

import android.app.Presentation
import android.content.Context
import android.graphics.Color
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.Display
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.gms.cast.Cast
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import org.json.JSONObject

private const val KING_ZULU_CAST_NAMESPACE = "urn:x-cast:com.kingzulu.presentation"
private const val DEFAULT_MEDIA_RECEIVER_ID = "CC1AD845"

/** Google Cast is a real session transport. SSDP/DIAL discovery is deliberately not used here. */
class GoogleCastBridge(context: Context) {
    private val castContext by lazy { CastContext.getSharedInstance(context.applicationContext) }
    fun hasActiveSession(): Boolean = castContext.sessionManager.currentCastSession?.isConnected == true
    fun currentDeviceName(): String? = castContext.sessionManager.currentCastSession?.takeIf { it.isConnected }?.castDevice?.friendlyName

    /**
     * Sends King Zulu presentation state only when a receiver supports our namespace.
     * With the default receiver this safely returns false; a registered King Zulu Web Receiver
     * application ID can be supplied at build time before custom presentation messaging is enabled.
     */
    fun publish(slide: PresentationSlide?, black: Boolean, theme: BackgroundTheme?, textSize: Int, autoFit: Boolean): Boolean {
        val session = castContext.sessionManager.currentCastSession ?: return false
        if (!session.isConnected) return false
        val payload = JSONObject().apply {
            put("type", "presentation_state")
            put("black", black)
            put("reference", slide?.reference.orEmpty())
            put("text", slide?.text.orEmpty())
            put("translation", slide?.translation.orEmpty())
            put("kind", slide?.kind.orEmpty())
            put("textSize", textSize)
            put("autoFit", autoFit)
            put("overlay", theme?.overlay ?: .38f)
        }.toString()
        return runCatching {
            session.sendMessage(KING_ZULU_CAST_NAMESPACE, payload)
            true
        }.getOrDefault(false)
    }
}

class KingZuluCastOptionsProvider : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions {
        // CC1AD845 is Google's Default Media Receiver. It provides standards-compliant discovery
        // and session establishment now. King Zulu's own registered receiver ID replaces this
        // when the custom Web Receiver is deployed; never reuse another application's receiver ID.
        return CastOptions.Builder().setReceiverApplicationId(DEFAULT_MEDIA_RECEIVER_ID).build()
    }
    override fun getAdditionalSessionProviders(context: Context): MutableList<SessionProvider>? = null
}

data class WiredDisplay(val id: Int, val name: String)

class WiredDisplayBridge(private val context: Context) {
    private val manager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    private var presentation: KingZuluWiredPresentation? = null
    private var activeDisplayId: Int? = null
    private var lastSlide: PresentationSlide? = null
    private var black = false
    fun available(): List<WiredDisplay> = manager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION).filter { it.displayId != Display.DEFAULT_DISPLAY }.map { WiredDisplay(it.displayId, it.name.ifBlank { "External display" }) }
    fun show(displayId: Int): Boolean { val display=manager.displays.firstOrNull{it.displayId==displayId}?:return false;if(presentation?.isShowing==true&&activeDisplayId==displayId)return true;dismiss();return runCatching{presentation=KingZuluWiredPresentation(context,display).also{it.show();it.render(lastSlide,black)};activeDisplayId=displayId;true}.getOrDefault(false)}
    fun render(slide: PresentationSlide?, isBlack: Boolean=false){lastSlide=slide;black=isBlack;presentation?.render(slide,isBlack)}
    fun dismiss(){runCatching{presentation?.dismiss()};presentation=null;activeDisplayId=null}
}

private class KingZuluWiredPresentation(context: Context, display: Display) : Presentation(context, display) {
    private lateinit var referenceView:TextView;private lateinit var bodyView:TextView;private lateinit var translationView:TextView
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);val root=FrameLayout(context).apply{setBackgroundColor(Color.BLACK)};val content=LinearLayout(context).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;setPadding(72,56,72,56)};referenceView=TextView(context).apply{setTextColor(Color.rgb(185,173,255));textSize=26f;gravity=Gravity.CENTER};bodyView=TextView(context).apply{setTextColor(Color.WHITE);textSize=42f;gravity=Gravity.CENTER;setLineSpacing(8f,1.05f)};translationView=TextView(context).apply{setTextColor(Color.LTGRAY);textSize=20f;gravity=Gravity.CENTER};content.addView(referenceView,LinearLayout.LayoutParams(-1,-2));content.addView(bodyView,LinearLayout.LayoutParams(-1,-2).apply{topMargin=24;bottomMargin=24});content.addView(translationView,LinearLayout.LayoutParams(-1,-2));root.addView(content,FrameLayout.LayoutParams(-1,-1));setContentView(root)}
    fun render(slide:PresentationSlide?,black:Boolean){if(!::bodyView.isInitialized)return;window?.decorView?.setBackgroundColor(Color.BLACK);if(black||slide==null){referenceView.text="";bodyView.text="";translationView.text="";return};referenceView.text=slide.reference;bodyView.text=slide.text;translationView.text=slide.translation}
}
