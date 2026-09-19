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
import com.google.android.gms.cast.framework.CastContext
import org.json.JSONObject

class GoogleCastBridge(context: Context) {
    private val castContext by lazy { CastContext.getSharedInstance(context.applicationContext) }
    fun hasActiveSession(): Boolean = castContext.sessionManager.currentCastSession?.isConnected == true
    fun currentDeviceName(): String? = castContext.sessionManager.currentCastSession?.takeIf { it.isConnected }?.castDevice?.friendlyName
    fun assignedBus(): PresentationBus = OutputRouting.busFor(OutputTransport.CAST)
    fun assignBus(bus: PresentationBus) = OutputRouting.assign(OutputTransport.CAST, bus)
    fun publish(slide: PresentationSlide?, black: Boolean, theme: BackgroundTheme?, textSize: Int, autoFit: Boolean): Boolean {
        val session = castContext.sessionManager.currentCastSession ?: return false
        if (!session.isConnected) return false
        val bus = assignedBus()
        val payload = JSONObject().apply {
            put("type", "presentation_state"); put("bus", bus.name.lowercase()); put("black", black); put("reference", slide?.reference.orEmpty()); put("text", slide?.text.orEmpty()); put("translation", slide?.translation.orEmpty()); put("kind", slide?.kind.orEmpty()); put("mediaType", slide?.mediaType.orEmpty()); put("hasMedia", !slide?.mediaPath.isNullOrBlank()); put("textSize", textSize); put("autoFit", autoFit); put("overlay", theme?.overlay ?: .38f)
            if (slide?.kind == "countdown" && bus == PresentationBus.MAIN) { CongregationTimer.refresh(); put("countdownRemaining", CongregationTimer.remainingSeconds); put("countdownRunning", CongregationTimer.running); put("serverNow", System.currentTimeMillis()) }
            if (bus == PresentationBus.FOLDBACK) { FoldbackTimer.refresh(); val model = ScriptureFoldbackRenderer.render(); put("foldback", JSONObject().apply { put("currentReference", model?.currentReference.orEmpty()); put("currentText", model?.currentText.orEmpty()); put("translation", model?.translation.orEmpty()); put("nextReference", model?.nextReference.orEmpty()); put("nextText", model?.nextText.orEmpty()); put("currentWeight", model?.currentWeight ?: 1f); put("nextWeight", model?.nextWeight ?: 0f); put("endOfReading", model?.endOfReading ?: false); put("endLabel", model?.endLabel.orEmpty()); put("mode", model?.mode?.name.orEmpty()); put("teleprompter", org.json.JSONArray().apply { model?.teleprompter?.forEach { (reference, text) -> put(JSONObject().put("reference", reference).put("text", text)) } }); put("stageTimer", FoldbackTimer.display()); put("stageTimerRunning", FoldbackTimer.running); put("stageTimerOvertime", FoldbackTimer.remainingSeconds < 0L); put("clockEnabled", FoldbackWidgetState.layout.clock.enabled); put("clock24Hour", FoldbackWidgetState.layout.clock.use24Hour); put("clockSeconds", FoldbackWidgetState.layout.clock.showSeconds); put("clockAnchor", FoldbackWidgetState.layout.clock.anchor.name); put("alertEnabled", FoldbackWidgetState.layout.alert.enabled); put("alertMessage", FoldbackWidgetState.layout.alert.message); put("alertCycles", FoldbackWidgetState.layout.alert.cycleCount); put("alertSpeed", FoldbackWidgetState.layout.alert.speedPercent) }) }
        }.toString()
        return runCatching { session.sendMessage(CastOptionsProvider.PRESENTATION_NAMESPACE, payload); true }.getOrDefault(false)
    }
}
data class WiredDisplay(val id: Int, val name: String)
class WiredDisplayBridge(private val context: Context) {
    private val manager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    private var presentation: KingZuluWiredPresentation? = null; private var activeDisplayId: Int? = null; private var lastSlide: PresentationSlide? = null; private var black = false
    fun assignedBus(): PresentationBus = OutputRouting.busFor(OutputTransport.WIRED); fun assignBus(bus: PresentationBus) = OutputRouting.assign(OutputTransport.WIRED, bus)
    fun available(): List<WiredDisplay> = manager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION).filter { it.displayId != Display.DEFAULT_DISPLAY }.map { WiredDisplay(it.displayId, it.name.ifBlank { "External display" }) }
    fun isActive(): Boolean = activeDisplayId != null && presentation?.isShowing == true
    fun activeDisplayName(): String? = activeDisplayId?.let { id -> manager.displays.firstOrNull { it.displayId == id }?.name }
    fun show(displayId: Int): Boolean { val display = manager.displays.firstOrNull { it.displayId == displayId } ?: return false; if (presentation?.isShowing == true && activeDisplayId == displayId) return true; dismiss(); return runCatching { presentation = KingZuluWiredPresentation(context, display).also { it.show(); it.render(lastSlide, black, assignedBus()) }; activeDisplayId = displayId; true }.getOrDefault(false) }
    fun render(slide: PresentationSlide?, isBlack: Boolean=false){lastSlide=slide;black=isBlack;presentation?.render(slide,isBlack,assignedBus())}; fun dismiss(){runCatching{presentation?.dismiss()};presentation=null;activeDisplayId=null}
}
private class KingZuluWiredPresentation(context: Context, display: Display) : Presentation(context, display) {
    private lateinit var referenceView:TextView; private lateinit var bodyView:TextView; private lateinit var translationView:TextView; private lateinit var content:LinearLayout
    override fun onCreate(savedInstanceState:Bundle?){ super.onCreate(savedInstanceState); val root=FrameLayout(context).apply{setBackgroundColor(Color.BLACK)}; content=LinearLayout(context).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;setPadding(72,56,72,56)}; referenceView=TextView(context).apply{setTextColor(Color.rgb(185,173,255));textSize=26f;gravity=Gravity.CENTER}; bodyView=TextView(context).apply{setTextColor(Color.WHITE);textSize=42f;gravity=Gravity.CENTER;setLineSpacing(8f,1.05f)}; translationView=TextView(context).apply{setTextColor(Color.LTGRAY);textSize=20f;gravity=Gravity.CENTER;setLineSpacing(5f,1.04f)}; content.addView(referenceView,LinearLayout.LayoutParams(-1,-2)); content.addView(bodyView,LinearLayout.LayoutParams(-1,0,1f).apply{topMargin=24;bottomMargin=18}); content.addView(translationView,LinearLayout.LayoutParams(-1,-2)); root.addView(content,FrameLayout.LayoutParams(-1,-1));setContentView(root) }
    fun render(slide:PresentationSlide?,black:Boolean,bus:PresentationBus){ if(!::bodyView.isInitialized)return; window?.decorView?.setBackgroundColor(Color.BLACK); if(black){referenceView.text="";bodyView.text="";translationView.text="";return}; if(bus==PresentationBus.FOLDBACK){ FoldbackTimer.refresh(); val timer=if(FoldbackTimer.running||FoldbackTimer.remainingSeconds!=FoldbackTimer.configuredSeconds)FoldbackTimer.display() else ""; val model=ScriptureFoldbackRenderer.render(); if(model==null){referenceView.text=if(timer.isBlank())"" else "STAGE TIMER · $timer";bodyView.text=slide?.text.orEmpty();translationView.text=slide?.translation.orEmpty();return}; referenceView.text=(if(timer.isBlank())"" else "STAGE TIMER · $timer\n")+"CURRENT · ${model.currentReference} · ${model.translation}"; bodyView.text=model.currentText; bodyView.textSize=when{model.currentText.length>300->34f;model.currentText.length>190->38f;else->44f}; if(model.mode==ScriptureFoldbackMode.TELEPROMPTER){translationView.text=model.teleprompter.drop(1).joinToString("\n\n"){(ref,text)->"$ref\n$text"};translationView.textSize=22f}else if(model.nextText!=null&&model.nextReference!=null){translationView.text="NEXT · ${model.nextReference}\n${model.nextText}";translationView.textSize=22f}else{translationView.text=model.endLabel.orEmpty();translationView.textSize=18f}; val currentParams=bodyView.layoutParams as LinearLayout.LayoutParams;currentParams.weight=model.currentWeight.coerceIn(.70f,1f);bodyView.layoutParams=currentParams; val nextParams=translationView.layoutParams as LinearLayout.LayoutParams;nextParams.height=if(model.nextWeight>0f)0 else LinearLayout.LayoutParams.WRAP_CONTENT;nextParams.weight=model.nextWeight.coerceAtLeast(0f);translationView.layoutParams=nextParams;return }; if(slide==null){referenceView.text="";bodyView.text="";translationView.text="";return}; referenceView.text=slide.reference;bodyView.text=slide.text;translationView.text=slide.translation; bodyView.textSize=42f;translationView.textSize=20f; (bodyView.layoutParams as LinearLayout.LayoutParams).also{it.height=LinearLayout.LayoutParams.WRAP_CONTENT;it.weight=0f;bodyView.layoutParams=it}; (translationView.layoutParams as LinearLayout.LayoutParams).also{it.height=LinearLayout.LayoutParams.WRAP_CONTENT;it.weight=0f;translationView.layoutParams=it} }
}
