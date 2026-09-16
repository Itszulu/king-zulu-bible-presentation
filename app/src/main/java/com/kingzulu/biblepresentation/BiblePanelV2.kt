package com.kingzulu.biblepresentation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun BiblePanelV2(onPreview:(PresentationSlide)->Unit){
 val context=LocalContext.current;val keyboard=LocalSoftwareKeyboardController.current;val scope=rememberCoroutineScope()
 var q by rememberSaveable{mutableStateOf("")};var message by rememberSaveable{mutableStateOf("")};var loading by rememberSaveable{mutableStateOf(false)};var expanded by remember{mutableStateOf(false)}
 var abbr by rememberSaveable{mutableStateOf(ScriptureRepository.preferredTranslation(context).abbreviation)}
 val selected=TranslationCatalog.byAbbreviation(abbr)?:TranslationCatalog.translations.first()
 fun submit(){if(loading)return;val r=BibleReferenceParser.parse(q);if(r==null){message="Verse not found — check the reference";return};loading=true;message="";keyboard?.hide();scope.launch{val result=withContext(Dispatchers.IO){ScriptureRepository.get(context,r,selected)};loading=false;result.onSuccess{v->onPreview(PresentationSlide(v.reference.display(),v.text,v.translation))}.onFailure{e->message=if(!selected.offline&&BuildConfig.YOUVERSION_APP_KEY.isBlank())"${selected.abbreviation} needs online translation access in this build. KJV remains available offline." else e.message?:"Unable to load ${selected.abbreviation}"}}}
 Text("Bible",style=MaterialTheme.typography.headlineLarge);Text("Find Scripture fast and choose the translation you want.",color=MaterialTheme.colorScheme.onSurfaceVariant)
 Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  ExposedDropdownMenuBox(expanded=expanded,onExpandedChange={expanded=!expanded}){OutlinedTextField("${selected.abbreviation} — ${selected.name}",{},readOnly=true,label={Text("Translation")},trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(expanded)},modifier=Modifier.menuAnchor().fillMaxWidth());ExposedDropdownMenu(expanded,{expanded=false}){TranslationCatalog.translations.forEach{t->DropdownMenuItem(text={Text("${t.abbreviation} — ${t.name}${if(t.offline)" · Offline" else ""}")},onClick={abbr=t.abbreviation;ScriptureRepository.setPreferredTranslation(context,t);expanded=false;message=""})}}}
  OutlinedTextField(q,{q=it;message=""},Modifier.fillMaxWidth(),singleLine=true,label={Text("Reference")},placeholder={Text("John 3:16 • Galatians 2:4")},keyboardOptions=KeyboardOptions(imeAction=ImeAction.Search),keyboardActions=KeyboardActions(onSearch={submit()}))
  Button({submit()},enabled=!loading,modifier=Modifier.fillMaxWidth()){if(loading)CircularProgressIndicator(Modifier.size(20.dp),strokeWidth=2.dp)else Text("PREVIEW VERSE · ${selected.abbreviation}")};if(message.isNotBlank())Text(message,color=MaterialTheme.colorScheme.secondary)
 }}
}
