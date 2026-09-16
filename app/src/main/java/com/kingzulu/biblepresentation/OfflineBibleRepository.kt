package com.kingzulu.biblepresentation

import android.content.Context
import org.json.JSONObject
import java.io.File

data class Verse(val reference: BibleReference,val text:String,val translation:String)
data class ScriptureMatch(val verse:Verse,val score:Double)
data class InstalledBible(val id:String,val name:String,val abbreviation:String,val source:String,val builtIn:Boolean=false)
private data class IndexedVerse(val verse:Verse,val normalized:String,val words:Set<String>)

/** Offline-first Bible engine. Built-in and downloaded translation modules use the same JSON format. */
object OfflineBibleRepository{
 private var appContext:Context?=null
 private val bibleCache=mutableMapOf<String,JSONObject>()
 private val indexCache=mutableMapOf<String,List<IndexedVerse>>()
 private var selected="KJV"
 fun initialize(context:Context){
  appContext=context.applicationContext
  val saved=context.getSharedPreferences("king_zulu_bibles",Context.MODE_PRIVATE).getString("selected","KJV")?:"KJV"
  selected=if(saved.equals("KJV",true)||installedBibles().any{it.abbreviation.equals(saved,true)})saved.uppercase() else "KJV"
  if(!selected.equals(saved,true))persistSelection(selected)
 }
 fun selectedTranslation():String=selected
 fun selectTranslation(abbreviation:String):Boolean{val key=abbreviation.uppercase();if(installedBibles().none{it.abbreviation.equals(key,true)})return false;if(data(key)==null)return false;selected=key;persistSelection(key);return true}
 private fun persistSelection(value:String){appContext?.getSharedPreferences("king_zulu_bibles",Context.MODE_PRIVATE)?.edit()?.putString("selected",value)?.apply()}
 fun installedBibles():List<InstalledBible>{val out=mutableListOf(InstalledBible("kjv","King James Version","KJV","Bundled",true));val dir=moduleDir()?:return out;dir.listFiles{f->f.isFile&&f.extension.equals("json",true)}?.forEach{f->readModule(f)?.let{j->val meta=j.optJSONObject("meta");val abbr=(meta?.optString("abbreviation")?.takeIf{it.isNotBlank()}?:f.nameWithoutExtension).uppercase();if(!abbr.equals("KJV",true))out+=InstalledBible(meta?.optString("id")?.takeIf{it.isNotBlank()}?:abbr.lowercase(),meta?.optString("name")?.takeIf{it.isNotBlank()}?:abbr,abbr,meta?.optString("source")?.takeIf{it.isNotBlank()}?:"Installed")}};return out.sortedBy{it.name}}
 fun isInstalled(abbreviation:String)=installedBibles().any{it.abbreviation.equals(abbreviation,true)}
 fun moduleDirectory():File?=moduleDir()
 fun refreshModules(){bibleCache.keys.filter{!it.equals("KJV",true)}.forEach{bibleCache.remove(it)};indexCache.clear();ensureSelectionAvailable()}
 private fun ensureSelectionAvailable(){if(!selected.equals("KJV",true)&&data(selected)==null){selected="KJV";persistSelection("KJV")}}
 private fun moduleDir():File?=appContext?.let{File(it.filesDir,"bibles").apply{mkdirs()}}
 private fun readModule(file:File)=runCatching{JSONObject(file.bufferedReader().use{it.readText()})}.getOrNull()
 private fun data(translation:String):JSONObject?{val key=translation.uppercase();bibleCache[key]?.let{return it};val c=appContext?:return null;val loaded=if(key=="KJV")runCatching{JSONObject(c.assets.open("bibles/kjv.json").bufferedReader().use{it.readText()})}.getOrNull() else moduleDir()?.listFiles()?.firstOrNull{it.isFile&&it.extension.equals("json",true)&&it.nameWithoutExtension.equals(key,true)}?.let(::readModule);return loaded?.also{bibleCache[key]=it}}
 private fun normalizeBookName(v:String)=v.lowercase().replace(Regex("[^a-z0-9]"),"").replace("psalm","psalms")
 private fun normalizeText(v:String)=v.lowercase().replace(Regex("[^a-z0-9 ]")," ").replace(Regex("\\s+")," ").trim()
 fun get(r:BibleReference,translation:String=selected):Verse?{if(r.verseStart==null)return null;val requested=translation.uppercase();val key=if(data(requested)!=null)requested else if(requested==selected&&requested!="KJV"){selected="KJV";persistSelection("KJV");"KJV"}else return null;val books=data(key)?.optJSONArray("books")?:return null;val wanted=normalizeBookName(r.book);var book:JSONObject?=null;for(i in 0 until books.length()){val c=books.optJSONObject(i)?:continue;if(normalizeBookName(c.optString("englishName"))==wanted||normalizeBookName(c.optString("book"))==wanted){book=c;break}};val chapters=book?.optJSONArray("chapters")?:return null;val chapter=(0 until chapters.length()).mapNotNull{chapters.optJSONObject(it)}.firstOrNull{it.optInt("chapter")==r.chapter}?:return null;val verses=chapter.optJSONArray("verses")?:return null;val start=r.verseStart;val end=r.verseEnd?:start;if(end<start)return null;val texts=mutableListOf<String>();for(n in start..end){val v=(0 until verses.length()).mapNotNull{verses.optJSONObject(it)}.firstOrNull{it.optInt("number",it.optInt("verse"))==n}?:return null;val t=v.optString("text").trim();if(t.isBlank())return null;texts+=t};return Verse(r,texts.joinToString(" "),key)}
 private fun index(translation:String):List<IndexedVerse>{val key=translation.uppercase();indexCache[key]?.let{return it};val out=ArrayList<IndexedVerse>(31102);val books=data(key)?.optJSONArray("books")?:return emptyList();for(bi in 0 until books.length()){val b=books.optJSONObject(bi)?:continue;val name=b.optString("englishName");val chapters=b.optJSONArray("chapters")?:continue;for(ci in 0 until chapters.length()){val c=chapters.optJSONObject(ci)?:continue;val cn=c.optInt("chapter");val verses=c.optJSONArray("verses")?:continue;for(vi in 0 until verses.length()){val v=verses.optJSONObject(vi)?:continue;val text=v.optString("text").trim();if(text.isBlank())continue;val verse=Verse(BibleReference(name,cn,v.optInt("number",v.optInt("verse"))),text,key);val n=normalizeText(text);out+=IndexedVerse(verse,n,n.split(" ").toSet())}}};return out.also{indexCache[key]=it}}
 private fun queryWords(spoken:String):List<String>{var cleaned=normalizeText(spoken);val prefixes=listOf("media please help me find this reference where it says","please help me find this reference where it says","help me find the reference where it says","find the reference where it says","where does the bible say","where does scripture say","the bible says","scripture says","where it says","it says");for(p in prefixes){val i=cleaned.indexOf(p);if(i>=0){cleaned=cleaned.substring(i+p.length).trim();break}};val filler=setOf("the","a","an","and","but","or","so","that","this","these","those","is","are","was","were","be","been","being","to","of","for","in","on","at","with","from","as","it","its","i","you","we","they","he","she","my","your","our","their","his","her","me","us","them","says","said","say","bible","scripture","verse","reference","book","open","turn","please","media","help","find","where");return cleaned.split(" ").filter{it.length>1&&!filler.contains(it)}}
 fun searchQuoteMatches(spoken:String,limit:Int=5,translation:String=selected):List<ScriptureMatch>{val qWords=queryWords(spoken);if(qWords.size<2)return emptyList();val phrase=qWords.joinToString(" ");val matches=ArrayList<ScriptureMatch>();for(iv in index(translation)){val hits=qWords.count{it in iv.words};if(hits<2)continue;var score=hits.toDouble()/qWords.size;if(phrase.length>=6&&iv.normalized.contains(phrase))score+=1.0;score+=qWords.windowed(2).count{iv.normalized.contains(it.joinToString(" "))}*0.14;score+=qWords.windowed(3).count{iv.normalized.contains(it.joinToString(" "))}*0.22;if(score>=0.42)matches+=ScriptureMatch(iv.verse,score)};return matches.sortedByDescending{it.score}.take(limit)}
 fun searchQuote(spoken:String,translation:String=selected):Verse?=searchQuoteMatches(spoken,1,translation).firstOrNull()?.takeIf{it.score>=0.62}?.verse
}
