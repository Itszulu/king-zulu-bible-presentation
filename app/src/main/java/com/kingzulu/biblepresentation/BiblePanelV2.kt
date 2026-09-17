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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiblePanelV2(onPreview:(PresentationSlide)->Unit){
    val context=LocalContext.current
    val keyboard=LocalSoftwareKeyboardController.current
    val scope=rememberCoroutineScope()
    val listState=rememberLazyListState()
    var q by rememberSaveable{mutableStateOf("")}
    var message by rememberSaveable{mutableStateOf("")}
    var loading by rememberSaveable{mutableStateOf(false)}
    var expanded by remember{mutableStateOf(false)}
    var abbr by rememberSaveable{mutableStateOf(ScriptureRepository.preferredTranslation(context).abbreviation)}
    var chapterVerses by remember{mutableStateOf<List<Verse>>(emptyList())}
    var selectedVerse by rememberSaveable{mutableStateOf<Int?>(null)}
    var chapterTitle by rememberSaveable{mutableStateOf("")}
    val selected=TranslationCatalog.byAbbreviation(abbr)?:TranslationCatalog.translations.first()

    fun loadReference(r:BibleReference){
        if(loading)return
        loading=true; message=""; keyboard?.hide()
        scope.launch{
            val chapter=withContext(Dispatchers.IO){OfflineBibleRepository.chapter(r.book,r.chapter,selected.abbreviation)}
            loading=false
            if(chapter.isEmpty()){
                // Never touch the live/preview output for an invalid chapter.
                message="${r.book} ${r.chapter} doesn't exist in the installed ${selected.abbreviation} Bible."
                chapterVerses=emptyList(); selectedVerse=null; chapterTitle=""
                return@launch
            }
            chapterVerses=chapter; chapterTitle="${r.book} ${r.chapter}"
            val wanted=r.verseStart
            if(wanted==null){ selectedVerse=null; return@launch }
            val index=chapter.indexOfFirst{it.reference.verseStart==wanted}
            if(index<0){
                selectedVerse=null
                val last=chapter.maxOfOrNull{it.reference.verseStart?:0}?:0
                message="${r.book} ${r.chapter} ends at verse $last. Verse $wanted doesn't exist."
                // Invalid verse is browsing only: project/preview nothing.
            } else {
                selectedVerse=wanted
                listState.scrollToItem(index)
            }
        }
    }

    fun submit(){
        val r=BibleReferenceParser.parse(q)
        if(r==null){message="Couldn't understand that Bible reference.";return}
        loadReference(r)
    }

    Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
        Text("Bible",style=MaterialTheme.typography.headlineLarge)
        Card(Modifier.fillMaxWidth()){
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                ExposedDropdownMenuBox(expanded=expanded,onExpandedChange={expanded=!expanded}){
                    OutlinedTextField("${selected.abbreviation} — ${selected.name}",{},readOnly=true,label={Text("Translation")},trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(expanded)},modifier=Modifier.menuAnchor().fillMaxWidth())
                    ExposedDropdownMenu(expanded,{expanded=false}){TranslationCatalog.translations.forEach{t->DropdownMenuItem(text={Text("${t.abbreviation} — ${t.name}${if(t.offline)" · Offline" else ""}")},onClick={abbr=t.abbreviation;ScriptureRepository.setPreferredTranslation(context,t);expanded=false;message=""})}}
                }
                OutlinedTextField(q,{q=it;message=""},Modifier.fillMaxWidth(),singleLine=true,label={Text("Reference")},placeholder={Text("John 3:16 • John 3 16")},keyboardOptions=KeyboardOptions(imeAction=ImeAction.Search),keyboardActions=KeyboardActions(onSearch={submit()}))
                Button({submit()},enabled=!loading,modifier=Modifier.fillMaxWidth()){if(loading)CircularProgressIndicator(Modifier.size(20.dp),strokeWidth=2.dp)else Text("OPEN")}
                if(message.isNotBlank()) Text(message,color=MaterialTheme.colorScheme.secondary)
            }
        }
        if(chapterVerses.isNotEmpty()){
            Text(chapterTitle,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold)
            LazyColumn(state=listState,verticalArrangement=Arrangement.spacedBy(6.dp),modifier=Modifier.fillMaxWidth()){
                items(chapterVerses,key={"${it.reference.chapter}:${it.reference.verseStart}"}){v->
                    val n=v.reference.verseStart?:0
                    val active=n==selectedVerse
                    Card(
                        colors=if(active)CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.secondaryContainer) else CardDefaults.cardColors(),
                        modifier=Modifier.fillMaxWidth().clickable{
                            selectedVerse=n
                            // Manual verse selection prepares this verse only; it does not navigate away.
                            onPreview(PresentationSlide(v.reference.display(),v.text,v.translation))
                        }
                    ){
                        Row(Modifier.padding(12.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)){
                            Text(n.toString(),fontWeight=FontWeight.Bold)
                            Text(v.text,modifier=Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
