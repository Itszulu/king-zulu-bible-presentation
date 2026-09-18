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
@Composable
fun BiblePanelV2(
    onPreview: (PresentationSlide) -> Unit,
    onGoLive: (PresentationSlide) -> Unit,
    onAddToService: ((PresentationSlide) -> Unit)? = null
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var query by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    var loading by rememberSaveable { mutableStateOf(false) }
    var translationMenu by remember { mutableStateOf(false) }
    var bookMenu by remember { mutableStateOf(false) }
    var chapterMenu by remember { mutableStateOf(false) }
    var verseMenu by remember { mutableStateOf(false) }
    var rowMenu by remember { mutableStateOf<Int?>(null) }
    var abbreviation by rememberSaveable { mutableStateOf(OfflineBibleRepository.selectedTranslation()) }
    var installed by remember { mutableStateOf(OfflineBibleRepository.installedBibles()) }
    var bibleBooks by remember { mutableStateOf(OfflineBibleRepository.books(abbreviation)) }
    var availableChapters by remember { mutableStateOf<List<Int>>(emptyList()) }
    var chapterVerses by remember { mutableStateOf<List<Verse>>(emptyList()) }
    var selectedVerse by rememberSaveable { mutableStateOf<Int?>(null) }
    var currentBook by rememberSaveable { mutableStateOf(bibleBooks.firstOrNull() ?: "Genesis") }
    var currentChapter by rememberSaveable { mutableIntStateOf(1) }

    fun slide(v: Verse) = PresentationSlide(v.reference.display(), v.text, v.translation)
    fun live(v: Verse) {
        selectedVerse = v.reference.verseStart
        onGoLive(slide(v))
        AiScriptureEngine.remember(v)
    }
    fun load(book: String, chapter: Int, wanted: Int? = null, makeLive: Boolean = false) {
        if (loading) return
        loading = true
        message = ""
        keyboard?.hide()
        scope.launch {
            val chapters = withContext(Dispatchers.IO) { OfflineBibleRepository.chapters(book, abbreviation) }
            val verses = withContext(Dispatchers.IO) { OfflineBibleRepository.chapter(book, chapter, abbreviation) }
            loading = false
            if (verses.isEmpty()) {
                message = "$book $chapter doesn't exist in the installed $abbreviation Bible."
                return@launch
            }
            availableChapters = chapters
            currentBook = verses.first().reference.book
            currentChapter = chapter
            chapterVerses = verses
            if (wanted == null) {
                selectedVerse = null
                listState.scrollToItem(0)
                return@launch
            }
            val index = verses.indexOfFirst { it.reference.verseStart == wanted }
            if (index < 0) {
                selectedVerse = null
                message = "$currentBook $chapter ends at verse ${verses.last().reference.verseStart}. Verse $wanted doesn't exist."
            } else {
                selectedVerse = wanted
                listState.scrollToItem(index)
                if (makeLive) live(verses[index])
            }
        }
    }
    fun refreshStructure(preferredBook: String? = null) {
        installed = OfflineBibleRepository.installedBibles()
        bibleBooks = OfflineBibleRepository.books(abbreviation)
        currentBook = preferredBook?.takeIf { it in bibleBooks } ?: bibleBooks.firstOrNull() ?: currentBook
        availableChapters = OfflineBibleRepository.chapters(currentBook, abbreviation)
        currentChapter = availableChapters.firstOrNull() ?: 1
    }
    fun submit() {
        val reference = BibleReferenceParser.parse(query)
        if (reference == null) {
            message = "Couldn't understand that Bible reference."
            return
        }
        load(reference.book, reference.chapter, reference.verseStart, reference.verseStart != null)
    }
    fun adjacent(delta: Int) {
        val number = selectedVerse ?: return
        val index = chapterVerses.indexOfFirst { it.reference.verseStart == number }
        val target = index + delta
        if (target in chapterVerses.indices) {
            val verse = chapterVerses[target]
            scope.launch { listState.animateScrollToItem(target) }
            live(verse)
            return
        }
        val chapterIndex = availableChapters.indexOf(currentChapter)
        val nextChapter = availableChapters.getOrNull(chapterIndex + if (delta > 0) 1 else -1) ?: return
        scope.launch {
            val verses = withContext(Dispatchers.IO) { OfflineBibleRepository.chapter(currentBook, nextChapter, abbreviation) }
            val verse = if (delta > 0) verses.firstOrNull() else verses.lastOrNull()
            if (verse != null) load(currentBook, nextChapter, verse.reference.verseStart, true)
        }
    }

    LaunchedEffect(Unit) {
        if (bibleBooks.isNotEmpty()) {
            availableChapters = OfflineBibleRepository.chapters(currentBook, abbreviation)
            load(currentBook, availableChapters.firstOrNull() ?: 1)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Bible", style = MaterialTheme.typography.headlineLarge)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(expanded = translationMenu, onExpandedChange = { translationMenu = !translationMenu }) {
                    OutlinedTextField(
                        value = installed.firstOrNull { it.abbreviation == abbreviation }?.let { "${it.abbreviation} — ${it.name}" } ?: abbreviation,
                        onValueChange = {}, readOnly = true, label = { Text("Installed Bible") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(translationMenu) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = translationMenu, onDismissRequest = { translationMenu = false }) {
                        installed.forEach { bible ->
                            DropdownMenuItem(text = { Text("${bible.abbreviation} — ${bible.name}") }, onClick = {
                                translationMenu = false
                                if (OfflineBibleRepository.selectTranslation(bible.abbreviation)) {
                                    abbreviation = bible.abbreviation
                                    refreshStructure(currentBook)
                                    load(currentBook, currentChapter)
                                }
                            })
                        }
                    }
                }
                OutlinedTextField(
                    value = query, onValueChange = { query = it; message = "" }, modifier = Modifier.fillMaxWidth(),
                    singleLine = true, label = { Text("Reference") }, placeholder = { Text("John 3:16") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { submit() })
                )
                Button(onClick = { submit() }, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text(if (loading) "LOADING…" else "OPEN / GO LIVE") }
                if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.secondary)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.weight(1.5f)) {
                OutlinedButton(onClick = { bookMenu = true }, modifier = Modifier.fillMaxWidth(), enabled = bibleBooks.isNotEmpty()) { Text("$currentBook ▾", maxLines = 1) }
                DropdownMenu(expanded = bookMenu, onDismissRequest = { bookMenu = false }) {
                    bibleBooks.forEach { book -> DropdownMenuItem(text = { Text(book) }, onClick = { bookMenu = false; val chapters = OfflineBibleRepository.chapters(book, abbreviation); availableChapters = chapters; load(book, chapters.firstOrNull() ?: 1) }) }
                }
            }
            Box(Modifier.weight(1f)) {
                OutlinedButton(onClick = { chapterMenu = true }, modifier = Modifier.fillMaxWidth(), enabled = availableChapters.isNotEmpty()) { Text("Ch $currentChapter ▾") }
                DropdownMenu(expanded = chapterMenu, onDismissRequest = { chapterMenu = false }) { availableChapters.forEach { chapter -> DropdownMenuItem(text = { Text("Chapter $chapter") }, onClick = { chapterMenu = false; load(currentBook, chapter) }) } }
            }
            Box(Modifier.weight(1f)) {
                OutlinedButton(onClick = { verseMenu = true }, modifier = Modifier.fillMaxWidth(), enabled = chapterVerses.isNotEmpty()) { Text("V ${selectedVerse ?: "—"} ▾") }
                DropdownMenu(expanded = verseMenu, onDismissRequest = { verseMenu = false }) { chapterVerses.forEach { verse -> val n = verse.reference.verseStart ?: return@forEach; DropdownMenuItem(text = { Text("Verse $n") }, onClick = { verseMenu = false; scope.launch { listState.animateScrollToItem(chapterVerses.indexOf(verse)) }; live(verse) }) } }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { adjacent(-1) }, modifier = Modifier.weight(1f), enabled = selectedVerse != null) { Text("‹ PREVIOUS") }
            Button(onClick = { adjacent(1) }, modifier = Modifier.weight(1f), enabled = selectedVerse != null) { Text("NEXT ›") }
            val chapterIndex = availableChapters.indexOf(currentChapter)
            OutlinedButton(onClick = { availableChapters.getOrNull(chapterIndex + 1)?.let { load(currentBook, it) } }, modifier = Modifier.weight(1f), enabled = chapterIndex >= 0 && chapterIndex < availableChapters.lastIndex) { Text("NEXT CH.") }
        }

        if (chapterVerses.isNotEmpty()) {
            LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
                items(chapterVerses, key = { "${it.reference.chapter}:${it.reference.verseStart}" }) { verse ->
                    val number = verse.reference.verseStart ?: 0
                    val active = number == selectedVerse
                    Card(colors = if (active) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer) else CardDefaults.cardColors(), modifier = Modifier.fillMaxWidth().clickable { live(verse) }) {
                        Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(number.toString(), fontWeight = FontWeight.Bold)
                            Text(verse.text, Modifier.weight(1f))
                            Box {
                                TextButton(onClick = { rowMenu = number }, contentPadding = PaddingValues(4.dp)) { Text("⋮") }
                                DropdownMenu(expanded = rowMenu == number, onDismissRequest = { rowMenu = null }) {
                                    DropdownMenuItem(text = { Text("Preview") }, onClick = { rowMenu = null; onPreview(slide(verse)) })
                                    DropdownMenuItem(text = { Text("Go Live") }, onClick = { rowMenu = null; live(verse) })
                                    if (onAddToService != null) DropdownMenuItem(text = { Text("Add to Service") }, onClick = { rowMenu = null; onAddToService(slide(verse)) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
