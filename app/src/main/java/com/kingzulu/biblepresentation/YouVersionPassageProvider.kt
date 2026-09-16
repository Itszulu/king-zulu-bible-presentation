package com.kingzulu.biblepresentation

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

object YouVersionPassageProvider {
    private const val BASE = "https://api.youversion.com/v1"
    private val memoryCache = ConcurrentHashMap<String, Verse>()

    private val usfm = mapOf(
        "genesis" to "GEN","exodus" to "EXO","leviticus" to "LEV","numbers" to "NUM","deuteronomy" to "DEU","joshua" to "JOS","judges" to "JDG","ruth" to "RUT","1 samuel" to "1SA","2 samuel" to "2SA","1 kings" to "1KI","2 kings" to "2KI","1 chronicles" to "1CH","2 chronicles" to "2CH","ezra" to "EZR","nehemiah" to "NEH","esther" to "EST","job" to "JOB","psalms" to "PSA","psalm" to "PSA","proverbs" to "PRO","ecclesiastes" to "ECC","song of solomon" to "SNG","isaiah" to "ISA","jeremiah" to "JER","lamentations" to "LAM","ezekiel" to "EZK","daniel" to "DAN","hosea" to "HOS","joel" to "JOL","amos" to "AMO","obadiah" to "OBA","jonah" to "JON","micah" to "MIC","nahum" to "NAM","habakkuk" to "HAB","zephaniah" to "ZEP","haggai" to "HAG","zechariah" to "ZEC","malachi" to "MAL","matthew" to "MAT","mark" to "MRK","luke" to "LUK","john" to "JHN","acts" to "ACT","romans" to "ROM","1 corinthians" to "1CO","2 corinthians" to "2CO","galatians" to "GAL","ephesians" to "EPH","philippians" to "PHP","colossians" to "COL","1 thessalonians" to "1TH","2 thessalonians" to "2TH","1 timothy" to "1TI","2 timothy" to "2TI","titus" to "TIT","philemon" to "PHM","hebrews" to "HEB","james" to "JAS","1 peter" to "1PE","2 peter" to "2PE","1 john" to "1JN","2 john" to "2JN","3 john" to "3JN","jude" to "JUD","revelation" to "REV")

    fun passageId(r: BibleReference): String? {
        val code = usfm[r.book.lowercase().trim()] ?: return null
        val start = r.verseStart ?: return "$code.${r.chapter}"
        val end = r.verseEnd
        return if (end != null && end != start) "$code.${r.chapter}.$start-$end" else "$code.${r.chapter}.$start"
    }

    fun get(context: Context, r: BibleReference, translation: BibleTranslation, appKey: String): Result<Verse> {
        val bibleId = translation.id ?: return Result.failure(IllegalArgumentException("Translation has no online Bible ID"))
        if (appKey.isBlank()) return Result.failure(IllegalStateException("Online Bible access is not configured in this King Zulu build."))
        val pid = passageId(r) ?: return Result.failure(IllegalArgumentException("Unsupported Bible reference"))
        val cacheKey = "$bibleId:$pid"
        memoryCache[cacheKey]?.let { return Result.success(it) }
        readDisk(context, cacheKey)?.let { memoryCache[cacheKey] = it; return Result.success(it) }
        return runCatching {
            val encoded = URLEncoder.encode(pid, "UTF-8").replace("%2E", ".")
            val conn = (URL("$BASE/bibles/$bibleId/passages/$encoded?format=text&include_headings=false&include_notes=false").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"; connectTimeout = 2500; readTimeout = 3500
                setRequestProperty("X-YVP-App-Key", appKey); setRequestProperty("Accept", "application/json")
            }
            try {
                val status = conn.responseCode
                if (status !in 200..299) {
                    val body = runCatching { conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty() }.getOrDefault("")
                    val message = when (status) {
                        401 -> "YouVersion rejected this app key. Check the configured API credential."
                        403 -> "${translation.abbreviation} is not authorised for this YouVersion app key, or passage access is not licensed."
                        404 -> "${translation.abbreviation} passage was not found for ${r.book} ${r.chapter}."
                        429 -> "YouVersion request limit reached. Please try again shortly."
                        else -> "YouVersion request failed ($status)."
                    }
                    throw IllegalStateException(if (body.isBlank()) message else message)
                }
                val root = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                val data = root.optJSONObject("data") ?: root
                val text = data.optString("content").trim()
                if (text.isBlank()) throw IllegalStateException("YouVersion returned no ${translation.abbreviation} passage text.")
                Verse(r, text, translation.abbreviation).also { verse -> memoryCache[cacheKey] = verse; writeDisk(context, cacheKey, verse) }
            } finally { conn.disconnect() }
        }
    }

    private fun prefs(context: Context) = context.getSharedPreferences("yv_passage_cache", Context.MODE_PRIVATE)
    private fun readDisk(context: Context, key: String): Verse? {
        val raw = prefs(context).getString(key, null) ?: return null
        return runCatching { val j=JSONObject(raw); val r=BibleReference(j.getString("book"),j.getInt("chapter"),j.getInt("start"),j.optInt("end").takeIf{it>0}); Verse(r,j.getString("text"),j.getString("translation")) }.getOrNull()
    }
    private fun writeDisk(context: Context, key: String, verse: Verse) {
        val j=JSONObject().put("book",verse.reference.book).put("chapter",verse.reference.chapter).put("start",verse.reference.verseStart?:0).put("end",verse.reference.verseEnd?:0).put("text",verse.text).put("translation",verse.translation)
        prefs(context).edit().putString(key,j.toString()).apply()
    }
}
