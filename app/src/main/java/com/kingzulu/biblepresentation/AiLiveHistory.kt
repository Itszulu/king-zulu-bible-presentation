package com.kingzulu.biblepresentation
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
data class AiLiveHistoryEntry(val slide:PresentationSlide,val timestamp:Long)
object AiLiveHistory{
 private const val PREFS="ai_live_history";private const val KEY="entries";private const val MAX=100
 @Synchronized fun record(context:Context,slide:PresentationSlide){
  if(slide.reference.isBlank()||slide.text.isBlank())return
  val items=entries(context).toMutableList();val now=System.currentTimeMillis();val last=items.lastOrNull()
  if(last!=null&&last.slide.reference==slide.reference&&last.slide.translation==slide.translation&&now-last.timestamp<10_000L)return
  items+=AiLiveHistoryEntry(slide,now);save(context,items.takeLast(MAX))
 }
 @Synchronized fun entries(context:Context):List<AiLiveHistoryEntry>{
  val raw=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY,"[]")?:"[]"
  return runCatching{val a=JSONArray(raw);(0 until a.length()).mapNotNull{i->val o=a.optJSONObject(i)?:return@mapNotNull null
   AiLiveHistoryEntry(PresentationSlide(o.optString("reference"),o.optString("text"),o.optString("translation")),o.optLong("timestamp"))
  }}.getOrDefault(emptyList())
 }
 @Synchronized fun clear(context:Context){context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().remove(KEY).apply()}
 private fun save(context:Context,items:List<AiLiveHistoryEntry>){val a=JSONArray();items.forEach{e->a.put(JSONObject().apply{put("reference",e.slide.reference);put("text",e.slide.text);put("translation",e.slide.translation);put("timestamp",e.timestamp)})};context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(KEY,a.toString()).apply()}
}
