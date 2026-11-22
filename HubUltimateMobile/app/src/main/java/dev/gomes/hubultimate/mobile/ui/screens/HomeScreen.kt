/*
 * Hub Ultimate · Android
 * © 2026 Gomes.dev — Todos os direitos reservados.
 * Proibida a cópia, redistribuição ou modificação sem autorização do autor.
 */
package dev.gomes.hubultimate.mobile.ui.screens

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gomes.hubultimate.mobile.core.AppEntry
import dev.gomes.hubultimate.mobile.core.AppRepository
import dev.gomes.hubultimate.mobile.core.Cleaner
import dev.gomes.hubultimate.mobile.core.DeviceProfile
import dev.gomes.hubultimate.mobile.core.DeviceTier
import dev.gomes.hubultimate.mobile.core.GameMode
import dev.gomes.hubultimate.mobile.core.Integrity
import dev.gomes.hubultimate.mobile.core.Perms
import dev.gomes.hubultimate.mobile.core.Prefs
import dev.gomes.hubultimate.mobile.core.RefreshRateManager
import dev.gomes.hubultimate.mobile.core.SystemInfo
import dev.gomes.hubultimate.mobile.core.SystemInfo.formatBytes
import dev.gomes.hubultimate.mobile.ui.Badge
import dev.gomes.hubultimate.mobile.ui.BodyText
import dev.gomes.hubultimate.mobile.ui.Bullet
import dev.gomes.hubultimate.mobile.ui.CardTitle
import dev.gomes.hubultimate.mobile.ui.HubCard
import dev.gomes.hubultimate.mobile.ui.LocalLiteMode
import dev.gomes.hubultimate.mobile.ui.LocalResumeTick
import dev.gomes.hubultimate.mobile.ui.Note
import dev.gomes.hubultimate.mobile.ui.ScreenColumn
import dev.gomes.hubultimate.mobile.ui.SecondaryButton
import dev.gomes.hubultimate.mobile.ui.SectionLabel
import dev.gomes.hubultimate.mobile.ui.SettingSwitch
import dev.gomes.hubultimate.mobile.ui.rememberTicker
import dev.gomes.hubultimate.mobile.ui.theme.HubColors
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HomeScreen(onOpenGames: () -> Unit, onOpenMonitor: () -> Unit, onLiteChange: (Boolean) -> Unit) {
    val ctx = LocalContext.current
    val tick = rememberTicker()
    val resume = LocalResumeTick.current
    val scope = rememberCoroutineScope()
    val lite = LocalLiteMode.current

    val mem = remember(tick, resume) { SystemInfo.memory(ctx) }
    val storage = remember(tick / 5, resume) { SystemInfo.storage() }
    val temp = remember(tick, resume) { SystemInfo.batteryTempC(ctx) }
    val level = remember(tick, resume) { SystemInfo.batteryLevel(ctx) }
    val powerSave = remember(tick, resume) { SystemInfo.isPowerSave(ctx) }
    val curHz = remember(tick, resume) { RefreshRateManager.currentRate(ctx) }
    val tier = remember { DeviceProfile.tier(ctx) }
    val cores = remember { DeviceProfile.cores() }
    val games by produceState<List<AppEntry>?>(null) {
        value = AppRepository.loadApps(ctx).filter { it.isGame }.take(3)
    }

    val gameModeOn by GameMode.active.collectAsState()
    val official = remember { Integrity.isOfficial(ctx) || Integrity.isDevBuild() }
    var busy by remember { mutableStateOf(false) }
    var busyPkg by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<String?>(null) }

    ScreenColumn(title = "", subtitle = "") {
        // Cabeçalho: "HUB ULTIMATE" com destaque neon + taxa atual da tela
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                buildAnnotatedString {
                    append("HUB ")
                    withStyle(SpanStyle(color = HubColors.Accent)) { append("ULTIMATE") }
                },
                color = HubColors.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp, modifier = Modifier.weight(1f),
            )
            Text(
                "$curHz Hz", color = HubColors.Accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .border(1.dp, HubColors.Accent, RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }

        // Botão BOOST redondo
        Column(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(244.dp)
                    .border(2.dp, HubColors.Accent, CircleShape)
                    .padding(12.dp)
                    .shadow(if (lite) 0.dp else 24.dp, CircleShape, ambientColor = HubColors.Accent, spotColor = HubColors.Accent)
                    .clip(CircleShape)
                    .background(HubColors.Surface)
                    .clickable(enabled = !busy, role = Role.Button, onClickLabel = "Ativar Boost") {
                        scope.launch {
                            busy = true
                            val r = Cleaner.killBackground(ctx)
                            val c = Cleaner.clearOwnCache(ctx)
                            result = "${r.processed} apps encerrados · +${formatBytes(r.freedBytes)} de RAM · cache: ${formatBytes(c)}"
                            busy = false
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Bolt, contentDescription = null, tint = HubColors.Accent, modifier = Modifier.size(44.dp))
                    Text(
                        if (busy) "..." else "BOOST", color = HubColors.Text, fontSize = 34.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 2.sp,
                    )
                    Text(
                        if (busy) "Otimizando" else "Toque para otimizar",
                        color = HubColors.Muted, fontSize = 12.sp,
                    )
                }
            }
            Text(
                "RAM livre: ${formatBytes(mem.availBytes)} · " +
                    (temp?.let { String.format(Locale.getDefault(), "%.0f °C", it) } ?: "—"),
                color = HubColors.Muted, fontSize = 13.sp,
            )
            result?.let { Text(it, color = HubColors.Accent, fontSize = 12.sp, textAlign = TextAlign.Center) }
        }

        if (!official) {
            Note("Atenção: esta cópia do Hub Ultimate não é a versão oficial da Gomes.dev e pode ter sido modificada. Baixe o app oficial.", HubColors.Bad)
        }

        if (gameModeOn) {
            HubCard {
                CardTitle("Modo jogo ativo", trailing = { Badge("ON", HubColors.Accent) })
                BodyText("Não perturbe e/ou sincronização pausada. Encerre quando terminar de jogar.")
                SecondaryButton("Encerrar modo jogo", { GameMode.stop(ctx) })
            }
        }

        // Três números principais
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NeonStat("${(mem.usedFraction * 100).roundToInt()}%", "RAM", Modifier.weight(1f), highlight = true)
            NeonStat("$level%", "Bateria", Modifier.weight(1f))
            NeonStat("${(storage.usedFraction * 100).roundToInt()}%", "Armaz.", Modifier.weight(1f))
        }
        if (powerSave) {
            Note("Economia de energia ligada: o Android pode limitar a tela a 60 Hz e reduzir o desempenho nos jogos.")
        }

        // Jogos com botão JOGAR
        SectionLabel("Jogar com Boost")
        val list = games
        when {
            list == null -> BodyText("Procurando jogos...")
            list.isEmpty() -> BodyText("Nenhum jogo detectado. Escolha o app na aba Jogos.")
            else -> list.forEach { app ->
                QuickGameRow(app, busy = busyPkg == app.packageName) {
                    scope.launch {
                        busyPkg = app.packageName
                        result = boostAndLaunch(
                            ctx, app, Prefs.boostKill(ctx),
                            Prefs.boostMaxRate(ctx) && RefreshRateManager.isCompatible(ctx),
                        )
                        busyPkg = null
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton("Todos os jogos", onOpenGames, Modifier.weight(1f))
            SecondaryButton("Mostrar FPS", onOpenMonitor, Modifier.weight(1f))
        }

        // Desempenho do aparelho + Modo leve
        HubCard {
            CardTitle("Desempenho do aparelho", trailing = {
                Badge(
                    tier.label,
                    when (tier) {
                        DeviceTier.WEAK -> HubColors.Warn
                        DeviceTier.MID -> HubColors.Text
                        DeviceTier.STRONG -> HubColors.Accent
                    },
                )
            })
            BodyText("${formatBytes(mem.totalBytes)} de RAM · $cores núcleos")
            SettingSwitch(
                "Modo leve",
                "Atualiza menos vezes, ícones menores, sem brilho no botão e FPS mais econômico. Ideal para celulares básicos.",
                lite,
                onChange = onLiteChange,
            )
            if (tier == DeviceTier.WEAK) {
                BodyText("Dicas para celular básico:", HubColors.Text)
                Bullet("Use o Boost antes de jogar: libera RAM, o principal gargalo.")
                Bullet("No jogo, gráficos Baixo/Suave e FPS 30 ou 40: mais estável e esquenta menos.")
                Bullet("Deixe pelo menos 2 GB livres no armazenamento.")
                Bullet("Animações do sistema em 0,5x nas Opções do desenvolvedor.")
                SecondaryButton("Abrir opções do desenvolvedor", {
                    if (!Perms.open(ctx, Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))) {
                        Toast.makeText(
                            ctx,
                            "Ative antes: Configurações › Sobre o telefone › toque 7x em \"Número da versão\".",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                })
            }
        }
    }
}

@Composable
private fun NeonStat(value: String, label: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier
            .clip(shape)
            .background(HubColors.Surface)
            .border(1.dp, HubColors.Border, shape)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            value, color = if (highlight) HubColors.Accent else HubColors.Text,
            fontSize = 22.sp, fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(2.dp))
        Text(label, color = HubColors.Muted, fontSize = 11.sp)
    }
}

@Composable
private fun QuickGameRow(app: AppEntry, busy: Boolean, onPlay: () -> Unit) {
    val ctx = LocalContext.current
    val lite = LocalLiteMode.current
    val icon by produceState<ImageBitmap?>(null, app.packageName, lite) {
        value = AppRepository.icon(ctx, app.packageName, lite)
    }
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(HubColors.Surface)
            .border(1.dp, HubColors.Border, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val bmp = icon
        if (bmp != null) {
            Image(bmp, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)))
        } else {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(HubColors.SurfaceHi))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            app.label, color = HubColors.Text, fontSize = 14.sp, fontWeight = FontWeight.Medium,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = onPlay,
            enabled = !busy,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = HubColors.Accent, contentColor = HubColors.OnAccent,
                disabledContainerColor = HubColors.SurfaceHi, disabledContentColor = HubColors.Muted,
            ),
        ) { Text(if (busy) "..." else "JOGAR", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
    }
}
