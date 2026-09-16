package com.kingzulu.biblepresentation

data class BibleReference(val book: String, val chapter: Int, val verseStart: Int? = null, val verseEnd: Int? = null) {
    fun display() = buildString { append("$book $chapter"); verseStart?.let { append(":$it"); if (verseEnd != null && verseEnd != it) append("–$verseEnd") } }
}

object BibleReferenceParser {
    private val aliases = mutableMapOf<String, String>().apply {
        fun add(book: String, vararg names: String) { (listOf(book) + names).forEach { put(it.lowercase(), book) } }
        add("Genesis", "gen", "ge", "gn"); add("Exodus", "ex", "exo"); add("Leviticus", "lev", "lv"); add("Numbers", "num", "nu", "nm"); add("Deuteronomy", "deut", "dt")
        add("Joshua", "josh", "jos"); add("Judges", "judg", "jdg"); add("Ruth", "ru"); add("1 Samuel", "1 sam", "1sam", "1 sa"); add("2 Samuel", "2 sam", "2sam", "2 sa")
        add("1 Kings", "1 kgs", "1kgs", "1 ki"); add("2 Kings", "2 kgs", "2kgs", "2 ki"); add("1 Chronicles", "1 chr", "1chr", "1 ch"); add("2 Chronicles", "2 chr", "2chr", "2 ch")
        add("Ezra", "ezr"); add("Nehemiah", "neh", "ne"); add("Esther", "est", "esth"); add("Job", "jb"); add("Psalms", "psalm", "ps", "psa"); add("Proverbs", "prov", "pr", "prv")
        add("Ecclesiastes", "eccl", "ecc", "ec"); add("Song of Solomon", "song", "songs", "sos", "song of songs"); add("Isaiah", "isa", "is"); add("Jeremiah", "jer", "je"); add("Lamentations", "lam", "la")
        add("Ezekiel", "ezek", "eze"); add("Daniel", "dan", "dn"); add("Hosea", "hos", "ho"); add("Joel", "jl"); add("Amos", "am"); add("Obadiah", "obad", "ob"); add("Jonah", "jon"); add("Micah", "mic", "mi")
        add("Nahum", "nah", "na"); add("Habakkuk", "hab"); add("Zephaniah", "zeph", "zep"); add("Haggai", "hag"); add("Zechariah", "zech", "zec"); add("Malachi", "mal")
        add("Matthew", "matt", "mt"); add("Mark", "mk", "mrk"); add("Luke", "lk", "luk"); add("John", "jn", "jhn"); add("Acts", "act", "ac"); add("Romans", "rom", "ro")
        add("1 Corinthians", "1 cor", "1cor", "1 co"); add("2 Corinthians", "2 cor", "2cor", "2 co"); add("Galatians", "gal", "ga"); add("Ephesians", "eph", "ep"); add("Philippians", "phil", "php")
        add("Colossians", "col"); add("1 Thessalonians", "1 thess", "1thess", "1 thes", "1 th"); add("2 Thessalonians", "2 thess", "2thess", "2 thes", "2 th")
        add("1 Timothy", "1 tim", "1tim", "1 ti"); add("2 Timothy", "2 tim", "2tim", "2 ti"); add("Titus", "tit"); add("Philemon", "philem", "phm"); add("Hebrews", "heb")
        add("James", "jas", "jam"); add("1 Peter", "1 pet", "1pet", "1 pe"); add("2 Peter", "2 pet", "2pet", "2 pe"); add("1 John", "1 jn", "1jn", "1 john"); add("2 John", "2 jn", "2jn", "2 john"); add("3 John", "3 jn", "3jn", "3 john")
        add("Jude", "jud"); add("Revelation", "rev", "re", "revelations")
    }

    fun parse(input: String): BibleReference? {
        var s = input.lowercase().trim()
            .replace(Regex("\\bchapter\\b"), " ")
            .replace(Regex("\\bverses?\\b"), " ")
            .replace(":", " ")
            .replace(Regex("([a-z])(\\d)"), "$1 $2")
            .replace(Regex("(\\d)([a-z])"), "$1 $2")
            .replace(Regex("\\s+"), " ")
            .trim()
        val match = Regex("^(.+?)\\s+(\\d+)(?:\\s+(\\d+)(?:[-–](\\d+))?)?$", RegexOption.IGNORE_CASE).matchEntire(s) ?: return null
        val rawBook = match.groupValues[1].trim().replace(Regex("\\s+"), " ")
        val book = aliases[rawBook] ?: return null
        val chapter = match.groupValues[2].toIntOrNull() ?: return null
        val verseStart = match.groupValues[3].takeIf { it.isNotBlank() }?.toIntOrNull()
        val verseEnd = match.groupValues[4].takeIf { it.isNotBlank() }?.toIntOrNull()
        return BibleReference(book, chapter, verseStart, verseEnd)
    }
}
