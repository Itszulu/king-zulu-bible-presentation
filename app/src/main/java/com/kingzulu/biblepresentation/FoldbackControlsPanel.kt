package com.kingzulu.biblepresentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable fun FoldbackControlsPanel() {
    val context = LocalContext.current
    LaunchedEffect(Unit) { FoldbackWidgetState.initialize(context); FoldbackTimer.initialize(context) }
    var clock by remember { mutableStateOf(FoldbackWidgetState.layout.clock) }
    var alert by remember { mutableStateOf(FoldbackWidgetState.layout.alert) }
    var timerMinutes by rememberSaveable { mutableStateOf((FoldbackTimer.configuredSeconds / 60L).toString()) }
    LaunchedEffect(Unit) { while (true) { FoldbackTimer.refresh(); delay(250) } }
    Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("PRIVATE FOLDBACK TIMER", fontWeight = FontWeight.Black); Text(FoldbackTimer.display(), fontSize = 42.sp, fontWeight = FontWeight.Black); Text("Visible on the stage/foldback output only. It does not change the congregation countdown.", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(value=timerMinutes,onValueChange={value->timerMinutes=value.filter(Char::isDigit).take(3)},modifier=Modifier.fillMaxWidth(),enabled=!FoldbackTimer.running,label={Text("Minutes (1–240)")},singleLine=true)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton({val minutes=timerMinutes.toLongOrNull()?.coerceIn(1L,240L)?:return@OutlinedButton;FoldbackTimer.setMinutes(minutes);timerMinutes=minutes.toString()},Modifier.weight(1f),enabled=!FoldbackTimer.running){Text("SET")};OutlinedButton({FoldbackTimer.reset()},Modifier.weight(1f)){Text("RESET")}}
        Button({if(FoldbackTimer.running)FoldbackTimer.pause()else FoldbackTimer.startOrResume()},Modifier.fillMaxWidth()){Text(if(FoldbackTimer.running)"PAUSE" else "START")}
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column(Modifier.weight(1f)){Text("Overtime");Text("Continue past 00:00 as +time",style=MaterialTheme.typography.bodySmall)};Switch(FoldbackTimer.allowOvertime,{FoldbackTimer.setOvertime(it)})}
    }}
    Card { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("FOLDBACK CLOCK",fontWeight=FontWeight.Black);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Show clock");Switch(clock.enabled,{clock=clock.copy(enabled=it);FoldbackWidgetState.configureClock(clock,context)})};Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("24-hour format");Switch(clock.use24Hour,{clock=clock.copy(use24Hour=it);FoldbackWidgetState.configureClock(clock,context)})};Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Show seconds");Switch(clock.showSeconds,{clock=clock.copy(showSeconds=it);FoldbackWidgetState.configureClock(clock,context)})};Text("Position",style=MaterialTheme.typography.labelMedium);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf(WidgetAnchor.TOP_LEFT,WidgetAnchor.TOP_RIGHT,WidgetAnchor.BOTTOM_LEFT,WidgetAnchor.BOTTOM_RIGHT).forEach{anchor->FilterChip(selected=clock.anchor==anchor,onClick={clock=clock.copy(anchor=anchor);FoldbackWidgetState.configureClock(clock,context)},label={Text(anchor.name.replace('_',' '),maxLines=1)})}} }}
    Card { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("SLIDING ALERT",fontWeight=FontWeight.Black);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Show alert");Switch(alert.enabled,{alert=alert.copy(enabled=it);FoldbackWidgetState.configureAlert(alert,context)})};OutlinedTextField(alert.message,{alert=alert.copy(message=it);FoldbackWidgetState.configureAlert(alert,context)},Modifier.fillMaxWidth(),label={Text("Message")},minLines=2);Text("Cycles: ${alert.cycleCount}");Slider(alert.cycleCount.toFloat(),{alert=alert.copy(cycleCount=it.toInt());FoldbackWidgetState.configureAlert(alert,context)},valueRange=1f..20f,steps=18);Text("Speed: ${alert.speedPercent}%");Slider(alert.speedPercent.toFloat(),{alert=alert.copy(speedPercent=it.toInt());FoldbackWidgetState.configureAlert(alert,context)},valueRange=5f..100f);Text("Font size: ${alert.fontSizeSp}");Slider(alert.fontSizeSp.toFloat(),{alert=alert.copy(fontSizeSp=it.toInt());FoldbackWidgetState.configureAlert(alert,context)},valueRange=14f..80f);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(alert.direction==AlertDirection.RIGHT_TO_LEFT,{alert=alert.copy(direction=AlertDirection.RIGHT_TO_LEFT);FoldbackWidgetState.configureAlert(alert,context)},label={Text("← RIGHT TO LEFT")});FilterChip(alert.direction==AlertDirection.LEFT_TO_RIGHT,{alert=alert.copy(direction=AlertDirection.LEFT_TO_RIGHT);FoldbackWidgetState.configureAlert(alert,context)},label={Text("LEFT TO RIGHT →")})};Text("Background opacity: ${alert.backgroundOpacityPercent}%");Slider(alert.backgroundOpacityPercent.toFloat(),{alert=alert.copy(backgroundOpacityPercent=it.toInt());FoldbackWidgetState.configureAlert(alert,context)},valueRange=0f..100f);Text("Alert remains a foldback overlay; it does not replace the main congregation slide.",style=MaterialTheme.typography.bodySmall) }}
}
