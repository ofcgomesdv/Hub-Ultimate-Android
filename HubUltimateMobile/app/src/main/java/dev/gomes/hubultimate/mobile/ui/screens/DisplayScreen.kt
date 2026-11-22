/*
 * Hub Ultimate · Android
 * © 2026 Gomes.dev — Todos os direitos reservados.
 * Proibida a cópia, redistribuição ou modificação sem autorização do autor.
 */
package dev.gomes.hubultimate.mobile.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gomes.hubultimate.mobile.core.Perms
import dev.gomes.hubultimate.mobile.core.RefreshRateManager
import dev.gomes.hubultimate.mobile.core.SystemInfo
import dev.gomes.hubultimate.mobile.overlay.MonitorOverlayService
import dev.gomes.hubultimate.mobile.overlay.OverlayState
import dev.gomes.hubultimate.mobile.ui.Badge
import dev.gomes.hubultimate.mobile.ui.BodyText
import dev.gomes.hubultimate.mobile.ui.CardTitle
import dev.gomes.hubultimate.mobile.ui.HubCard
import dev.gomes.hubultimate.mobile.ui.LocalResumeTick
import dev.gomes.hubultimate.mobile.ui.Note
import dev.gomes.hubultimate.mobile.ui.PrimaryButton
import dev.gomes.hubultimate.mobile.ui.ScreenColumn
import dev.gomes.hubultimate.mobile.ui.SecondaryButton
import dev.gomes.hubultimate.mobile.ui.rememberTicker
import dev.gomes.hubultimate.mobile.ui.theme.HubColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayScreen() {
    val ctx = LocalContext.current
    val tick = rememberTicker(1000L)
    val resume = LocalResumeTick.current

    val options = remember(resume) { RefreshRateManager.options(ctx).filter { it.rate >= 60 } }
    val compatible = options.any { it.rate > 60 }
    val maxHz = options.maxOfOrNull { it.rate } ?: 60
    val current = remember(tick, resume) { RefreshRateManager.currentRate(ctx) }
    val canOverlay = remember(resume) { Perms.canOverlay(ctx) }
    val powerSave = remember(tick, resume) { SystemInfo.isPowerSave(ctx) }
    val locked by OverlayState.lockedRate.collectAsState()
    var selected by rememberSaveable { mutableIntStateOf(maxHz) }

    ScreenColumn("Tela", "Taxa de atualização para jogos") {
        HubCard {
            CardTitle("Taxa atual", trailing = {
                if (compatible) Badge("Compatível · até $maxHz Hz", HubColors.Ok)
                else Badge("60 Hz", HubColors.Warn)
            })
            Text("$current Hz", color = HubColors.Text, fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
            BodyText("Modos suportados: " + options.joinToString(" · ") { "${it.rate} Hz" })
            locked?.let { BodyText("Travado pelo Hub em $it Hz", HubColors.Accent) }
        }

        if (!compatible) {
            HubCard {
                CardTitle("Recurso indisponível")
                BodyText(
                    "O ajuste de taxa de quadros é exclusivo para celulares com tela acima de 60 Hz. " +
                        "Seu aparelho informa no máximo 60 Hz. As outras funções (Boost, Limpeza e Monitor de FPS) continuam disponíveis.",
                )
            }
        } else {
            HubCard {
                CardTitle("Travar taxa de quadros")
                BodyText(
                    "O Hub pede ao Android para manter a tela na taxa escolhida enquanto você joga, " +
                        "evitando quedas automáticas para 60 Hz. O jogo não é alterado.",
                )
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    options.forEach { o ->
                        val sel = selected == o.rate
                        FilterChip(
                            selected = sel,
                            onClick = { selected = o.rate },
                            label = { Text("${o.rate} Hz") },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = HubColors.SurfaceHi,
                                labelColor = HubColors.Text,
                                selectedContainerColor = HubColors.Accent.copy(alpha = 0.2f),
                                selectedLabelColor = HubColors.Text,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = sel,
                                borderColor = HubColors.Border,
                                selectedBorderColor = HubColors.Accent,
                            ),
                        )
                    }
                }
                if (powerSave) Note("Economia de energia ligada: o sistema pode ignorar a trava e manter 60 Hz.")
                if (!canOverlay) {
                    Note("Permita \"Sobrepor a outros apps\" para o Hub manter a taxa durante o jogo.")
                    PrimaryButton("Conceder permissão", { Perms.open(ctx, Perms.overlayIntent(ctx)) })
                } else {
                    PrimaryButton(
                        if (locked == selected) "Travado em $selected Hz" else "Travar em $selected Hz",
                        onClick = {
                            options.firstOrNull { it.rate == selected }?.let {
                                MonitorOverlayService.lockRate(ctx, it.modeId, it.rate)
                            }
                        },
                    )
                    if (locked != null) {
                        SecondaryButton("Liberar (voltar ao padrão do sistema)", { MonitorOverlayService.unlockRate(ctx) })
                    }
                }
            }

            HubCard {
                CardTitle("Se a taxa não subir")
                BodyText(
                    "Alguns fabricantes limitam a taxa pelas configurações. Ative \"Taxa de atualização alta\", " +
                        "\"Suavidade de movimento\" ou \"Tela suave\" nas configurações de tela. " +
                        "O Android também reduz a taxa com bateria fraca ou aquecimento, e alguns jogos têm limite de FPS próprio.",
                )
                SecondaryButton("Abrir configurações de tela", {
                    Perms.open(ctx, Intent(Settings.ACTION_DISPLAY_SETTINGS))
                })
            }
        }
    }
}
