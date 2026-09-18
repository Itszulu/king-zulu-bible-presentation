package com.kingzulu.biblepresentation

enum class ScriptureSpeechIntent { DIRECT_READ, LISTING, NAVIGATION, MENTION, RANGE_READ, UNKNOWN }
data class ScriptureSpeechDecision(val intent:ScriptureSpeechIntent,val activate:Verse?=null,val queue:List<Verse> = emptyList(),val confidence:Double=0.0)
data class ScriptureHistoryEntry(val verse:Verse,val usedAt:Long=System.currentTimeMillis())

object AiSermonContext {
 @Volatile private var active:Verse?=null
 private val mentioned=ArrayDeque<Verse>();private val queued=mutableListOf<Verse>();private val history=mutableListOf<ScriptureHistoryEntry>()
 private val numberWords=mapOf("one" to 1,"two" to 2,"three" to 3,"four" to 4,"five" to 5,"six" to 6,"seven" to 7,"eight" to 8,"nine" to 9,"ten" to 10,"eleven" to 11,"twelve" to 12,"thirteen" to 13,"fourteen" to 14,"fifteen" to 15,"sixteen" to 16,"seventeen" to 17,"eighteen" to 18,"nineteen" to 19,"twenty" to 20,"thirty" to 30,"forty" to 40,"fifty" to 50,"sixty" to 60,"seventy" to 70,"eighty" to 80,"ninety" to 90)
 fun activeVerse():Verse?=active
 fun queue():List<Verse> = synchronized(queued){queued.toList()}
 fun history():List<ScriptureHistoryEntry> = synchronized(history){history.toList()}
 fun clearSession(){active=null;synchronized(queued){queued.clear()};synchronized(mentioned){mentioned.clear()};synchronized(history){history.clear()};ScriptureReadingSession.clear()}
 fun decide(alternatives:List<String>):ScriptureSpeechDecision{
  for(raw in alternatives){
   val text=normalize(raw)
   resolveHistoryCommand(text)?.let{return activate(it,ScriptureSpeechIntent.NAVIGATION,.98)}
   if(isExplicitNavigation(text)){
    ScriptureReadingSession.currentState()?.let{
     contextualVerseNumber(text)?.let{n->ScriptureReadingSession.goTo(n)?.let{return activate(it,ScriptureSpeechIntent.NAVIGATION,.99)}}
     if(Regex("\\b(next verse|continue|continue reading|continue to the next verse|go forward one verse)\\b").containsMatchIn(text))ScriptureReadingSession.next()?.let{return activate(it,ScriptureSpeechIntent.NAVIGATION,.99)}
     if(Regex("\\b(previous verse|go back|go back one verse|take me back one verse)\\b").containsMatchIn(text))ScriptureReadingSession.previous()?.let{return activate(it,ScriptureSpeechIntent.NAVIGATION,.99)}
    }
    AiScriptureEngine.resolveSpeech(listOf(raw))?.let{return activate(it,ScriptureSpeechIntent.NAVIGATION,.99)}
   }
   val parsed=SpokenBibleReferenceParser.parse(raw)
   if(parsed?.verseStart!=null&&parsed.verseEnd!=null&&parsed.verseEnd>parsed.verseStart){val session=ScriptureReadingSession.start(parsed)?:continue;retireQueueForNewReading(session.current);session.current?.let{return activate(it,ScriptureSpeechIntent.RANGE_READ,.99)}}
   val refs=extractReferences(raw);if(refs.isEmpty()){ScriptureReadingSession.followSpeech(raw)?.let{return activate(it,ScriptureSpeechIntent.RANGE_READ,.94)};continue};refs.forEach(::rememberMention)
   if(refs.size>1||isListLanguage(text)){val ordered=reorderByIntent(text,refs);synchronized(queued){queued.clear();queued.addAll(ordered)};resolveQueueSelection(text)?.let{return activate(it,ScriptureSpeechIntent.DIRECT_READ,.96)};return ScriptureSpeechDecision(ScriptureSpeechIntent.LISTING,queue=ordered,confidence=.96)}
   val only=refs.first();if(isDirectRead(text)){retireQueueForNewReading(only);ScriptureReadingSession.clear();return activate(only,ScriptureSpeechIntent.DIRECT_READ,.98)}
   if(isMentionOnly(text))return ScriptureSpeechDecision(ScriptureSpeechIntent.MENTION,queue=queue(),confidence=.90)
   if(looksLikeStandaloneReference(text)){retireQueueForNewReading(only);ScriptureReadingSession.clear();return activate(only,ScriptureSpeechIntent.DIRECT_READ,.94)}
   return ScriptureSpeechDecision(ScriptureSpeechIntent.MENTION,queue=queue(),confidence=.72)
  }
  alternatives.firstNotNullOfOrNull{resolveQueueSelection(normalize(it))}?.let{return activate(it,ScriptureSpeechIntent.DIRECT_READ,.95)}
  alternatives.firstNotNullOfOrNull{ScriptureReadingSession.followSpeech(it)}?.let{return activate(it,ScriptureSpeechIntent.RANGE_READ,.94)}
  return ScriptureSpeechDecision(ScriptureSpeechIntent.UNKNOWN)
 }
 private fun contextualVerseNumber(t:String):Int?{Regex("\\b(?:go|jump|take me|back)?\\s*(?:back )?(?:to )?verse ([0-9]{1,3})\\b").find(t)?.groupValues?.get(1)?.toIntOrNull()?.let{return it};val m=Regex("\\b(?:go|jump|take me|back)?\\s*(?:back )?(?:to )?verse ((?:[a-z]+ ?){1,4})").find(t)?:return null;val ws=m.groupValues[1].trim().split(" ");var total=0;var current=0;for(w in ws){if(w=="and")continue;val n=numberWords[w]?:break;if(n>=20&&n%10==0)current+=n else current+=n};total+=current;return total.takeIf{it in 1..176}}
 private fun reorderByIntent(t:String,refs:List<Verse>):List<Verse>{if(refs.size<2)return refs;if(t.contains("before that")||t.contains("but first")||t.contains("first give me")||t.contains("first lets read")){val direct=refs.indexOfLast{v->val b=v.reference.book.lowercase();val p=t.lastIndexOf(b);p>=0&&t.substring(maxOf(0,p-24),p).let{s->s.contains("before that")||s.contains("first")||s.contains("lets read")||s.contains("give me")}};if(direct>0)return listOf(refs[direct])+refs.filterIndexed{i,_->i!=direct}};return refs}
 private fun resolveHistoryCommand(t:String):Verse?{if(!(t.contains("go back to")||t.contains("back to")||t.contains("return to")||t.contains("give me that")||t.contains("put the last verse back")))return null;val h=history();if(h.isEmpty())return null;if(t.contains("last verse")||t.contains("previous scripture"))return h.asReversed().drop(1).firstOrNull()?.verse?:h.last().verse;val books=OfflineBibleRepository.books().sortedByDescending{it.length};val named=books.firstOrNull{Regex("\\b${Regex.escape(it.lowercase())}\\b").containsMatchIn(t)};if(named!=null){val matches=h.filter{it.verse.reference.book.equals(named,true)};if(matches.isEmpty())return null;if(t.contains("first"))return matches.first().verse;return matches.last().verse};return null}
 private fun retireQueueForNewReading(v:Verse?){if(v==null)return;synchronized(queued){if(queued.none{sameRef(it,v)})queued.clear()}}
 private fun activate(v:Verse,intent:ScriptureSpeechIntent,confidence:Double):ScriptureSpeechDecision{active=v;AiScriptureEngine.remember(v);rememberHistory(v);synchronized(queued){queued.removeAll{sameRef(it,v)}};return ScriptureSpeechDecision(intent,activate=v,queue=queue(),confidence=confidence)}
 private fun rememberHistory(v:Verse){synchronized(history){if(history.lastOrNull()?.verse?.let{sameRef(it,v)}!=true)history+=ScriptureHistoryEntry(v);if(history.size>250)history.removeAt(0)}}
 private fun sameRef(a:Verse,b:Verse)=a.translation.equals(b.translation,true)&&a.reference.display()==b.reference.display()
 private fun extractReferences(raw:String):List<Verse>{val pieces=raw.replace(";",",").split(Regex(",|\\band\\b|\\bthen\\b"),limit=12);val found=mutableListOf<Verse>();pieces.forEach{part->SpokenBibleReferenceParser.parse(part)?.let{OfflineBibleRepository.get(it)}?.let{v->if(found.none{sameRef(it,v)})found+=v}};if(found.isEmpty())SpokenBibleReferenceParser.parse(raw)?.let{OfflineBibleRepository.get(it)}?.let(found::add);return found}
 private fun resolveQueueSelection(t:String):Verse?{val q=queue();if(q.isEmpty())return null;val ordinal=mapOf("first" to 0,"second" to 1,"third" to 2,"fourth" to 3,"last" to q.lastIndex);ordinal.entries.firstOrNull{(word,_)->Regex("\\b(read|take|go to|look at|lets read|let us read)?\\s*(the )?$word( one| scripture| passage)?\\b").containsMatchIn(t)}?.let{return q.getOrNull(it.value)};q.firstOrNull{v->t.contains(v.reference.book.lowercase())&&(t.contains("read")||t.contains("take")||t.contains("look")||t.contains("go to"))}?.let{return it};return null}
 private fun rememberMention(v:Verse){synchronized(mentioned){mentioned.addLast(v);while(mentioned.size>24)mentioned.removeFirst()}}
 private fun normalize(s:String)=s.lowercase().replace("let's","lets").replace(Regex("[^a-z0-9 ]")," ").replace(Regex("\\s+")," ").trim()
 private fun isExplicitNavigation(t:String)=listOf("next verse","previous verse","repeat that","repeat verse","next chapter","previous chapter","go back","continue","jump to verse","take me to verse","go to verse","back to verse","verse one","verse two","verse three","verse four","verse five","verse six","verse seven","verse eight","verse nine").any(t::contains)||Regex("\\b(?:go|jump|take me) (?:back )?(?:to )?verse \\d+\\b").containsMatchIn(t)||Regex("\\bverse (?:one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)\\b").containsMatchIn(t)
 private fun isListLanguage(t:String)=listOf("you can find this in","you can find these in","write down","note these scriptures","these scriptures","our texts are","references are","we will read","we'll read").any(t::contains)
 private fun isDirectRead(t:String)=listOf("lets read","let us read","turn to","open to","go to","give me","put up","show us","read from","we are reading").any(t::contains)
 private fun isMentionOnly(t:String)=listOf("people quote","not where","not going to","mentions","mentioned","for example","such as").any(t::contains)
 private fun looksLikeStandaloneReference(t:String)=t.split(" ").size<=8
}
