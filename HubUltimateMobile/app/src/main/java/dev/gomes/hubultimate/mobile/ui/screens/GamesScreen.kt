/*
 * Hub Ultimate · Android
 * © 2026 Gomes.dev — Todos os direitos reservados.
 * Proibida a cópia, redistribuição ou modificação sem autorização do autor.
 */
package dev.gomes.hubultimate.mobile.ui.screens

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gomes.hubultimate.mobile.core.AppEntry
import dev.gomes.hubultimate.mobile.core.AppRepository
import dev.gomes.hubultimate.mobile.core.Cleaner
import dev.gomes.hubultimate.mobile.core.GameMode
import dev.gomes.hubultimate.mobile.core.Perms
import dev.gomes.hubultimate.mobile.core.Prefs
import dev.gomes.hubultimate.mobile.core.RefreshRateManager
import dev.gomes.hubultimate.mobile.core.SystemInfo.formatBytes
import dev.gomes.hubultimate.mobile.overlay.MonitorOverlayService
import dev.gomes.hubultimate.mobile.ui.Badge
import dev.gomes.hubultimate.mobile.ui.BodyText
import dev.gomes.hubultimate.mobile.ui.CardTitle
import dev.gomes.hubultimate.mobile.ui.Footer
import dev.gomes.hubultimate.mobile.ui.HubCard
import dev.gomes.hubultimate.mobile.ui.LocalLiteMode
import dev.gomes.hubultimate.mobile.ui.ScreenHeader
import dev.gomes.hubultimate.mobile.ui.SettingSwitch
import dev.gomes.hubultimate.mobile.ui.theme.HubColors
import kotlinx.coroutines.launch

@Composable
fun GamesScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val apps by produceState<List<AppEntry>?>(null) { value = AppRepository.loadApps(ctx) }

    var showAll by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    var busyPkg by remember { mutableStateOf<String?>(null) }
    var autoKill by remember { mutableStateOf(Prefs.boostKill(ctx)) }
    var autoRate by remember { mutableStateOf(Prefs.boostMaxRate(ctx)) }
    val compatible = remember { RefreshRateManager.isCompatible(ctx) }
    val maxHz = remember { RefreshRateManager.maxRate(ctx) }

    val list = apps?.filter { (showAll || it.isGame) && it.label.contains(query.trim(), ignoreCase = true) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenHeader("Jogos", "Abra seus jogos com o Boost do Hub") }

        item {
            HubCard {
                CardTitle("Ao iniciar com Boost")
                SettingSwitch(
                    "Encerrar apps em segundo plano",
                    "Libera RAM antes de abrir o jogo.",
                    autoKill,
                ) { autoKill = it; Prefs.setBoostKill(ctx, it) }
                SettingSwitch(
                    "Travar taxa máxima da tela",
                    if (compatible) "Mantém a tela em $maxHz Hz durante o jogo."
                    else "Indisponível: sua tela é de 60 Hz.",
                    autoRate && compatible,
                    enabled = compatible,
                ) { autoRate = it; Prefs.setBoostMaxRate(ctx, it) }
                status?.let { BodyText(it, HubColors.Ok) }
            }
        }

        item {
            Column {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Buscar jogo ou app") },
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HubColors.Accent,
                    unfocusedBorderColor = HubColors.Border,
                    focusedTextColor = HubColors.Text,
                    unfocusedTextColor = HubColors.Text,
                    cursorColor = HubColors.Accent,
                    focusedContainerColor = HubColors.Surface,
                    unfocusedContainerColor = HubColors.Surface,
                    focusedPlaceholderColor = HubColors.Muted,
                    unfocusedPlaceholderColor = HubColors.Muted,
                ),
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (showAll) "Todos os apps" else "Jogos detectados",
                    color = HubColors.Muted, fontSize = 13.sp, modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { showAll = !showAll }) {
                    Text(if (showAll) "Só jogos" else "Mostrar todos", color = HubColors.Accent)
                }
            }
            }
        }

        when {
            list == null -> item {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = HubColors.Accent)
                }
            }
            list.isEmpty() -> item {
                HubCard {
                    BodyText(
                        if (!showAll) "Nenhum jogo foi detectado automaticamente. Toque em \"Mostrar todos\" e escolha o app do jogo."
                        else "Nenhum app encontrado.",
                    )
                }
            }
            else -> items(list, key = { it.packageName }) { app ->
                GameRow(app, busy = busyPkg == app.packageName) {
                    scope.launch {
                        busyPkg = app.packageName
                        status = boostAndLaunch(ctx, app, autoKill, autoRate && compatible)
                        busyPkg = null
                    }
                }
            }
        }

        item { Footer() }
    }
}

internal suspend fun boostAndLaunch(ctx: Context, app: AppEntry, kill: Boolean, lockRate: Boolean): String {
    val parts = mutableListOf<String>()
    if (kill) {
        val r = Cleaner.killBackground(ctx, exclude = setOf(app.packageName))
        parts += "${r.processed} apps processados" +
            if (r.freedBytes > 0) " (+${formatBytes(r.freedBytes)} de RAM)" else ""
    }
    if (lockRate) {
        if (Perms.canOverlay(ctx)) {
            RefreshRateManager.best(ctx)?.let {
                MonitorOverlayService.lockRate(ctx, it.modeId, it.rate)
                parts += "tela em ${it.rate} Hz"
            }
        } else {
            parts += "taxa não travada (permita a sobreposição na aba Tela)"
        }
    }
    if (Prefs.gameDnd(ctx) || Prefs.gamePauseSync(ctx)) {
        val gm = GameMode.start(ctx, Prefs.gameDnd(ctx), Prefs.gamePauseSync(ctx))
        if (gm.isNotEmpty()) parts += "modo jogo: " + gm.joinToString(", ")
    }
    if (!AppRepository.launch(ctx, app.packageName)) parts += "não foi possível abrir ${app.label}"
    return if (parts.isEmpty()) "Abrindo ${app.label}" else "${app.label}: " + parts.joinToString(" · ")
}

@Composable
private fun GameRow(app: AppEntry, busy: Boolean, onBoost: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(HubColors.Surface)
            .border(1.dp, HubColors.Border, shape)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val ctx = LocalContext.current
        val lite = LocalLiteMode.current
        val icon by produceState<ImageBitmap?>(null, app.packageName, lite) {
            value = AppRepository.icon(ctx, app.packageName, lite)
        }
        val bmp = icon
        if (bmp != null) {
            Image(bmp, contentDescription = null, modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)))
        } else {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(HubColors.SurfaceHi))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                app.label, color = HubColors.Text, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            if (app.isGame) Badge("Jogo", HubColors.Accent)
            else Text(app.packageName, color = HubColors.Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = onBoost,
            enabled = !busy,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = HubColors.Accent, contentColor = HubColors.OnAccent,
                disabledContainerColor = HubColors.SurfaceHi, disabledContentColor = HubColors.Muted,
            ),
        ) { Text(if (busy) "..." else "JOGAR", fontWeight = FontWeight.Bold) }
    }
}
