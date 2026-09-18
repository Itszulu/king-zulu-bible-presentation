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
    val mode: ScriptureFoldbackMode = ScriptureFoldbackMode.CURRENT_NEXT
) {
    val current: Verse? get() = verses.firstOrNull { it.reference.verseStart == currentVerse }
    val next: Verse? get() = verses.firstOrNull { it.reference.verseStart == currentVerse + 1 }
    val remaining: List<Verse> get() = verses.filter { (it.reference.verseStart ?: 0) >= currentVerse }
}

/**
 * Local reading-range state. The audience sees only current; foldback may expose current+next
 * or the remaining passage. Advancement is driven by explicit commands or confident speech match,
 * never by elapsed time/silence.
 */
object ScriptureReadingSession {
    @Volatile private var state: ScriptureReadingState? = null

    fun currentState(): ScriptureReadingState? = state
    fun clear() { state = null }

    fun start(reference: BibleReference, translation: String = OfflineBibleRepository.selectedTranslation()): ScriptureReadingState? {
        val start = reference.verseStart ?: return null
        val end = reference.verseEnd ?: start
        if (end < start) return null
        val chapter = OfflineBibleRepository.chapter(reference.book, reference.chapter, translation)
        val range = chapter.filter { (it.reference.verseStart ?: 0) in start..end }
        if (range.isEmpty()) return null
        return ScriptureReadingState(reference.book, reference.chapter, start, end, start, range).also { state = it }
    }

    fun setMode(mode: ScriptureFoldbackMode) { state = state?.copy(mode = mode) }

    fun goTo(verseNumber: Int): Verse? {
        val s = state ?: return null
        if (verseNumber !in s.startVerse..s.endVerse) return null
        val target = s.verses.firstOrNull { it.reference.verseStart == verseNumber } ?: return null
        state = s.copy(currentVerse = verseNumber)
        AiScriptureEngine.remember(target)
        return target
    }

    fun next(): Verse? = state?.let { goTo(it.currentVerse + 1) }
    fun previous(): Verse? = state?.let { goTo(it.currentVerse - 1) }

    /**
     * Follow speech inside the declared range. Prefer upcoming verses, permit skips, and require
     * a strong wording match. This deliberately does not advance on silence or a timer.
     */
    fun followSpeech(spoken: String): Verse? {
        val s = state ?: return null
        val normalized = normalize(spoken)
        if (normalized.split(' ').size < 3) return null
        val candidates = s.verses.filter { (it.reference.verseStart ?: 0) > s.currentVerse }
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
