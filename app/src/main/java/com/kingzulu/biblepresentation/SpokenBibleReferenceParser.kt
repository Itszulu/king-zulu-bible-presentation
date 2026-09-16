package com.kingzulu.biblepresentation

object SpokenBibleReferenceParser {
    private val numberWords = mapOf(
        "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5, "six" to 6,
        "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10, "eleven" to 11, "twelve" to 12,
        "thirteen" to 13, "fourteen" to 14, "fifteen" to 15, "sixteen" to 16, "seventeen" to 17,
        "eighteen" to 18, "nineteen" to 19, "twenty" to 20, "thirty" to 30, "forty" to 40,
        "fifty" to 50, "sixty" to 60, "seventy" to 70, "eighty" to 80, "ninety" to 90
    )

    private fun spokenNumber(tokens: List<String>): Int? {
        if (tokens.isEmpty()) return null
        tokens.joinToString("").toIntOrNull()?.let { return it }
        var total = 0
        var found = false
        for (token in tokens) {
            token.toIntOrNull()?.let { total += it; found = true; continue }
            val n = numberWords[token] ?: continue
            total += n
            found = true
        }
        return if (found) total else null
    }

    fun parse(transcript: String): BibleReference? {
        val clean = transcript.lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        val tokens = clean.split(" ")
        val chapterIndex = tokens.indexOfFirst { it == "chapter" }
        if (chapterIndex <= 0) return BibleReferenceParser.parse(clean)
        val verseIndex = tokens.indexOfFirst { it == "verse" || it == "verses" }
        if (verseIndex <= chapterIndex) return null
        val bookText = tokens.subList(0, chapterIndex).takeLast(3).joinToString(" ")
        val chapter = spokenNumber(tokens.subList(chapterIndex + 1, verseIndex)) ?: return null
        val afterVerse = tokens.drop(verseIndex + 1)
        val verse = spokenNumber(afterVerse.takeWhile { it != "to" && it != "through" }) ?: return null
        val separator = afterVerse.indexOfFirst { it == "to" || it == "through" }
        val verseEnd = if (separator >= 0) spokenNumber(afterVerse.drop(separator + 1)) else null
        return BibleReferenceParser.parse("$bookText $chapter $verse${verseEnd?.let { "-$it" } ?: ""}")
    }
}
