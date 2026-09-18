package com.kingzulu.biblepresentation

/** Persistent Bible context for hands-free commands such as next/previous/repeat verse. */
object AiScriptureEngine {
    @Volatile private var current: Verse? = null

    fun remember(verse: Verse) { current = verse }
    fun currentVerse(): Verse? = current

    fun resolveSpeech(alternatives: List<String>): Verse? {
        for (raw in alternatives) {
            resolveCommand(raw)?.let { remember(it); return it }
            SpokenBibleReferenceParser.parse(raw)?.let { ref ->
                OfflineBibleRepository.get(ref)?.let { remember(it); return it }
            }
        }
        return null
    }

    private fun resolveCommand(raw: String): Verse? {
        val base = current ?: return null
        val t = raw.lowercase().trim().replace(Regex("[^a-z0-9 ]"), " ").replace(Regex("\\s+"), " ")
        val ref = base.reference
        val currentVerse = ref.verseStart ?: return null
        fun verse(n: Int): Verse? = if (n > 0) OfflineBibleRepository.get(BibleReference(ref.book, ref.chapter, n)) else null
        fun nextVerse(): Verse? = verse(currentVerse + 1) ?: run {
            val chapters = OfflineBibleRepository.chapters(ref.book, base.translation)
            val ci = chapters.indexOf(ref.chapter)
            chapters.getOrNull(ci + 1)?.let { OfflineBibleRepository.chapter(ref.book, it, base.translation).firstOrNull() }
        }
        fun previousVerse(): Verse? = verse(currentVerse - 1) ?: run {
            val chapters = OfflineBibleRepository.chapters(ref.book, base.translation)
            val ci = chapters.indexOf(ref.chapter)
            chapters.getOrNull(ci - 1)?.let { OfflineBibleRepository.chapter(ref.book, it, base.translation).lastOrNull() }
        }

        if (t.matches(Regex(".*\\b(next verse|continue|go forward|carry on|keep reading)\\b.*"))) return nextVerse()
        if (t.matches(Regex(".*\\b(previous verse|last verse|go back one verse|go back a verse)\\b.*"))) return previousVerse()
        if (t.matches(Regex(".*\\b(repeat|repeat that|same verse|repeat verse)\\b.*"))) return base

        Regex("(?:go (?:back )?to |back to )?verse ([0-9]{1,3})").find(t)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { return verse(it) }
        Regex("(?:go to |open )?chapter ([0-9]{1,3})(?: verse ([0-9]{1,3}))?").find(t)?.let { m ->
            val chapter=m.groupValues[1].toIntOrNull()?:return@let
            val v=m.groupValues.getOrNull(2)?.toIntOrNull()?:1
            return OfflineBibleRepository.get(BibleReference(ref.book,chapter,v))
        }
        if (t.contains("next chapter")) {
            val chapters=OfflineBibleRepository.chapters(ref.book,base.translation);val ci=chapters.indexOf(ref.chapter)
            return chapters.getOrNull(ci+1)?.let { OfflineBibleRepository.chapter(ref.book,it,base.translation).firstOrNull() }
        }
        if (t.contains("previous chapter")) {
            val chapters=OfflineBibleRepository.chapters(ref.book,base.translation);val ci=chapters.indexOf(ref.chapter)
            return chapters.getOrNull(ci-1)?.let { OfflineBibleRepository.chapter(ref.book,it,base.translation).firstOrNull() }
        }
        return null
    }
}
