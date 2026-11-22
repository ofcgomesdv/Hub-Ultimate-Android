/*
 * Hub Ultimate · Android
 * © 2026 Gomes.dev — Todos os direitos reservados.
 * Proibida a cópia, redistribuição ou modificação sem autorização do autor.
 */
package dev.gomes.hubultimate.mobile.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import dev.gomes.hubultimate.mobile.core.Perms
import dev.gomes.hubultimate.mobile.core.Prefs
import dev.gomes.hubultimate.mobile.overlay.MonitorOverlayService
import dev.gomes.hubultimate.mobile.overlay.OverlayState
import dev.gomes.hubultimate.mobile.ui.Badge
import dev.gomes.hubultimate.mobile.ui.BodyText
import dev.gomes.hubultimate.mobile.ui.Bullet
import dev.gomes.hubultimate.mobile.ui.CardTitle
import dev.gomes.hubultimate.mobile.ui.HubCard
import dev.gomes.hubultimate.mobile.ui.LocalResumeTick
import dev.gomes.hubultimate.mobile.ui.Note
import dev.gomes.hubultimate.mobile.ui.PrimaryButton
import dev.gomes.hubultimate.mobile.ui.ScreenColumn
import dev.gomes.hubultimate.mobile.ui.SecondaryButton
import dev.gomes.hubultimate.mobile.ui.SettingSwitch
import dev.gomes.hubultimate.mobile.ui.theme.HubColors

@Composable
fun MonitorScreen(onRequestFps: () -> Unit) {
    val ctx = LocalContext.current
    val resume = LocalResumeTick.current
    val monitorOn by OverlayState.monitorOn.collectAsState()
    val fpsOn by OverlayState.fpsOn.collectAsState()
    val canOverlay = remember(resume) { Perms.canOverlay(ctx) }
    var wantFps by rememberSaveable { mutableStateOf(Prefs.monitorFps(ctx)) }

    ScreenColumn("Monitor", "FPS e desempenho por cima do jogo") {
        HubCard {
            CardTitle("Monitor na tela", trailing = {
                when {
                    monitorOn && fpsOn -> Badge("Ativo · FPS", HubColors.Ok)
                    monitorOn -> Badge("Ativo", HubColors.Ok)
                    else -> Badge("Desligado", HubColors.Muted)
                }
            })
            BodyText("Uma pequena janela flutuante (pode arrastar) mostra em tempo real:")
            Bullet("FPS — quadros novos exibidos por segundo")
            Bullet("Hz — taxa atual da tela")
            Bullet("RAM em uso e temperatura da bateria")
            SettingSwitch(
                "Mostrar FPS",
                "Usa a captura de tela do Android só para contar quadros. Nenhuma imagem é salva ou enviada.",
                wantFps,
                enabled = !monitorOn,
            ) { wantFps = it; Prefs.setMonitorFps(ctx, it) }

            when {
                !canOverlay -> {
                    Note("Permita \"Sobrepor a outros apps\" para o monitor aparecer em cima do jogo.")
                    PrimaryButton("Conceder permissão", { Perms.open(ctx, Perms.overlayIntent(ctx)) })
                }
                !monitorOn -> PrimaryButton("Ligar monitor", {
                    if (wantFps) onRequestFps() else MonitorOverlayService.startMonitor(ctx, 0, null)
                })
                else -> SecondaryButton("Desligar monitor", { MonitorOverlayService.stopMonitor(ctx) })
            }
        }

        HubCard {
            CardTitle("Como o FPS é medido")
            BodyText(
                "O Hub não lê, não altera e não injeta nada no jogo. Ele conta quantas vezes a imagem da tela " +
                    "muda por segundo — como um gravador de tela, só que sem gravar. Por isso o valor é uma " +
                    "estimativa bem próxima do FPS real.",
            )
            Bullet("Ao ligar, o Android pede autorização e mostra o aviso de compartilhamento de tela. Escolha \"Tela inteira\".")
            Bullet("Em menus parados o número cai, porque nada novo é desenhado — isso é normal.")
            Bullet("Para desligar tudo, use o botão acima ou \"Desligar\" na notificação.")
        }
    }
}
