package com.kingzulu.biblepresentation

import android.content.Context
import org.json.JSONObject

data class Verse(val reference: BibleReference,val text:String,val translation:String)
private data class IndexedVerse(val verse:Verse,val normalized:String,val words:Set<String>)
object OfflineBibleRepository{
 private var appContext:Context?=null;private var bible:JSONObject?=null;private var verseIndex:List<IndexedVerse>?=null
 fun initialize(context:Context){appContext=context.applicationContext}
 private fun data():JSONObject?{bible?.let{return it};val c=appContext?:return null;return runCatching{JSONObject(c.assets.open("bibles/kjv.json").bufferedReader().use{it.readText()}).also{bible=it}}.getOrNull()}
 private fun normalizeBookName(v:String)=v.lowercase().replace(Regex("[^a-z0-9]"),"").replace("psalm","psalms")
 private fun normalizeText(v:String)=v.lowercase().replace(Regex("[^a-z0-9 ]")," ").replace(Regex("\\s+")," ").trim()
 fun get(r:BibleReference,translation:String="KJV"):Verse?{if(!translation.equals("KJV",true)||r.verseStart==null)return null;val books=data()?.optJSONArray("books")?:return null;val wanted=normalizeBookName(r.book);var book:JSONObject?=null;for(i in 0 until books.length()){val c=books.optJSONObject(i)?:continue;if(normalizeBookName(c.optString("englishName"))==wanted||normalizeBookName(c.optString("book"))==wanted){book=c;break}};val chapters=book?.optJSONArray("chapters")?:return null;val chapter=(0 until chapters.length()).mapNotNull{chapters.optJSONObject(it)}.firstOrNull{it.optInt("chapter")==r.chapter}?:return null;val verses=chapter.optJSONArray("verses")?:return null;val start=r.verseStart;val end=r.verseEnd?:start;if(end<start)return null;val texts=mutableListOf<String>();for(n in start..end){val v=(0 until verses.length()).mapNotNull{verses.optJSONObject(it)}.firstOrNull{it.optInt("number",it.optInt("verse"))==n}?:return null;val t=v.optString("text").trim();if(t.isBlank())return null;texts+=t};return Verse(r,texts.joinToString(" "),"KJV")}
 private fun index():List<IndexedVerse>{verseIndex?.let{return it};val out=ArrayList<IndexedVerse>(31102);val books=data()?.optJSONArray("books")?:return emptyList();for(bi in 0 until books.length()){val b=books.optJSONObject(bi)?:continue;val name=b.optString("englishName");val chapters=b.optJSONArray("chapters")?:continue;for(ci in 0 until chapters.length()){val c=chapters.optJSONObject(ci)?:continue;val cn=c.optInt("chapter");val verses=c.optJSONArray("verses")?:continue;for(vi in 0 until verses.length()){val v=verses.optJSONObject(vi)?:continue;val text=v.optString("text").trim();if(text.isBlank())continue;val verse=Verse(BibleReference(name,cn,v.optInt("number",v.optInt("verse"))),text,"KJV");val n=normalizeText(text);out+=IndexedVerse(verse,n,n.split(" ").toSet())}}};return out.also{verseIndex=it}}
 fun searchQuote(spoken:String):Verse?{
  var cleaned=normalizeText(spoken)
  val commandPrefixes=listOf("media please help me find this reference where it says","please help me find this reference where it says","help me find the reference where it says","find the reference where it says","where does the bible say","where does scripture say","the bible says","scripture says","where it says","it says")
  for(p in commandPrefixes){val i=cleaned.indexOf(p);if(i>=0){cleaned=cleaned.substring(i+p.length).trim();break}}
  val filler=setOf("the","a","an","and","but","or","so","that","this","these","those","is","are","was","were","be","been","being","to","of","for","in","on","at","with","from","as","it","its","i","you","we","they","he","she","my","your","our","their","his","her","me","us","them","says","said","say","bible","scripture","verse","reference","book","open","turn","please","media","help","find","where")
  val qWords=cleaned.split(" ").filter{it.length>1&&!filler.contains(it)};if(qWords.size<3)return null
  val phrase=qWords.joinToString(" ");var best:Verse?=null;var bestScore=0.0
  for(iv in index()){
   val hits=qWords.count{it in iv.words};if(hits<2)continue
   var score=hits.toDouble()/qWords.size
   if(phrase.length>=8&&iv.normalized.contains(phrase))score+=1.0
   val pairs=qWords.windowed(2).count{iv.normalized.contains(it.joinToString(" "))};score+=pairs*0.14
   val triples=qWords.windowed(3).count{iv.normalized.contains(it.joinToString(" "))};score+=triples*0.22
   if(score>bestScore){bestScore=score;best=iv.verse}
  }
  return if(bestScore>=0.62)best else null
 }
}
