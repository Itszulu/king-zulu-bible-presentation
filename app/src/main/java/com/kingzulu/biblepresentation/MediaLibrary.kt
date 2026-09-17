package com.kingzulu.biblepresentation

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

enum class MediaKind { IMAGE, VIDEO }
data class SavedMedia(val id:String,val name:String,val kind:MediaKind,val path:String,val updatedAt:Long=System.currentTimeMillis())

/** Private on-device reusable media library. Imported files survive Gallery permission/lifecycle changes. */
object MediaLibrary {
    private const val PREFS="king_zulu_media_library"
    private const val KEY="items"

    fun all(context:Context):List<SavedMedia>{
        val raw=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY,"[]")?:"[]"
        return runCatching{val a=JSONArray(raw);buildList{for(i in 0 until a.length()){val o=a.getJSONObject(i);val item=SavedMedia(o.getString("id"),o.getString("name"),MediaKind.valueOf(o.getString("kind")),o.getString("path"),o.optLong("updatedAt",0));if(File(item.path).exists())add(item)}}}.getOrDefault(emptyList()).sortedByDescending{it.updatedAt}
    }

    fun import(context:Context,uri:Uri,kind:MediaKind,name:String?=null):SavedMedia?=runCatching{
        val dir=File(context.filesDir,"media").apply{mkdirs()};val ext=if(kind==MediaKind.VIDEO)"mp4" else "img";val file=File(dir,"${UUID.randomUUID()}.$ext")
        context.contentResolver.openInputStream(uri)!!.use{input->file.outputStream().use{output->input.copyTo(output,128*1024)}}
        val item=SavedMedia(UUID.randomUUID().toString(),name?.trim().takeUnless{it.isNullOrBlank()}?:if(kind==MediaKind.VIDEO)"Video" else "Image",kind,file.absolutePath)
        persist(context,listOf(item)+all(context));item
    }.getOrNull()

    fun rename(context:Context,id:String,name:String){persist(context,all(context).map{if(it.id==id)it.copy(name=name.trim().ifBlank{it.name},updatedAt=System.currentTimeMillis())else it})}
    fun delete(context:Context,id:String){val all=all(context);all.firstOrNull{it.id==id}?.let{runCatching{File(it.path).delete()}};persist(context,all.filterNot{it.id==id})}
    fun search(context:Context,q:String):List<SavedMedia>{val x=q.trim().lowercase();return if(x.isBlank())all(context)else all(context).filter{it.name.lowercase().contains(x)}}

    private fun persist(context:Context,items:List<SavedMedia>){val a=JSONArray();items.distinctBy{it.id}.forEach{i->a.put(JSONObject().apply{put("id",i.id);put("name",i.name);put("kind",i.kind.name);put("path",i.path);put("updatedAt",i.updatedAt)})};context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(KEY,a.toString()).apply()}
}
