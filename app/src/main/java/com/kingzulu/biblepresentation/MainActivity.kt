@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.kingzulu.biblepresentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext

private val KingZuluDark = darkColorScheme(
    primary = Color(0xFF7C5CFF), secondary = Color(0xFF9C8BFF), background = Color(0xFF090A0D),
    surface = Color(0xFF14161B), surfaceVariant = Color(0xFF202229), onBackground = Color(0xFFF7F7F8), onSurface = Color(0xFFF7F7F8)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = KingZuluDark) { KingZuluApp() } }
    }
}

@Composable
fun KingZuluApp() {
    var tab by remember { mutableStateOf("Bible") }
    var live by remember { mutableStateOf<PresentationSlide?>(null) }
    var preview by remember { mutableStateOf<PresentationSlide?>(null) }
    var theme by remember { mutableStateOf<BackgroundTheme?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { theme = LocalBackgroundStore.importFromGallery(context, it) } }
    val castContext = remember { runCatching { CastContext.getSharedInstance(context) }.getOrNull() }
    val castSession = castContext?.sessionManager?.currentCastSession
    val connectedName = castSession?.castDevice?.friendlyName

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("KING ZULU", fontWeight = FontWeight.Black, fontSize = 21.sp)
                        Text("Bible Presentation", color = Color(0xFFA6A7AD), fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(if (connectedName != null) "DISPLAY CONNECTED" else "CONNECT DISPLAY", fontSize = 10.sp, color = Color(0xFFA6A7AD), fontWeight = FontWeight.Bold)
                        AndroidCastButton()
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(color = if (live != null) Color(0xFF32161A) else MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(20.dp)) {
                        Text(if (live != null) "● LIVE" else "○ READY", Modifier.padding(horizontal = 11.dp, vertical = 7.dp), color = if (live != null) Color(0xFFFF6B72) else Color(0xFFB8BAC1), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF101116)) {
                listOf("Bible", "Lyrics", "Timer", "Live").forEach { item -> NavigationBarItem(selected = tab == item, onClick = { tab = item }, icon = { Text(if (tab == item) "●" else "○") }, label = { Text(item) }) }
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (connectedName != null) {
                Surface(Modifier.fillMaxWidth(), color = Color(0xFF15251F), shape = RoundedCornerShape(16.dp)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("📺", fontSize = 22.sp); Spacer(Modifier.width(10.dp)); Column { Text(connectedName, fontWeight = FontWeight.Bold); Text("Google Cast display connected", color = Color(0xFFA7C9B7), fontSize = 12.sp) }
                    }
                }
            }
            when (tab) {
                "Bible" -> BiblePanel { preview = it; tab = "Live" }
                "Lyrics" -> LyricsPanel { preview = it; tab = "Live" }
                "Timer" -> TimerPanel { preview = it; tab = "Live" }
                else -> LivePanel(preview, live, theme, { live = it }, { gallery.launch("image/*") })
            }
        }
    }
}

@Composable
private fun AndroidCastButton() {
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { ctx -> MediaRouteButton(ctx).apply { CastButtonFactory.setUpMediaRouteButton(ctx, this) } },
        modifier = Modifier.size(42.dp)
    )
}

@Composable private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(22.dp)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content) }
}

@Composable fun BiblePanel(onPreview: (PresentationSlide) -> Unit) {
    var q by remember { mutableStateOf("") }; var message by remember { mutableStateOf("") }
    Text("Bible", fontSize = 32.sp, fontWeight = FontWeight.Bold); Text("Type a reference and King Zulu will understand it.", color = Color(0xFFA6A7AD))
    SectionCard {
        OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), label = { Text("Search Scripture") }, placeholder = { Text("Jn 5 24  •  Rom 8 28  •  Ps 23 1") }, singleLine = true, shape = RoundedCornerShape(16.dp))
        Button({ val r = BibleReferenceParser.parse(q); val v = r?.let { OfflineBibleRepository.get(it) }; if (v != null) { message = ""; onPreview(PresentationSlide(v.reference.display(), v.text, v.translation)) } else message = "Verse not in Beta starter set" }, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("PREVIEW VERSE", fontWeight = FontWeight.Bold) }
        if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.secondary)
    }
    Text("Quick access", fontWeight = FontWeight.SemiBold, fontSize = 18.sp); SectionCard { Text("John 3:16", fontWeight = FontWeight.Bold); Text("Romans 8:28", fontWeight = FontWeight.Bold); Text("Psalm 23:1", fontWeight = FontWeight.Bold) }
}

@Composable fun LyricsPanel(onPreview: (PresentationSlide) -> Unit) {
    var text by remember { mutableStateOf("") }; Text("Lyrics", fontSize = 32.sp, fontWeight = FontWeight.Bold); Text("Prepare song lyrics for the live display.", color = Color(0xFFA6A7AD))
    SectionCard { OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth().height(220.dp), label = { Text("Paste or type lyrics") }, shape = RoundedCornerShape(16.dp)); Button({ if (text.isNotBlank()) onPreview(PresentationSlide(text = text.lines().take(4).joinToString("\n"), kind = "lyrics")) }, Modifier.fillMaxWidth()) { Text("PREVIEW LYRICS") } }
}

@Composable fun TimerPanel(onPreview: (PresentationSlide) -> Unit) {
    var min by remember { mutableStateOf("5") }; Text("Countdown", fontSize = 32.sp, fontWeight = FontWeight.Bold); Text("Create a clean service countdown screen.", color = Color(0xFFA6A7AD))
    SectionCard { OutlinedTextField(min, { min = it.filter(Char::isDigit).take(3) }, Modifier.fillMaxWidth(), label = { Text("Minutes") }, shape = RoundedCornerShape(16.dp)); Button({ val s = (min.toLongOrNull() ?: 5) * 60; onPreview(PresentationSlide(text = "SERVICE BEGINS IN\n\n${formatCountdown(s)}", kind = "countdown")) }, Modifier.fillMaxWidth()) { Text("PREVIEW COUNTDOWN") } }
}

@Composable fun LivePanel(preview: PresentationSlide?, live: PresentationSlide?, theme: BackgroundTheme?, setLive: (PresentationSlide?) -> Unit, chooseBackground: () -> Unit) {
    Text("Present", fontSize = 32.sp, fontWeight = FontWeight.Bold); Text("Preview first. Send to the congregation when you're ready.", color = Color(0xFFA6A7AD))
    Text("PREVIEW", fontWeight = FontWeight.Bold, color = Color(0xFFA6A7AD)); Surface(shape = RoundedCornerShape(18.dp), color = Color.Black) { PresentationCanvas(preview, theme, Modifier.fillMaxWidth()) }
    Button({ setLive(preview) }, enabled = preview != null, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp)) { Text("GO LIVE", fontWeight = FontWeight.Black) }
    Text("CURRENT LIVE", fontWeight = FontWeight.Bold, color = Color(0xFFA6A7AD)); Surface(shape = RoundedCornerShape(18.dp), color = Color.Black) { PresentationCanvas(live, theme, Modifier.fillMaxWidth()) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button({ setLive(PresentationSlide(text = "", kind = "black")) }, Modifier.weight(1f)) { Text("BLACK") }; OutlinedButton({ setLive(null) }, Modifier.weight(1f)) { Text("CLEAR") }; OutlinedButton({ setLive(PresentationSlide(text = "KING ZULU", kind = "logo")) }, Modifier.weight(1f)) { Text("LOGO") } }
    OutlinedButton(chooseBackground, Modifier.fillMaxWidth()) { Text("BACKGROUND FROM GALLERY") }
}
