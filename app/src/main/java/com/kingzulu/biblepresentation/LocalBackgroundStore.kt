package com.kingzulu.biblepresentation

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

data class BackgroundTheme(val id:String,val localPath:String?,val name:String,val overlay:Float=.38f)
object LocalBackgroundStore{
    fun importFromGallery(context:Context,uri:Uri):BackgroundTheme?=try{
        val dir=File(context.filesDir,"presentation_backgrounds").apply{mkdirs()};val f=File(dir,"${UUID.randomUUID()}.img")
        context.contentResolver.openInputStream(uri)?.use{input->f.outputStream().use{input.copyTo(it)}}?:return null
        BackgroundTheme(f.name,f.absolutePath,"My Background")
    }catch(_:Exception){null}
}
