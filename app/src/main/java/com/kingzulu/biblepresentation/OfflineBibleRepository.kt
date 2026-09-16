package com.kingzulu.biblepresentation

import android.content.Context
import org.json.JSONObject

data class Verse(val reference: BibleReference, val text: String, val translation: String)

object OfflineBibleRepository {
    private var appContext: Context? = null
    private var bible: JSONObject? = null

    fun initialize(context: Context) { appContext = context.applicationContext }

    private fun data(): JSONObject? {
        bible?.let { return it }
        val context = appContext ?: return null
        return runCatching {
            val json = context.assets.open("bibles/kjv.json").bufferedReader().use { it.readText() }
            JSONObject(json).also { bible = it }
        }.getOrNull()
    }

    private fun normalizeBookName(value: String): String = value.lowercase()
        .replace(Regex("[^a-z0-9]"), "")
        .replace("psalm", "psalms")

    fun get(r: BibleReference, translation: String = "KJV"): Verse? {
        if (!translation.equals("KJV", ignoreCase = true) || r.verseStart == null) return null
        val books = data()?.optJSONArray("books") ?: return null
        val wantedBook = normalizeBookName(r.book)
        var book: JSONObject? = null
        for (i in 0 until books.length()) {
            val candidate = books.optJSONObject(i) ?: continue
            // The bundled KJV stores short codes in `book` (Gen, Exod, Ps...) and the
            // canonical names in `englishName`. The old loader compared only `book`,
            // which made most normal searches such as Genesis/John/Romans fail.
            val englishName = normalizeBookName(candidate.optString("englishName"))
            val shortName = normalizeBookName(candidate.optString("book"))
            if (englishName == wantedBook || shortName == wantedBook) {
                book = candidate
                break
            }
        }
        val chapters = book?.optJSONArray("chapters") ?: return null
        val chapter = (0 until chapters.length())
            .mapNotNull { chapters.optJSONObject(it) }
            .firstOrNull { it.optInt("chapter") == r.chapter }
            ?: return null
        val verses = chapter.optJSONArray("verses") ?: return null
        val start = r.verseStart
        val end = r.verseEnd ?: start
        if (end < start) return null
        val texts = mutableListOf<String>()
        for (number in start..end) {
            val verse = (0 until verses.length())
                .mapNotNull { verses.optJSONObject(it) }
                .firstOrNull { it.optInt("number", it.optInt("verse")) == number }
                ?: return null
            val text = verse.optString("text").trim()
            if (text.isBlank()) return null
            texts += text
        }
        return Verse(r, texts.joinToString(" "), "KJV")
    }
}
