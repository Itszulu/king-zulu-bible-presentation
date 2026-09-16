package com.kingzulu.biblepresentation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

/**
 * Online passage provider for Bible versions made available to King Zulu
 * through YouVersion Platform. The App Key is injected at build/runtime;
 * it must never be committed to source control.
 */
object YouVersionBibleProvider {
    private const val BASE_URL = "https://api.youversion.com/v1"

    suspend fun get(reference: BibleReference, translation: BibleTranslation, appKey: String): Verse? = withContext(Dispatchers.IO) {
        if (appKey.isBlank() || translation.id == null || reference.verseStart == null) return@withContext null
        val passage = toUsfm(reference) ?: return@withContext null
        val encoded = URLEncoder.encode(passage, Charsets.UTF_8.name()).replace("+", "%20")
        val connection = (URL("$BASE_URL/bibles/${translation.id}/passages/$encoded?format=text&include_headings=false&include_notes=false").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 7000
            readTimeout = 7000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("X-YVP-App-Key", appKey)
        }
        try {
            if (connection.responseCode !in 200..299) return@withContext null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val text = json.optString("content").trim()
            if (text.isBlank()) return@withContext null
            Verse(reference, text, translation.abbreviation)
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun toUsfm(r: BibleReference): String? {
        val book = USFM_BOOKS[normalizeBook(r.book)] ?: return null
        val start = r.verseStart ?: return "$book.${r.chapter}"
        val end = r.verseEnd
        return if (end != null && end != start) "$book.${r.chapter}.$start-$book.${r.chapter}.$end" else "$book.${r.chapter}.$start"
    }

    private fun normalizeBook(value: String) = value.lowercase().replace(Regex("[^a-z0-9]"), "").replace("psalm", "psalms")

    private val USFM_BOOKS = mapOf(
        "genesis" to "GEN", "exodus" to "EXO", "leviticus" to "LEV", "numbers" to "NUM", "deuteronomy" to "DEU",
        "joshua" to "JOS", "judges" to "JDG", "ruth" to "RUT", "1samuel" to "1SA", "2samuel" to "2SA",
        "1kings" to "1KI", "2kings" to "2KI", "1chronicles" to "1CH", "2chronicles" to "2CH", "ezra" to "EZR",
        "nehemiah" to "NEH", "esther" to "EST", "job" to "JOB", "psalms" to "PSA", "proverbs" to "PRO",
        "ecclesiastes" to "ECC", "songofsolomon" to "SNG", "songofsongs" to "SNG", "isaiah" to "ISA", "jeremiah" to "JER",
        "lamentations" to "LAM", "ezekiel" to "EZK", "daniel" to "DAN", "hosea" to "HOS", "joel" to "JOL",
        "amos" to "AMO", "obadiah" to "OBA", "jonah" to "JON", "micah" to "MIC", "nahum" to "NAM",
        "habakkuk" to "HAB", "zephaniah" to "ZEP", "haggai" to "HAG", "zechariah" to "ZEC", "malachi" to "MAL",
        "matthew" to "MAT", "mark" to "MRK", "luke" to "LUK", "john" to "JHN", "acts" to "ACT",
        "romans" to "ROM", "1corinthians" to "1CO", "2corinthians" to "2CO", "galatians" to "GAL", "ephesians" to "EPH",
        "philippians" to "PHP", "colossians" to "COL", "1thessalonians" to "1TH", "2thessalonians" to "2TH", "1timothy" to "1TI",
        "2timothy" to "2TI", "titus" to "TIT", "philemon" to "PHM", "hebrews" to "HEB", "james" to "JAS",
        "1peter" to "1PE", "2peter" to "2PE", "1john" to "1JN", "2john" to "2JN", "3john" to "3JN",
        "jude" to "JUD", "revelation" to "REV"
    )
}
