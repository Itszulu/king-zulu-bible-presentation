package com.kingzulu.biblepresentation

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

data class BackgroundTheme(val id:String,val localPath:String?,val name:String,val overlay:Float=.38f)
object LocalBackgroundStore{
    private const val PREFS="king_zulu_presentation"
    fun importFromGallery(context:Context,uri:Uri):BackgroundTheme?=try{
        val dir=File(context.filesDir,"presentation_backgrounds").apply{mkdirs()};val f=File(dir,"${UUID.randomUUID()}.img")
        context.contentResolver.openInputStream(uri)?.use{input->f.outputStream().use{input.copyTo(it)}}?:return null
        BackgroundTheme(f.name,f.absolutePath,"My Background").also{saveSelected(context,it)}
    }catch(_:Exception){null}
    fun saveSelected(context:Context,theme:BackgroundTheme?){
        val e=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit()
        if(theme==null)e.remove("background_path").remove("background_id").remove("background_name").remove("background_overlay")
        else e.putString("background_path",theme.localPath).putString("background_id",theme.id).putString("background_name",theme.name).putFloat("background_overlay",theme.overlay)
        e.apply()
    }
    fun loadSelected(context:Context):BackgroundTheme?{
        val p=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);val path=p.getString("background_path",null)?:return null
        if(!File(path).exists())return null
        return BackgroundTheme(p.getString("background_id",File(path).name)?:File(path).name,path,p.getString("background_name","My Background")?:"My Background",p.getFloat("background_overlay",.38f))
    }
}
