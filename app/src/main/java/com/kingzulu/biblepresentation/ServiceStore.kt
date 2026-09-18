package com.kingzulu.biblepresentation

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ServiceStore {
    private const val PREFS = "king_zulu_service"
    private const val KEY = "running_order"

    fun load(context: Context): List<PresentationSlide> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        return runCatching {
            val a = JSONArray(raw)
            (0 until a.length()).mapNotNull { i ->
                val o = a.optJSONObject(i) ?: return@mapNotNull null
                PresentationSlide(
                    reference = o.optString("reference"),
                    text = o.optString("text"),
                    translation = o.optString("translation"),
                    kind = o.optString("kind")
                )
            }
        }.getOrDefault(emptyList())
    }

    fun save(context: Context, slides: List<PresentationSlide>) {
        val a = JSONArray()
        slides.forEach { s ->
            a.put(JSONObject().apply {
                put("reference", s.reference)
                put("text", s.text)
                put("translation", s.translation)
                put("kind", s.kind)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, a.toString()).apply()
    }
}
