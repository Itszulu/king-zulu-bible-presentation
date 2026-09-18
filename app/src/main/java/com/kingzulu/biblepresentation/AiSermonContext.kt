package com.kingzulu.biblepresentation

/**
 * Lightweight local discourse state for AI Listen. It keeps recognition fast while separating
 * mentioned/listed Scriptures from the Scripture the preacher actually asks to read.
 */
enum class ScriptureSpeechIntent { DIRECT_READ, LISTING, NAVIGATION, MENTION, UNKNOWN }

data class ScriptureSpeechDecision(
    val intent: ScriptureSpeechIntent,
    val activate: Verse? = null,
    val queue: List<Verse> = emptyList(),
    val confidence: Double = 0.0
)

object AiSermonContext {
    @Volatile private var active: Verse? = null
    private val mentioned = ArrayDeque<Verse>()
    private val queued = mutableListOf<Verse>()

    fun activeVerse(): Verse? = active
    fun queue(): List<Verse> = synchronized(queued) { queued.toList() }

    fun decide(alternatives: List<String>): ScriptureSpeechDecision {
        for (raw in alternatives) {
            val text = normalize(raw)

            // Navigation has the fastest path and uses the current Scripture context.
            AiScriptureEngine.resolveSpeech(listOf(raw))?.let { resolved ->
                if (isNavigation(text)) return activate(resolved, ScriptureSpeechIntent.NAVIGATION, .99)
            }

            val refs = extractReferences(raw)
            if (refs.isEmpty()) continue
            refs.forEach(::rememberMention)

            // Multiple references or explicit list language means prepare them, don't flash each live.
            if (refs.size > 1 || isListLanguage(text)) {
                synchronized(queued) { queued.clear(); queued.addAll(refs) }
                resolveQueueSelection(text)?.let { return activate(it, ScriptureSpeechIntent.DIRECT_READ, .96) }
                return ScriptureSpeechDecision(ScriptureSpeechIntent.LISTING, queue = refs, confidence = .96)
            }

            val only = refs.first()
            if (isDirectRead(text)) return activate(only, ScriptureSpeechIntent.DIRECT_READ, .98)
            if (isMentionOnly(text)) return ScriptureSpeechDecision(ScriptureSpeechIntent.MENTION, queue = queue(), confidence = .90)

            // A clean standalone reference is treated as operator-worthy, preserving the fast path.
            if (looksLikeStandaloneReference(text)) return activate(only, ScriptureSpeechIntent.DIRECT_READ, .94)
            return ScriptureSpeechDecision(ScriptureSpeechIntent.MENTION, queue = queue(), confidence = .72)
        }
        // Commands such as "read the second one" may contain no full reference.
        alternatives.firstNotNullOfOrNull { resolveQueueSelection(normalize(it)) }?.let {
            return activate(it, ScriptureSpeechIntent.DIRECT_READ, .95)
        }
        return ScriptureSpeechDecision(ScriptureSpeechIntent.UNKNOWN)
    }

    private fun activate(v: Verse, intent: ScriptureSpeechIntent, confidence: Double): ScriptureSpeechDecision {
        active = v
        AiScriptureEngine.remember(v)
        return ScriptureSpeechDecision(intent, activate = v, queue = queue(), confidence = confidence)
    }

    private fun extractReferences(raw: String): List<Verse> {
        val normalized = raw.replace(";", ",")
        val pieces = normalized.split(Regex(",|\\band\\b|\\bthen\\b"), limit = 12)
        val found = mutableListOf<Verse>()
        pieces.forEach { part ->
            SpokenBibleReferenceParser.parse(part)?.let { ref -> OfflineBibleRepository.get(ref) }?.let { v ->
                if (found.none { it.reference.display() == v.reference.display() }) found += v
            }
        }
        if (found.isEmpty()) SpokenBibleReferenceParser.parse(raw)?.let { OfflineBibleRepository.get(it) }?.let(found::add)
        return found
    }

    private fun resolveQueueSelection(t: String): Verse? {
        val q = queue()
        if (q.isEmpty()) return null
        val ordinal = mapOf("first" to 0,"second" to 1,"third" to 2,"fourth" to 3,"last" to q.lastIndex)
        ordinal.entries.firstOrNull { (word, _) -> Regex("\\b(read|take|go to|look at|lets read|let us read)?\\s*(the )?$word( one| scripture| passage)?\\b").containsMatchIn(t) }?.let { return q.getOrNull(it.value) }
        q.firstOrNull { v -> t.contains(v.reference.book.lowercase()) && (t.contains("read") || t.contains("take") || t.contains("look") || t.contains("go to")) }?.let { return it }
        return null
    }

    private fun rememberMention(v: Verse) { synchronized(mentioned) { mentioned.addLast(v); while (mentioned.size > 12) mentioned.removeFirst() } }
    private fun normalize(s: String) = s.lowercase().replace("let's", "lets").replace(Regex("[^a-z0-9 ]"), " ").replace(Regex("\\s+"), " ").trim()
    private fun isNavigation(t: String) = listOf("next verse","previous verse","repeat that","repeat verse","next chapter","previous chapter").any(t::contains) || Regex("\\b(go back to |go to )?verse \\d+\\b").containsMatchIn(t)
    private fun isListLanguage(t: String) = listOf("you can find this in","you can find these in","write down","note these scriptures","these scriptures","our texts are","references are","we will read","we'll read").any(t::contains)
    private fun isDirectRead(t: String) = listOf("lets read","let us read","turn to","open to","go to","give me","put up","show us","read from","we are reading").any(t::contains)
    private fun isMentionOnly(t: String) = listOf("people quote","not where","not going to","mentions","mentioned","for example","such as").any(t::contains)
    private fun looksLikeStandaloneReference(t: String): Boolean = t.split(" ").size <= 8
}
