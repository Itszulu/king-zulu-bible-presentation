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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun BiblePanelV2(onPreview:(PresentationSlide)->Unit,onGoLive:(PresentationSlide)->Unit,onAddToService:((PresentationSlide)->Unit)?=null){
 val keyboard=LocalSoftwareKeyboardController.current;val scope=rememberCoroutineScope();val listState=rememberLazyListState()
 var q by rememberSaveable{mutableStateOf("")};var message by rememberSaveable{mutableStateOf("")};var loading by rememberSaveable{mutableStateOf(false)};var translationMenu by remember{mutableStateOf(false)};var bookMenu by remember{mutableStateOf(false)};var chapterMenu by remember{mutableStateOf(false)};var verseMenu by remember{mutableStateOf(false)};var rowMenu by remember{mutableStateOf<Int?>(null)}
 var abbr by rememberSaveable{mutableStateOf(OfflineBibleRepository.selectedTranslation())};var installed by remember{mutableStateOf(OfflineBibleRepository.installedBibles())};var bibleBooks by remember{mutableStateOf(OfflineBibleRepository.books(abbr))};var availableChapters by remember{mutableStateOf<List<Int>>(emptyList())};var chapterVerses by remember{mutableStateOf<List<Verse>>(emptyList())};var selectedVerse by rememberSaveable{mutableStateOf<Int?>(null)};var currentBook by rememberSaveable{mutableStateOf(bibleBooks.firstOrNull()?:"Genesis")};var currentChapter by rememberSaveable{mutableIntStateOf(1)}
 fun slide(v:Verse)=PresentationSlide(v.reference.display(),v.text,v.translation)
 fun live(v:Verse){selectedVerse=v.reference.verseStart;onGoLive(slide(v));AiScriptureEngine.remember(v)}
 fun refreshStructure(preferredBook:String?=null){installed=OfflineBibleRepository.installedBibles();bibleBooks=OfflineBibleRepository.books(abbr);currentBook=preferredBook?.takeIf{it in bibleBooks}?:bibleBooks.firstOrNull()?:currentBook;availableChapters=OfflineBibleRepository.chapters(currentBook,abbr);currentChapter=availableChapters.firstOrNull()?:1}
 fun load(book:String,chapter:Int,wanted:Int?=null,makeLive:Boolean=false){if(loading)return;loading=true;message="";keyboard?.hide();scope.launch{val chapters=withContext(Dispatchers.IO){OfflineBibleRepository.chapters(book,abbr)};val verses=withContext(Dispatchers.IO){OfflineBibleRepository.chapter(book,chapter,abbr)};loading=false;if(verses.isEmpty()){message="$book $chapter doesn't exist in the installed $abbr Bible.";return@launch};availableChapters=chapters;currentBook=verses.first().reference.book;currentChapter=chapter;chapterVerses=verses;if(wanted==null){selectedVerse=null;listState.scrollToItem(0);return@launch};val i=verses.indexOfFirst{it.reference.verseStart==wanted};if(i<0){selectedVerse=null;message="$currentBook $chapter ends at verse ${verses.last().reference.verseStart}. Verse $wanted doesn't exist."}else{selectedVerse=wanted;listState.scrollToItem(i);if(makeLive)live(verses[i])}}}
 fun submit(){val r=BibleReferenceParser.parse(q);if(r==null){message="Couldn't understand that Bible reference.";return};load(r.book,r.chapter,r.verseStart,r.verseStart!=null)}
 fun adjacent(delta:Int){val n=selectedVerse?:return;val i=chapterVerses.indexOfFirst{it.reference.verseStart==n};val target=i+delta;if(target in chapterVerses.indices){val v=chapterVerses[target];scope.launch{listState.animateScrollToItem(target)};live(v);return};val ci=availableChapters.indexOf(currentChapter);val nextChapter=availableChapters.getOrNull(ci+if(delta>0)1 else -1)?:return;scope.launch{val vs=withContext(Dispatchers.IO){OfflineBibleRepository.chapter(currentBook,nextChapter,abbr)};val v=if(delta>0)vs.firstOrNull()else vs.lastOrNull();if(v!=null)load(currentBook,nextChapter,v.reference.verseStart,true)}}
 LaunchedEffect(Unit){if(bibleBooks.isNotEmpty()){availableChapters=OfflineBibleRepository.chapters(currentBook,abbr);load(currentBook,availableChapters.firstOrNull()?:1,null,false)}}
 Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
  Text("Bible",style=MaterialTheme.typography.headlineLarge)
  Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
   ExposedDropdownMenuBox(translationMenu,{translationMenu=!translationMenu}){OutlinedTextField(installed.firstOrNull{it.abbreviation==abbr}?.let{"${it.abbreviation} — ${it.name}"}?:abbr,{},readOnly=true,label={Text("Installed Bible")},trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(translationMenu)},modifier=Modifier.menuAnchor().fillMaxWidth());ExposedDropdownMenu(translationMenu,{translationMenu=false}){installed.forEach{b->DropdownMenuItem({Text("${b.abbreviation} — ${b.name}")},{translationMenu=false;if(OfflineBibleRepository.selectTranslation(b.abbreviation)){abbr=b.abbreviation;refreshStructure(currentBook);load(currentBook,currentChapter,null,false)}})}}
   OutlinedTextField(q,{q=it;message=""},Modifier.fillMaxWidth(),singleLine=true,label={Text("Reference")},placeholder={Text("John 3:16")},keyboardOptions=KeyboardOptions(imeAction=ImeAction.Search),keyboardActions=KeyboardActions(onSearch={submit()}));Button({submit()},enabled=!loading,modifier=Modifier.fillMaxWidth()){Text(if(loading)"LOADING…" else "OPEN / GO LIVE")};if(message.isNotBlank())Text(message,color=MaterialTheme.colorScheme.secondary)
  }}
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
   Box(Modifier.weight(1.5f)){OutlinedButton({bookMenu=true},Modifier.fillMaxWidth(),enabled=bibleBooks.isNotEmpty()){Text("$currentBook ▾",maxLines=1)};DropdownMenu(bookMenu,{bookMenu=false}){bibleBooks.forEach{b->DropdownMenuItem({Text(b)},{bookMenu=false;val cs=OfflineBibleRepository.chapters(b,abbr);availableChapters=cs;load(b,cs.firstOrNull()?:1,null,false)})}}}
   Box(Modifier.weight(1f)){OutlinedButton({chapterMenu=true},Modifier.fillMaxWidth(),enabled=availableChapters.isNotEmpty()){Text("Ch $currentChapter ▾")};DropdownMenu(chapterMenu,{chapterMenu=false}){availableChapters.forEach{c->DropdownMenuItem({Text("Chapter $c")},{chapterMenu=false;load(currentBook,c,null,false)})}}}
   Box(Modifier.weight(1f)){OutlinedButton({verseMenu=true},Modifier.fillMaxWidth(),enabled=chapterVerses.isNotEmpty()){Text("V ${selectedVerse?:"—"} ▾")};DropdownMenu(verseMenu,{verseMenu=false}){chapterVerses.forEach{v->val n=v.reference.verseStart?:return@forEach;DropdownMenuItem({Text("Verse $n")},{verseMenu=false;scope.launch{listState.animateScrollToItem(chapterVerses.indexOf(v))};live(v)})}}}
  }
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton({adjacent(-1)},Modifier.weight(1f),enabled=selectedVerse!=null){Text("‹ PREVIOUS")};Button({adjacent(1)},Modifier.weight(1f),enabled=selectedVerse!=null){Text("NEXT ›")};OutlinedButton({val ci=availableChapters.indexOf(currentChapter);availableChapters.getOrNull(ci+1)?.let{load(currentBook,it,null,false)}},Modifier.weight(1f),enabled=availableChapters.indexOf(currentChapter) in 0 until availableChapters.lastIndex){Text("NEXT CH.")}}
  if(chapterVerses.isNotEmpty())LazyColumn(state=listState,verticalArrangement=Arrangement.spacedBy(5.dp),modifier=Modifier.fillMaxWidth()){items(chapterVerses,key={"${it.reference.chapter}:${it.reference.verseStart}"}){v->val n=v.reference.verseStart?:0;val active=n==selectedVerse;Card(colors=if(active)CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.secondaryContainer)else CardDefaults.cardColors(),modifier=Modifier.fillMaxWidth().clickable{live(v)}){Row(Modifier.padding(10.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){Text(n.toString(),fontWeight=FontWeight.Bold);Text(v.text,Modifier.weight(1f));Box{TextButton({rowMenu=n},contentPadding=PaddingValues(4.dp)){Text("⋮")};DropdownMenu(rowMenu==n,{rowMenu=null}){DropdownMenuItem({Text("Preview")},{rowMenu=null;onPreview(slide(v))});DropdownMenuItem({Text("Go Live")},{rowMenu=null;live(v)});if(onAddToService!=null)DropdownMenuItem({Text("Add to Service")},{rowMenu=null;onAddToService(slide(v))})}}}}}}
 }
}
