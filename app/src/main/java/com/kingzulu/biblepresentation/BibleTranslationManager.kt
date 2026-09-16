package com.kingzulu.biblepresentation

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/** Installs authorised Bible modules into King Zulu's private offline library. */
object BibleTranslationManager {
    data class InstallResult(val success: Boolean, val abbreviation: String? = null, val message: String)

    private val safeAbbreviation = Regex("[A-Z0-9_-]{2,16}")
    private const val MAX_MODULE_BYTES = 64 * 1024 * 1024

    fun install(context: Context, bytes: ByteArray): InstallResult {
        if (bytes.isEmpty()) return InstallResult(false, message = "Bible module is empty")
        if (bytes.size > MAX_MODULE_BYTES) return InstallResult(false, message = "Bible module is too large")

        val json = runCatching { JSONObject(bytes.toString(Charsets.UTF_8)) }.getOrNull()
            ?: return InstallResult(false, message = "Invalid Bible module")
        val validation = validate(json)
        if (validation != null) return InstallResult(false, message = validation)

        val abbreviation = json.getJSONObject("meta").getString("abbreviation").trim().uppercase()
        if (!safeAbbreviation.matches(abbreviation)) {
            return InstallResult(false, message = "Translation abbreviation is invalid")
        }
        if (abbreviation == "KJV") return InstallResult(false, message = "Built-in KJV cannot be replaced")

        val dir = OfflineBibleRepository.moduleDirectory()
            ?: File(context.filesDir, "bibles").apply { mkdirs() }
        if (!dir.exists() && !dir.mkdirs()) return InstallResult(false, abbreviation, "Unable to open offline Bible library")

        val target = File(dir, "$abbreviation.json")
        val part = File(dir, "$abbreviation.part")
        val backup = File(dir, "$abbreviation.backup")
        part.delete()
        backup.delete()

        return runCatching {
            FileOutputStream(part).use { out -> out.write(bytes); out.fd.sync() }
            val written = JSONObject(part.bufferedReader().use { it.readText() })
            validate(written)?.let { error(it) }

            var backedUp = false
            if (target.exists()) {
                if (!target.renameTo(backup)) error("Unable to protect existing Bible module")
                backedUp = true
            }

            try {
                if (!part.renameTo(target)) error("Unable to activate Bible module")
                backup.delete()
            } catch (activationError: Throwable) {
                target.delete()
                if (backedUp && !backup.renameTo(target)) {
                    throw IllegalStateException("Install failed and the previous module could not be restored", activationError)
                }
                throw activationError
            }

            OfflineBibleRepository.refreshModules()
            InstallResult(true, abbreviation, "$abbreviation installed for offline use")
        }.getOrElse {
            part.delete()
            if (!target.exists() && backup.exists()) backup.renameTo(target)
            OfflineBibleRepository.refreshModules()
            InstallResult(false, abbreviation, "Install failed — the previous Bible was preserved")
        }
    }

    fun remove(abbreviation: String): Boolean {
        val key = abbreviation.trim().uppercase()
        if (key == "KJV" || !safeAbbreviation.matches(key)) return false
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
        val abbreviation = meta.optString("abbreviation").trim().uppercase()
        if (abbreviation.isBlank()) return "Translation abbreviation is missing"
        if (!safeAbbreviation.matches(abbreviation)) return "Translation abbreviation is invalid"
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
                for (vi in 0 until verses.length()) {
                    if (verses.optJSONObject(vi)?.optString("text")?.isNotBlank() == true) verseCount++
                }
            }
        }
        if (verseCount < 1) return "Bible module contains no readable verses"
        return null
    }
}
