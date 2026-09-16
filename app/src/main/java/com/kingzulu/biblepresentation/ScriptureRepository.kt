package com.kingzulu.biblepresentation

import android.content.Context

/** One Scripture entry point for Bible UI and AI Listen. */
object ScriptureRepository {
    private const val PREFS = "king_zulu_scripture"
    private const val KEY_TRANSLATION = "preferred_translation"

    fun preferredTranslation(context: Context): BibleTranslation {
        val abbr = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TRANSLATION, "KJV") ?: "KJV"
        return TranslationCatalog.byAbbreviation(abbr) ?: TranslationCatalog.translations.first()
    }

    fun setPreferredTranslation(context: Context, translation: BibleTranslation) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_TRANSLATION, translation.abbreviation).apply()
    }

    fun get(context: Context, reference: BibleReference, translation: BibleTranslation): Result<Verse> {
        if (translation.offline) {
            val verse = OfflineBibleRepository.get(reference)
                ?: return Result.failure(IllegalArgumentException("Verse not found"))
            return Result.success(verse)
        }
        return YouVersionPassageProvider.get(
            context = context,
            r = reference,
            translation = translation,
            appKey = BuildConfig.YOUVERSION_APP_KEY
        )
    }
}
