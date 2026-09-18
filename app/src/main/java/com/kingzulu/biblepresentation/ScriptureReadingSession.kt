package com.kingzulu.biblepresentation

/** How the stage display assists a preacher through a declared Scripture range. */
enum class ScriptureFoldbackMode { CURRENT_NEXT, TELEPROMPTER }

data class ScriptureReadingState(
    val book: String,
    val chapter: Int,
    val startVerse: Int,
    val endVerse: Int,
    val currentVerse: Int,
    val verses: List<Verse>,
    val mode: ScriptureFoldbackMode = ScriptureFoldbackMode.CURRENT_NEXT,
    val declaredStartVerse: Int = startVerse,
    val declaredEndVerse: Int = endVerse
) {
    val current: Verse? get() = verses.firstOrNull { it.reference.verseStart == currentVerse }
    val next: Verse? get() = verses.firstOrNull { it.reference.verseStart == currentVerse + 1 }
    val remaining: List<Verse> get() = verses.filter { (it.reference.verseStart ?: 0) >= currentVerse }
    val insideDeclaredRange: Boolean get() = currentVerse in declaredStartVerse..declaredEndVerse
    val atDeclaredEnd: Boolean get() = insideDeclaredRange && currentVerse == declaredEndVerse
}

/**
 * Local reading state for the active book/chapter. A declared range controls automatic reading
 * progression, but explicit contextual commands may jump anywhere in the same chapter.
 * The audience sees only current; foldback may expose current+next or the remaining passage.
 */
object ScriptureReadingSession {
    @Volatile private var state: ScriptureReadingState? = null

    fun currentState(): ScriptureReadingState? = state
    fun clear() { state = null }

    fun start(reference: BibleReference, translation: String = OfflineBibleRepository.selectedTranslation()): ScriptureReadingState? {
        val start = reference.verseStart ?: return null
        val end = reference.verseEnd ?: start
        if (end < start) return null
        val chapterVerses = OfflineBibleRepository.chapter(reference.book, reference.chapter, translation)
        if (chapterVerses.none { (it.reference.verseStart ?: 0) == start }) return null
        return ScriptureReadingState(
            book = reference.book,
            chapter = reference.chapter,
            startVerse = start,
            endVerse = end,
            currentVerse = start,
            verses = chapterVerses,
            declaredStartVerse = start,
            declaredEndVerse = end
        ).also { state = it }
    }

    fun setMode(mode: ScriptureFoldbackMode) { state = state?.copy(mode = mode) }

    /** Explicit verse navigation inherits the active book/chapter and may leave the declared range. */
    fun goTo(verseNumber: Int): Verse? {
        val s = state ?: return null
        val target = s.verses.firstOrNull { it.reference.verseStart == verseNumber } ?: return null
        state = s.copy(currentVerse = verseNumber)
        AiScriptureEngine.remember(target)
        return target
    }

    /**
     * NEXT/PREVIOUS are contextual chapter navigation. At the declared range endpoint, NEXT does
     * not silently escape the reading; an explicit verse command can still jump elsewhere.
     */
    fun next(): Verse? {
        val s = state ?: return null
        if (s.atDeclaredEnd) return null
        return goTo(s.currentVerse + 1)
    }
    fun previous(): Verse? = state?.let { goTo(it.currentVerse - 1) }

    /**
     * Follow spoken Bible wording only inside the declared range. Prefer upcoming verses, permit
     * skips, and require a strong wording match. Explicit verse commands are handled by goTo().
     */
    fun followSpeech(spoken: String): Verse? {
        val s = state ?: return null
        if (!s.insideDeclaredRange || s.atDeclaredEnd) return null
        val normalized = normalize(spoken)
        if (normalized.split(' ').size < 3) return null
        val candidates = s.verses.filter {
            val n = it.reference.verseStart ?: 0
            n > s.currentVerse && n <= s.declaredEndVerse
        }
        var best: Pair<Verse, Double>? = null
        for (v in candidates) {
            val score = similarity(normalized, normalize(v.text))
            if (best == null || score > best!!.second) best = v to score
        }
        val chosen = best ?: return null
        val distance = (chosen.first.reference.verseStart ?: s.currentVerse) - s.currentVerse
        val threshold = if (distance <= 2) .52 else .66
        return if (chosen.second >= threshold) goTo(chosen.first.reference.verseStart ?: return null) else null
    }

    private fun normalize(value: String): String = value.lowercase().replace(Regex("[^a-z0-9 ]"), " ").replace(Regex("\\s+"), " ").trim()
    private fun similarity(spoken: String, verse: String): Double {
        val a = spoken.split(' ').filter { it.length > 1 }
        val b = verse.split(' ').filter { it.length > 1 }
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val head = b.take(12)
        val overlap = a.takeLast(14).count { it in head }.toDouble() / minOf(a.takeLast(14).size, head.size).coerceAtLeast(1)
        val ordered = a.takeLast(14).windowed(2).count { pair -> verse.contains(pair.joinToString(" ")) }
        return overlap + ordered * .08
    }
}
