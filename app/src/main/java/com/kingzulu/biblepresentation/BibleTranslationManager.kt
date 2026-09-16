package com.kingzulu.biblepresentation

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * Installs authorised Bible modules into King Zulu's private offline library.
 * Modules are validated before replacing an installed translation, so a bad
 * download can never destroy the working local copy.
 */
object BibleTranslationManager {
    data class InstallResult(val success: Boolean, val abbreviation: String? = null, val message: String)

    fun install(context: Context, bytes: ByteArray): InstallResult {
        val json = runCatching { JSONObject(bytes.toString(Charsets.UTF_8)) }.getOrNull()
            ?: return InstallResult(false, message = "Invalid Bible module")
        val validation = validate(json)
        if (validation != null) return InstallResult(false, message = validation)

        val meta = json.getJSONObject("meta")
        val abbreviation = meta.getString("abbreviation").trim().uppercase()
        if (abbreviation == "KJV") return InstallResult(false, message = "Built-in KJV cannot be replaced")

        val dir = OfflineBibleRepository.moduleDirectory()
            ?: File(context.filesDir, "bibles").apply { mkdirs() }
        val target = File(dir, "$abbreviation.json")
        val temp = File(dir, "$abbreviation.installing")
        return runCatching {
            FileOutputStream(temp).use { out -> out.write(bytes); out.fd.sync() }
            // Parse the bytes we actually wrote before touching the working copy.
            JSONObject(temp.bufferedReader().use { it.readText() })
            if (target.exists() && !target.delete()) error("Unable to replace existing module")
            if (!temp.renameTo(target)) error("Unable to activate Bible module")
            OfflineBibleRepository.refreshModules()
            InstallResult(true, abbreviation, "$abbreviation installed for offline use")
        }.getOrElse {
            temp.delete()
            InstallResult(false, abbreviation, "Install failed — existing Bible remains unchanged")
        }
    }

    fun remove(abbreviation: String): Boolean {
        val key = abbreviation.trim().uppercase()
        if (key == "KJV") return false
        val file = OfflineBibleRepository.moduleDirectory()?.let { File(it, "$key.json") } ?: return false
        val removed = !file.exists() || file.delete()
        if (removed) {
            if (OfflineBibleRepository.selectedTranslation().equals(key, true)) OfflineBibleRepository.selectTranslation("KJV")
            OfflineBibleRepository.refreshModules()
        }
        return removed
    }

    private fun validate(json: JSONObject): String? {
        val meta = json.optJSONObject("meta") ?: return "Bible module metadata is missing"
        if (meta.optString("abbreviation").isBlank()) return "Translation abbreviation is missing"
        if (meta.optString("name").isBlank()) return "Translation name is missing"
        val books = json.optJSONArray("books") ?: return "Bible books are missing"
        if (books.length() < 1) return "Bible module contains no books"
        var verseCount = 0
        for (bi in 0 until books.length()) {
            val book = books.optJSONObject(bi) ?: continue
            if (book.optString("englishName").isBlank() && book.optString("book").isBlank()) continue
            val chapters = book.optJSONArray("chapters") ?: continue
            for (ci in 0 until chapters.length()) {
                val verses = chapters.optJSONObject(ci)?.optJSONArray("verses") ?: continue
                for (vi in 0 until verses.length()) if (verses.optJSONObject(vi)?.optString("text")?.isNotBlank() == true) verseCount++
            }
        }
        if (verseCount < 1) return "Bible module contains no readable verses"
        return null
    }
}
