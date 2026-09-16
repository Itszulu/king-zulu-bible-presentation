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

private val KZPurple = Color(0xFF7B61FF)
private val KZPurpleSoft = Color(0xFF9B8AFF)
private val KZBlack = Color(0xFF0A0A0B)
private val KZSurface = Color(0xFF151518)
private val KZSurface2 = Color(0xFF202025)
private val KZText = Color(0xFFF7F7F8)
private val KZMuted = Color(0xFFAAAAB2)
private val KZLive = Color(0xFFFF5C68)
private val KZGreen = Color(0xFF73D59B)

private val KingZuluDark = darkColorScheme(
    primary = KZPurple,
    onPrimary = Color.White,
    secondary = KZPurpleSoft,
    background = KZBlack,
    surface = KZSurface,
    surfaceVariant = KZSurface2,
    onBackground = KZText,
    onSurface = KZText,
    outline = Color(0xFF3B3B42)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OfflineBibleRepository.initialize(this)
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
        containerColor = KZBlack,
        topBar = {
            Surface(color = KZBlack, tonalElevation = 0.dp) {
                Column {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("KING ZULU", fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = 0.8.sp)
                            Text("Worship presentation", color = KZMuted, fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(if (connectedName != null) "DISPLAY CONNECTED" else "CONNECT DISPLAY", fontSize = 9.sp, color = if (connectedName != null) KZGreen else KZMuted, fontWeight = FontWeight.Bold)
                            AndroidCastButton()
                        }
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = if (live != null) Color(0xFF35161B) else KZSurface2,
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(
                                if (live != null) "● LIVE" else "○ READY",
                                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                color = if (live != null) KZLive else Color(0xFFB8BAC1),
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            )
                        }
                    }
                    if (live != null) {
                        Surface(color = Color(0xFF111114), modifier = Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("ON AIR", color = KZLive, fontWeight = FontWeight.Black, fontSize = 10.sp)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    live!!.reference.ifBlank { live!!.kind.uppercase() },
                                    color = KZText,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF101013), tonalElevation = 0.dp) {
                listOf(
                    "Bible" to "▤",
                    "Listen" to "◉",
                    "Lyrics" to "♫",
                    "Timer" to "◷",
                    "Present" to "▣"
                ).forEach { (item, glyph) ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            indicatorColor = Color(0xFF2B2448),
                            unselectedIconColor = Color(0xFF8C8D95),
                            unselectedTextColor = Color(0xFF8C8D95)
                        ),
                        icon = { Text(glyph, fontSize = 19.sp, fontWeight = FontWeight.Bold) },
                        label = { Text(item, fontSize = 10.sp, fontWeight = if (tab == item) FontWeight.Bold else FontWeight.Medium) }
                    )
                }
            }
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (connectedName != null) {
                Surface(Modifier.fillMaxWidth(), color = Color(0xFF14241D), shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("◉", color = KZGreen, fontSize = 18.sp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(connectedName, fontWeight = FontWeight.Bold)
                            Text("Ready for live output", color = Color(0xFFA7C9B7), fontSize = 12.sp)
                        }
                    }
                }
            }
            when (tab) {
                "Bible" -> BiblePanel { preview = it; tab = "Present" }
                "Listen" -> AiListenPanel(
                    onPreview = { preview = it; tab = "Present" },
                    onGoLive = { slide -> preview = slide; live = slide; tab = "Present" }
                )
                "Lyrics" -> LyricsPanel { preview = it; tab = "Present" }
                "Timer" -> TimerPanel { preview = it; tab = "Present" }
                else -> LivePanel(preview, live, theme, { live = it }, { gallery.launch("image/*") })
            }
        }
    }
}

@Composable
private fun AndroidCastButton() {
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { ctx -> MediaRouteButton(ctx).apply { CastButtonFactory.setUpMediaRouteButton(ctx, this) } },
        modifier = Modifier.size(40.dp)
    )
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        Modifier.fillMaxWidth(),
        color = KZSurface,
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 0.dp
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun PageHeading(title: String, subtitle: String) {
    Text(title, fontSize = 32.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
    Text(subtitle, color = KZMuted, fontSize = 14.sp, lineHeight = 20.sp)
}

@Composable
fun BiblePanel(onPreview: (PresentationSlide) -> Unit) {
    var q by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    PageHeading("Bible", "Find Scripture fast and prepare it for the congregation screen.")
    SectionCard {
        Text("FAST SCRIPTURE SEARCH", color = KZMuted, fontSize = 11.sp, fontWeight = FontWeight.Black)
        OutlinedTextField(
            q,
            { q = it },
            Modifier.fillMaxWidth(),
            label = { Text("Reference") },
            placeholder = { Text("Jn 5 24  •  Rom 8 28  •  Ps 23 1") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )
        Button(
            {
                val r = BibleReferenceParser.parse(q)
                val v = r?.let { OfflineBibleRepository.get(it) }
                if (v != null) {
                    message = ""
                    onPreview(PresentationSlide(v.reference.display(), v.text, v.translation))
                } else message = "Verse not found — check the reference"
            },
            Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp)
        ) { Text("PREVIEW VERSE", fontWeight = FontWeight.Black) }
        if (message.isNotBlank()) Text(message, color = KZPurpleSoft)
    }

    Text("Quick access", fontWeight = FontWeight.Bold, fontSize = 18.sp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("John 3:16", "Romans 8:28", "Psalm 23:1").forEach { ref ->
            AssistChip(
                onClick = {
                    q = ref
                    val r = BibleReferenceParser.parse(ref)
                    val v = r?.let { OfflineBibleRepository.get(it) }
                    if (v != null) onPreview(PresentationSlide(v.reference.display(), v.text, v.translation))
                },
                label = { Text(ref, fontSize = 11.sp) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun LyricsPanel(onPreview: (PresentationSlide) -> Unit) {
    var text by remember { mutableStateOf("") }
    PageHeading("Lyrics", "Prepare worship lyrics in clean, readable slides.")
    SectionCard {
        OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth().height(220.dp), label = { Text("Paste or type lyrics") }, shape = RoundedCornerShape(16.dp))
        Button({ if (text.isNotBlank()) onPreview(PresentationSlide(text = text.lines().take(4).joinToString("\n"), kind = "lyrics")) }, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
            Text("PREVIEW LYRICS", fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun TimerPanel(onPreview: (PresentationSlide) -> Unit) {
    var min by remember { mutableStateOf("5") }
    PageHeading("Countdown", "Build a clean pre-service or event countdown screen.")
    SectionCard {
        OutlinedTextField(min, { min = it.filter(Char::isDigit).take(3) }, Modifier.fillMaxWidth(), label = { Text("Minutes") }, shape = RoundedCornerShape(16.dp))
        Button({ val s = (min.toLongOrNull() ?: 5) * 60; onPreview(PresentationSlide(text = "SERVICE BEGINS IN\n\n${formatCountdown(s)}", kind = "countdown")) }, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
            Text("PREVIEW COUNTDOWN", fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun LivePanel(
    preview: PresentationSlide?,
    live: PresentationSlide?,
    theme: BackgroundTheme?,
    setLive: (PresentationSlide?) -> Unit,
    chooseBackground: () -> Unit
) {
    PageHeading("Present", "Preview first, then send confidently to the congregation display.")

    Text("PREVIEW", fontWeight = FontWeight.Black, color = KZMuted, fontSize = 11.sp)
    Surface(shape = RoundedCornerShape(20.dp), color = Color.Black) {
        PresentationCanvas(preview, theme, Modifier.fillMaxWidth())
    }
    Button({ setLive(preview) }, enabled = preview != null, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp)) {
        Text("GO LIVE", fontWeight = FontWeight.Black)
    }

    Text("CURRENT LIVE", fontWeight = FontWeight.Black, color = KZMuted, fontSize = 11.sp)
    Surface(shape = RoundedCornerShape(20.dp), color = Color.Black) {
        PresentationCanvas(live, theme, Modifier.fillMaxWidth())
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button({ setLive(PresentationSlide(text = "", kind = "black")) }, Modifier.weight(1f)) { Text("BLACK", fontWeight = FontWeight.Bold) }
        OutlinedButton({ setLive(null) }, Modifier.weight(1f)) { Text("CLEAR", fontWeight = FontWeight.Bold) }
        OutlinedButton({ setLive(PresentationSlide(text = "KING ZULU", kind = "logo")) }, Modifier.weight(1f)) { Text("LOGO", fontWeight = FontWeight.Bold) }
    }
    OutlinedButton(chooseBackground, Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp)) {
        Text("BACKGROUND FROM GALLERY", fontWeight = FontWeight.Bold)
    }
}
