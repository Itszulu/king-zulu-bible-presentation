package com.kingzulu.biblepresentation

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/** Installs authorised Bible modules and normalizes common JSON schemas into King Zulu's internal format. */
object BibleTranslationManager {
    data class InstallResult(val success:Boolean,val abbreviation:String?=null,val message:String)
    private val safeAbbreviation=Regex("[A-Z0-9_-]{2,16}");private const val MAX_MODULE_BYTES=64*1024*1024

    fun install(context:Context,bytes:ByteArray):InstallResult{
        if(bytes.isEmpty())return InstallResult(false,message="Bible module is empty")
        if(bytes.size>MAX_MODULE_BYTES)return InstallResult(false,message="Bible module is too large")
        val source=runCatching{JSONObject(bytes.toString(Charsets.UTF_8))}.getOrNull()?:return InstallResult(false,message="Invalid Bible JSON")
        val normalized=normalize(source)?:return InstallResult(false,message="Bible format was not recognised — no readable books, chapters and verses were found")
        val validation=validate(normalized);if(validation!=null)return InstallResult(false,message=validation)
        val meta=normalized.getJSONObject("meta");val abbreviation=meta.getString("abbreviation").trim().uppercase()
        if(!safeAbbreviation.matches(abbreviation))return InstallResult(false,message="Translation abbreviation is invalid")
        if(abbreviation=="KJV")return InstallResult(false,message="Built-in KJV cannot be replaced")
        val dir=OfflineBibleRepository.moduleDirectory()?:File(context.filesDir,"bibles").apply{mkdirs()};if(!dir.exists()&&!dir.mkdirs())return InstallResult(false,abbreviation,"Unable to open offline Bible library")
        val target=File(dir,"$abbreviation.json");val part=File(dir,"$abbreviation.part");val backup=File(dir,"$abbreviation.backup");part.delete();backup.delete()
        return runCatching{
            FileOutputStream(part).use{out->out.write(normalized.toString().toByteArray(Charsets.UTF_8));out.fd.sync()}
            val written=JSONObject(part.readText());validate(written)?.let{error(it)}
            var backedUp=false;if(target.exists()){if(!target.renameTo(backup))error("Unable to protect existing Bible module");backedUp=true}
            try{if(!part.renameTo(target))error("Unable to activate Bible module");backup.delete()}catch(e:Throwable){target.delete();if(backedUp&&!backup.renameTo(target))throw IllegalStateException("Install failed and previous module could not be restored",e);throw e}
            OfflineBibleRepository.refreshModules();val stats=stats(normalized);InstallResult(true,abbreviation,"$abbreviation installed — ${stats.first} books, ${stats.second} chapters, ${stats.third} verses")
        }.getOrElse{part.delete();if(!target.exists()&&backup.exists())backup.renameTo(target);OfflineBibleRepository.refreshModules();InstallResult(false,abbreviation,"Install failed — the previous Bible was preserved")}
    }

    fun remove(abbreviation:String):Boolean{val key=abbreviation.trim().uppercase();if(key=="KJV"||!safeAbbreviation.matches(key))return false;val file=OfflineBibleRepository.moduleDirectory()?.let{File(it,"$key.json")}?:return false;val removed=!file.exists()||file.delete();if(removed){if(OfflineBibleRepository.selectedTranslation().equals(key,true))OfflineBibleRepository.selectTranslation("KJV");OfflineBibleRepository.refreshModules()};return removed}

    /** Accept native King Zulu modules plus common {translation, books:[{name,chapters:[{chapter,verses:{"1":"text"}}]}]} files. */
    private fun normalize(source:JSONObject):JSONObject?{
        val sourceBooks=source.optJSONArray("books")?:return null
        val translation=source.optString("translation").trim()
        val sourceMeta=source.optJSONObject("meta")
        val name=sourceMeta?.optString("name")?.takeIf{it.isNotBlank()}?:translation.takeIf{it.isNotBlank()}?:source.optString("name").takeIf{it.isNotBlank()}?:"Imported Bible"
        val abbreviation=(sourceMeta?.optString("abbreviation")?.takeIf{it.isNotBlank()}?:deriveAbbreviation(name)).uppercase()
        val books=JSONArray();var readable=0
        for(bi in 0 until sourceBooks.length()){
            val b=sourceBooks.optJSONObject(bi)?:continue;val bookName=listOf("englishName","name","book").firstNotNullOfOrNull{k->b.optString(k).takeIf{it.isNotBlank()}}?:continue
            val sourceChapters=b.optJSONArray("chapters")?:continue;val chapters=JSONArray()
            for(ci in 0 until sourceChapters.length()){
                val c=sourceChapters.optJSONObject(ci)?:continue;val chapterNumber=c.optInt("chapter",c.optInt("number",ci+1));if(chapterNumber<1)continue
                val normalizedVerses=JSONArray();val raw=c.opt("verses")
                when(raw){
                    is JSONArray->for(vi in 0 until raw.length()){val item=raw.opt(vi);when(item){is JSONObject->{val n=item.optInt("number",item.optInt("verse",vi+1));val text=item.optString("text").trim();if(n>0&&text.isNotBlank()){normalizedVerses.put(JSONObject().put("number",n).put("text",text));readable++}};is String->{if(item.isNotBlank()){normalizedVerses.put(JSONObject().put("number",vi+1).put("text",item.trim()));readable++}}}}
                    is JSONObject->{val keys=raw.keys().asSequence().toList().sortedBy{it.toIntOrNull()?:Int.MAX_VALUE};for(key in keys){val n=key.toIntOrNull()?:continue;val value=raw.opt(key);val text=when(value){is String->value;is JSONObject->value.optString("text");else->""}.trim();if(n>0&&text.isNotBlank()){normalizedVerses.put(JSONObject().put("number",n).put("text",text));readable++}}}
                }
                if(normalizedVerses.length()>0)chapters.put(JSONObject().put("chapter",chapterNumber).put("verses",normalizedVerses))
            }
            if(chapters.length()>0)books.put(JSONObject().put("englishName",bookName).put("book",bookName).put("chapters",chapters))
        }
        if(readable<1)return null
        val meta=JSONObject().put("id",sourceMeta?.optString("id")?.takeIf{it.isNotBlank()}?:abbreviation.lowercase()).put("name",name).put("abbreviation",abbreviation).put("source",sourceMeta?.optString("source")?.takeIf{it.isNotBlank()}?:"Imported JSON")
        source.optString("copyright").takeIf{it.isNotBlank()}?.let{meta.put("copyright",it)}
        return JSONObject().put("meta",meta).put("books",books)
    }
    private fun deriveAbbreviation(name:String):String{Regex("\\(([A-Za-z0-9_-]{2,16})\\)").find(name)?.groupValues?.get(1)?.let{return it};val words=name.replace(Regex("[^A-Za-z0-9 ]")," ").split(Regex("\\s+")).filter{it.isNotBlank()&&it.lowercase() !in setOf("the","version","translation","bible")};return words.mapNotNull{it.firstOrNull()?.uppercaseChar()?.toString()}.joinToString("").take(8).ifBlank{"BIBLE"}}
    private fun stats(json:JSONObject):Triple<Int,Int,Int>{val books=json.optJSONArray("books")?:return Triple(0,0,0);var chapters=0;var verses=0;for(bi in 0 until books.length()){val cs=books.optJSONObject(bi)?.optJSONArray("chapters")?:continue;chapters+=cs.length();for(ci in 0 until cs.length())verses+=cs.optJSONObject(ci)?.optJSONArray("verses")?.length()?:0};return Triple(books.length(),chapters,verses)}
    private fun validate(json:JSONObject):String?{val meta=json.optJSONObject("meta")?:return "Bible module metadata is missing";val abbreviation=meta.optString("abbreviation").trim().uppercase();if(abbreviation.isBlank())return "Translation abbreviation is missing";if(!safeAbbreviation.matches(abbreviation))return "Translation abbreviation is invalid";if(meta.optString("name").isBlank())return "Translation name is missing";val books=json.optJSONArray("books")?:return "Bible books are missing";if(books.length()<1)return "Bible module contains no books";val s=stats(json);if(s.third<1)return "Bible module contains no readable verses";return null}
}
