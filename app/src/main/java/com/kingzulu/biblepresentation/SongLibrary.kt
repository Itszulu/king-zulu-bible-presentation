package com.kingzulu.biblepresentation

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class SavedSong(val id:String,val title:String,val lyrics:String)

class SongLibrary(private val context:Context?=null){
    private val songs=mutableListOf<SavedSong>()
    init{load()}
    fun all():List<SavedSong> = songs.toList()
    fun save(song:SavedSong){songs.removeAll{it.id==song.id};songs.add(0,song);persist()}
    fun delete(id:String){songs.removeAll{it.id==id};persist()}
    fun search(query:String):List<SavedSong>{val q=query.trim().lowercase();if(q.isBlank())return all();return songs.filter{it.title.lowercase().contains(q)||it.lyrics.lowercase().contains(q)}}
    private fun persist(){val c=context?:return;val array=JSONArray();songs.forEach{s->array.put(JSONObject().apply{put("id",s.id);put("title",s.title);put("lyrics",s.lyrics)})};c.getSharedPreferences("king_zulu_songs",Context.MODE_PRIVATE).edit().putString("library",array.toString()).apply()}
    private fun load(){val c=context?:return;val raw=c.getSharedPreferences("king_zulu_songs",Context.MODE_PRIVATE).getString("library","[]")?:"[]";runCatching{val a=JSONArray(raw);for(i in 0 until a.length()){val o=a.getJSONObject(i);songs+=SavedSong(o.optString("id"),o.optString("title"),o.optString("lyrics"))}}}
}
