package com.kingzulu.biblepresentation

data class SavedSong(
    val id: String,
    val title: String,
    val lyrics: String,
    val slides: List<LyricSlide>
)

class SongLibrary {
    private val songs = mutableListOf<SavedSong>()
    fun all(): List<SavedSong> = songs.toList()
    fun save(song: SavedSong) {
        songs.removeAll { it.id == song.id }
        songs += song
    }
    fun search(query: String): List<SavedSong> {
        val q=query.trim().lowercase()
        if(q.isBlank()) return all()
        return songs.filter { it.title.lowercase().contains(q) || it.lyrics.lowercase().contains(q) }
    }
}
