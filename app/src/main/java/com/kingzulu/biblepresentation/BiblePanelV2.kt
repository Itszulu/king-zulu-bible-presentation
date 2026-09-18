package com.kingzulu.biblepresentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val KZ_BIBLE_BOOKS=listOf("Genesis","Exodus","Leviticus","Numbers","Deuteronomy","Joshua","Judges","Ruth","1 Samuel","2 Samuel","1 Kings","2 Kings","1 Chronicles","2 Chronicles","Ezra","Nehemiah","Esther","Job","Psalms","Proverbs","Ecclesiastes","Song of Solomon","Isaiah","Jeremiah","Lamentations","Ezekiel","Daniel","Hosea","Joel","Amos","Obadiah","Jonah","Micah","Nahum","Habakkuk","Zephaniah","Haggai","Zechariah","Malachi","Matthew","Mark","Luke","John","Acts","Romans","1 Corinthians","2 Corinthians","Galatians","Ephesians","Philippians","Colossians","1 Thessalonians","2 Thessalonians","1 Timothy","2 Timothy","Titus","Philemon","Hebrews","James","1 Peter","2 Peter","1 John","2 John","3 John","Jude","Revelation")

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun BiblePanelV2(onPreview:(PresentationSlide)->Unit,onGoLive:(PresentationSlide)->Unit,onAddToService:((PresentationSlide)->Unit)?=null){
 val context=LocalContext.current;val keyboard=LocalSoftwareKeyboardController.current;val scope=rememberCoroutineScope();val listState=rememberLazyListState()
 var q by rememberSaveable{mutableStateOf("")};var message by rememberSaveable{mutableStateOf("")};var loading by rememberSaveable{mutableStateOf(false)};var translationMenu by remember{mutableStateOf(false)};var bookMenu by remember{mutableStateOf(false)};var chapterMenu by remember{mutableStateOf(false)};var verseMenu by remember{mutableStateOf(false)};var rowMenu by remember{mutableStateOf<Int?>(null)}
 var abbr by rememberSaveable{mutableStateOf(ScriptureRepository.preferredTranslation(context).abbreviation)};var chapterVerses by remember{mutableStateOf<List<Verse>>(emptyList())};var selectedVerse by rememberSaveable{mutableStateOf<Int?>(null)};var currentBook by rememberSaveable{mutableStateOf("John")};var currentChapter by rememberSaveable{mutableIntStateOf(3)}
 val selected=TranslationCatalog.byAbbreviation(abbr)?:TranslationCatalog.translations.first()
 fun slide(v:Verse)=PresentationSlide(v.reference.display(),v.text,v.translation)
 fun live(v:Verse){selectedVerse=v.reference.verseStart;onGoLive(slide(v));AiScriptureEngine.remember(v)}
 fun load(book:String,chapter:Int,wanted:Int?=null,makeLive:Boolean=false){if(loading)return;loading=true;message="";keyboard?.hide();scope.launch{val verses=withContext(Dispatchers.IO){OfflineBibleRepository.chapter(book,chapter,selected.abbreviation)};loading=false;if(verses.isEmpty()){message="$book $chapter doesn't exist in the installed ${selected.abbreviation} Bible.";return@launch};currentBook=verses.first().reference.book;currentChapter=chapter;chapterVerses=verses;if(wanted==null){selectedVerse=null;listState.scrollToItem(0);return@launch};val i=verses.indexOfFirst{it.reference.verseStart==wanted};if(i<0){selectedVerse=null;message="$currentBook $chapter ends at verse ${verses.last().reference.verseStart}. Verse $wanted doesn't exist."}else{selectedVerse=wanted;listState.scrollToItem(i);if(makeLive)live(verses[i])}}}
 fun submit(){val r=BibleReferenceParser.parse(q);if(r==null){message="Couldn't understand that Bible reference.";return};load(r.book,r.chapter,r.verseStart,makeLive=r.verseStart!=null)}
 fun adjacent(delta:Int){val n=selectedVerse?:return;val i=chapterVerses.indexOfFirst{it.reference.verseStart==n};val target=i+delta;if(target in chapterVerses.indices){val v=chapterVerses[target];selectedVerse=v.reference.verseStart;scope.launch{listState.animateScrollToItem(target)};live(v)}else if(delta>0){val next=currentChapter+1;scope.launch{val vs=withContext(Dispatchers.IO){OfflineBibleRepository.chapter(currentBook,next,selected.abbreviation)};if(vs.isNotEmpty())load(currentBook,next,vs.first().reference.verseStart,true)}}else if(currentChapter>1){val prev=currentChapter-1;scope.launch{val vs=withContext(Dispatchers.IO){OfflineBibleRepository.chapter(currentBook,prev,selected.abbreviation)};if(vs.isNotEmpty())load(currentBook,prev,vs.last().reference.verseStart,true)}}}
 Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
  Text("Bible",style=MaterialTheme.typography.headlineLarge)
  Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
   ExposedDropdownMenuBox(translationMenu,{translationMenu=!translationMenu}){OutlinedTextField("${selected.abbreviation} — ${selected.name}",{},readOnly=true,label={Text("Translation")},trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(translationMenu)},modifier=Modifier.menuAnchor().fillMaxWidth());ExposedDropdownMenu(translationMenu,{translationMenu=false}){TranslationCatalog.translations.forEach{t->DropdownMenuItem({Text("${t.abbreviation} — ${t.name}")},{abbr=t.abbreviation;ScriptureRepository.setPreferredTranslation(context,t);translationMenu=false;if(chapterVerses.isNotEmpty())load(currentBook,currentChapter,selectedVerse,false)})}}}
   OutlinedTextField(q,{q=it;message=""},Modifier.fillMaxWidth(),singleLine=true,label={Text("Reference")},placeholder={Text("John 3:16")},keyboardOptions=KeyboardOptions(imeAction=ImeAction.Search),keyboardActions=KeyboardActions(onSearch={submit()}));Button({submit()},enabled=!loading,modifier=Modifier.fillMaxWidth()){Text(if(loading)"LOADING…" else "OPEN / GO LIVE")};if(message.isNotBlank())Text(message,color=MaterialTheme.colorScheme.secondary)
  }}
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
   Box(Modifier.weight(1.5f)){OutlinedButton({bookMenu=true},Modifier.fillMaxWidth()){Text("$currentBook ▾",maxLines=1)};DropdownMenu(bookMenu,{bookMenu=false}){KZ_BIBLE_BOOKS.forEach{b->DropdownMenuItem({Text(b)},{bookMenu=false;load(b,1,null,false)})}}}
   Box(Modifier.weight(1f)){OutlinedButton({chapterMenu=true},Modifier.fillMaxWidth()){Text("Ch $currentChapter ▾")};DropdownMenu(chapterMenu,{chapterMenu=false}){(1..150).forEach{c->DropdownMenuItem({Text("Chapter $c")},{chapterMenu=false;load(currentBook,c,null,false)})}}}
   Box(Modifier.weight(1f)){OutlinedButton({verseMenu=true},Modifier.fillMaxWidth(),enabled=chapterVerses.isNotEmpty()){Text("V ${selectedVerse?:"—"} ▾")};DropdownMenu(verseMenu,{verseMenu=false}){chapterVerses.forEach{v->val n=v.reference.verseStart?:return@forEach;DropdownMenuItem({Text("Verse $n")},{verseMenu=false;selectedVerse=n;scope.launch{listState.animateScrollToItem(chapterVerses.indexOf(v))};live(v)})}}}
  }
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton({adjacent(-1)},Modifier.weight(1f),enabled=selectedVerse!=null){Text("‹ PREVIOUS")};Button({adjacent(1)},Modifier.weight(1f),enabled=selectedVerse!=null){Text("NEXT ›")};OutlinedButton({load(currentBook,(currentChapter+1),null,false)},Modifier.weight(1f)){Text("NEXT CH.")}}
  if(chapterVerses.isNotEmpty())LazyColumn(state=listState,verticalArrangement=Arrangement.spacedBy(5.dp),modifier=Modifier.fillMaxWidth()){
   items(chapterVerses,key={"${it.reference.chapter}:${it.reference.verseStart}"}){v->val n=v.reference.verseStart?:0;val active=n==selectedVerse;Card(colors=if(active)CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.secondaryContainer)else CardDefaults.cardColors(),modifier=Modifier.fillMaxWidth().clickable{live(v)}){Row(Modifier.padding(10.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){Text(n.toString(),fontWeight=FontWeight.Bold);Text(v.text,Modifier.weight(1f));Box{TextButton({rowMenu=n},contentPadding=PaddingValues(4.dp)){Text("⋮")};DropdownMenu(rowMenu==n,{rowMenu=null}){DropdownMenuItem({Text("Preview")},{rowMenu=null;onPreview(slide(v))});DropdownMenuItem({Text("Go Live")},{rowMenu=null;live(v)});if(onAddToService!=null)DropdownMenuItem({Text("Add to Service")},{rowMenu=null;onAddToService(slide(v))})}}}}}
  }
 }
}
