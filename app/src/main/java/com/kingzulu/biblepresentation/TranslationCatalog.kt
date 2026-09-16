package com.kingzulu.biblepresentation

data class BibleTranslation(
    val id: Int?,
    val abbreviation: String,
    val name: String,
    val offline: Boolean,
    val aliases: Set<String>
)

object TranslationCatalog {
    val translations = listOf(
        BibleTranslation(null, "KJV", "King James Version", true, setOf("kjv", "king james", "king james version")),
        BibleTranslation(111, "NIV", "New International Version", false, setOf("niv", "new international version")),
        BibleTranslation(12, "ASV", "American Standard Version", false, setOf("asv", "american standard", "american standard version")),
        BibleTranslation(206, "WEBUS", "World English Bible", false, setOf("web", "webus", "world english bible")),
        BibleTranslation(3034, "BSB", "Berean Standard Bible", false, setOf("bsb", "berean standard", "berean standard bible"))
    )

    fun detectCommand(spoken: String): BibleTranslation? {
        val s = spoken.lowercase().replace(Regex("[^a-z0-9 ]"), " ").replace(Regex("\\s+"), " ").trim()
        val commandLanguage = listOf("show", "see", "switch", "change", "version", "translation", "read", "give me")
        if (commandLanguage.none { s.contains(it) }) return null
        return translations.firstOrNull { t -> t.aliases.any { alias -> s.contains(alias) } }
    }

    fun byAbbreviation(value: String) = translations.firstOrNull { it.abbreviation.equals(value, true) }
}
