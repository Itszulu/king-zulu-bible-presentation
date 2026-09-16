package com.kingzulu.biblepresentation

import android.content.Context
import android.net.Uri
import java.io.File

data class ChurchBranding(val path: String, val mediaType: String)

object BrandingStore {
    private const val PREFS = "king_zulu_branding"
    private const val MAX_BYTES = 80L * 1024L * 1024L

    fun load(context: Context): ChurchBranding? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val path = p.getString("logo_path", null) ?: return null
        if (!File(path).isFile) return null
        return ChurchBranding(path, p.getString("logo_type", "image") ?: "image")
    }

    fun import(context: Context, uri: Uri): ChurchBranding? = runCatching {
        val mime = context.contentResolver.getType(uri).orEmpty()
        val type = if (mime.startsWith("video/")) "video" else "image"
        val dir = File(context.filesDir, "church_branding").apply { mkdirs() }
        val target = File(dir, if (type == "video") "logo.video" else "logo.image")
        val staged = File(dir, "logo.part")
        context.contentResolver.openInputStream(uri)?.use { input ->
            staged.outputStream().use { output ->
                val buffer = ByteArray(64 * 1024)
                var total = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    require(total <= MAX_BYTES) { "Brand media is too large" }
                    output.write(buffer, 0, read)
                }
            }
        } ?: return null
        if (target.exists()) target.delete()
        require(staged.renameTo(target))
        dir.listFiles()?.filter { it != target && it.name.startsWith("logo.") }?.forEach { it.delete() }
        ChurchBranding(target.absolutePath, type).also {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("logo_path", it.path).putString("logo_type", it.mediaType).apply()
        }
    }.getOrNull()

    fun clear(context: Context) {
        load(context)?.let { runCatching { File(it.path).delete() } }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun slide(context: Context): PresentationSlide {
        val brand = load(context)
        return if (brand == null) PresentationSlide(text = "KING ZULU", kind = "logo")
        else PresentationSlide(text = "", kind = "logo", mediaPath = brand.path, mediaType = brand.mediaType)
    }
}
