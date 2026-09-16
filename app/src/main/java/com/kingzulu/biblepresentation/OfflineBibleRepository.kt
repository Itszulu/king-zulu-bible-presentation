package com.kingzulu.biblepresentation

import android.content.Context
import org.json.JSONObject

data class Verse(val reference: BibleReference, val text: String, val translation: String)

object OfflineBibleRepository {
    private var appContext: Context? = null
    private var bible: JSONObject? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private fun data(): JSONObject? {
        bible?.let { return it }
        val context = appContext ?: return null
        return runCatching {
            val json = context.assets.open("bibles/kjv.json").bufferedReader().use { it.readText() }
            JSONObject(json).also { bible = it }
        }.getOrNull()
    }

    fun get(r: BibleReference, translation: String = "KJV"): Verse? {
        if (!translation.equals("KJV", ignoreCase = true) || r.verseStart == null) return null
        val root = data() ?: return null
        val books = root.optJSONArray("books") ?: return null
        var book: JSONObject? = null
        for (i in 0 until books.length()) {
            val candidate = books.optJSONObject(i) ?: continue
            if (candidate.optString("book").equals(r.book, ignoreCase = true)) {
                book = candidate
                break
            }
        }
        val chapters = book?.optJSONArray("chapters") ?: return null
        val chapter = (0 until chapters.length())
            .mapNotNull { chapters.optJSONObject(it) }
            .firstOrNull { it.optInt("chapter") == r.chapter }
            ?: chapters.optJSONObject(r.chapter - 1)
            ?: return null
        val verses = chapter.optJSONArray("verses") ?: return null
        val wanted = r.verseStart
        val verse = (0 until verses.length())
            .mapNotNull { verses.optJSONObject(it) }
            .firstOrNull { it.optInt("verse", it.optInt("number")) == wanted }
            ?: verses.optJSONObject(wanted - 1)
            ?: return null
        val text = verse.optString("text").takeIf { it.isNotBlank() } ?: return null
        return Verse(r, text, "KJV")
    }
}
