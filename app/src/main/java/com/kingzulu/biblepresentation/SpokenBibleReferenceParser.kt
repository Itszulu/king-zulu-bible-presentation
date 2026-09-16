package com.kingzulu.biblepresentation

object SpokenBibleReferenceParser {
    private val numberWords = mapOf(
        "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5, "six" to 6,
        "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10, "eleven" to 11, "twelve" to 12,
        "thirteen" to 13, "fourteen" to 14, "fifteen" to 15, "sixteen" to 16, "seventeen" to 17,
        "eighteen" to 18, "nineteen" to 19, "twenty" to 20, "thirty" to 30, "forty" to 40,
        "fifty" to 50, "sixty" to 60, "seventy" to 70, "eighty" to 80, "ninety" to 90
    )
    private val ordinals = mapOf("first" to "1", "second" to "2", "third" to "3")
    private val books = listOf(
        "song of solomon","song of songs","1 thessalonians","2 thessalonians","1 corinthians","2 corinthians",
        "1 chronicles","2 chronicles","1 samuel","2 samuel","1 kings","2 kings","1 timothy","2 timothy",
        "1 peter","2 peter","1 john","2 john","3 john","genesis","exodus","leviticus","numbers","deuteronomy",
        "joshua","judges","ruth","ezra","nehemiah","esther","job","psalms","psalm","proverbs","ecclesiastes",
        "isaiah","jeremiah","lamentations","ezekiel","daniel","hosea","joel","amos","obadiah","jonah","micah",
        "nahum","habakkuk","zephaniah","haggai","zechariah","malachi","matthew","mark","luke","john","acts",
        "romans","galatians","ephesians","philippians","colossians","titus","philemon","hebrews","james","jude","revelation"
    )

    private fun spokenNumber(tokens: List<String>): Int? {
        if (tokens.isEmpty()) return null
        tokens.firstOrNull()?.toIntOrNull()?.let { return it }
        var total = 0; var found = false
        tokens.forEach { token ->
            token.toIntOrNull()?.let { total += it; found = true } ?: numberWords[token]?.let { total += it; found = true }
        }
        return if (found) total else null
    }

    private fun normalize(raw: String): String {
        var s = raw.lowercase().replace(Regex("[^a-z0-9: -]"), " ").replace(Regex("\\s+"), " ").trim()
        ordinals.forEach { (word, digit) -> s = s.replace(Regex("\\b$word\\b"), digit) }
        return s
    }

    fun parse(transcript: String): BibleReference? {
        val clean = normalize(transcript)
        // Search every position for a known Bible book, longest names first. This lets a preacher say
        // “please turn with me to John chapter three verse sixteen” instead of only the bare reference.
        for (book in books.sortedByDescending { it.length }) {
            val regex = Regex("\\b${Regex.escape(book)}\\b")
            for (match in regex.findAll(clean)) {
                val tail = clean.substring(match.range.first).trim()
                parseFromBook(tail)?.let { return it }
            }
        }
        // Also handle abbreviated references embedded in a sentence by trying short suffix windows.
        val words = clean.split(" ")
        for (start in words.indices) {
            val candidate = words.drop(start).take(8).joinToString(" ")
            BibleReferenceParser.parse(candidate)?.let { return it }
        }
        return null
    }

    private fun parseFromBook(text: String): BibleReference? {
        BibleReferenceParser.parse(text)?.let { return it }
        val tokens = text.split(" ")
        val chapterIndex = tokens.indexOfFirst { it == "chapter" }
        val verseIndex = tokens.indexOfFirst { it == "verse" || it == "verses" }
        if (chapterIndex <= 0 || verseIndex <= chapterIndex) return null
        val bookText = tokens.subList(0, chapterIndex).joinToString(" ")
        val chapter = spokenNumber(tokens.subList(chapterIndex + 1, verseIndex)) ?: return null
        val after = tokens.drop(verseIndex + 1)
        val endAt = after.indexOfFirst { it == "to" || it == "through" || it == "and" || it == "then" || it == "where" || it == "which" || it == "says" || it == "reads" }
        val firstTokens = if (endAt >= 0) after.take(endAt) else after.take(3)
        val verse = spokenNumber(firstTokens) ?: return null
        val separator = after.indexOfFirst { it == "to" || it == "through" }
        val verseEnd = if (separator >= 0) spokenNumber(after.drop(separator + 1).take(3)) else null
        return BibleReferenceParser.parse("$bookText $chapter $verse${verseEnd?.let { "-$it" } ?: ""}")
    }
}
