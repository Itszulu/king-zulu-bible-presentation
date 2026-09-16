package com.kingzulu.biblepresentation

enum class ServiceItemType { SCRIPTURE, SONG, PRESENTATION, COUNTDOWN, MESSAGE, MEDIA }

data class LyricSlide(val label: String, val text: String)

data class SongItem(
    val id: String,
    val title: String,
    val slides: List<LyricSlide>
)

data class CountdownItem(
    val title: String = "Service begins in",
    val seconds: Int
)

data class ServiceItem(
    val id: String,
    val title: String,
    val type: ServiceItemType,
    val subtitle: String = ""
)

object LyricSplitter {
    fun split(text: String, maxLines: Int = 4): List<LyricSlide> {
        val lines=text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if(lines.isEmpty()) return emptyList()
        return lines.chunked(maxLines.coerceIn(1,8)).mapIndexed { index, chunk ->
            LyricSlide("Slide ${index+1}", chunk.joinToString("\n"))
        }
    }
}
