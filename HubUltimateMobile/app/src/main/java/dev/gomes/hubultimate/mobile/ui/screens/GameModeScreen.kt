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
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import dev.gomes.hubultimate.mobile.core.GameMode
import dev.gomes.hubultimate.mobile.core.Perms
import dev.gomes.hubultimate.mobile.core.Prefs
import dev.gomes.hubultimate.mobile.core.SystemInfo
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
fun GameModeScreen() {
    val ctx = LocalContext.current
    val resume = LocalResumeTick.current
    val active by GameMode.active.collectAsState()
    val canDnd = remember(resume) { GameMode.canDnd(ctx) }
    val powerSave = remember(resume) { SystemInfo.isPowerSave(ctx) }
    var dnd by remember { mutableStateOf(Prefs.gameDnd(ctx)) }
    var sync by remember { mutableStateOf(Prefs.gamePauseSync(ctx)) }

    ScreenColumn("Modo jogo", "Mais desempenho e menos interrupções") {
        HubCard {
            CardTitle("Modo jogo", trailing = {
                if (active) Badge("Ativo", HubColors.Accent) else Badge("Desligado", HubColors.Muted)
            })
            BodyText("Liga sozinho quando você abre um jogo pelo botão JOGAR ou Boost, e volta tudo ao normal quando você toca em Encerrar.")
            SettingSwitch(
                "Não perturbe durante o jogo",
                "Silencia notificações que tiram o foco e podem travar o jogo. Alarmes e chamadas permitidas continuam.",
                dnd,
            ) { dnd = it; Prefs.setGameDnd(ctx, it) }
            if (dnd && !canDnd) {
                Note("Libere \"Acesso ao Não perturbe\" para o Hub.")
                SecondaryButton("Liberar Não perturbe", { Perms.open(ctx, GameMode.dndIntent()) })
            }
            SettingSwitch(
                "Pausar sincronização automática",
                "Para backups, e-mail e fotos de usarem a internet em segundo plano. Ajuda a evitar picos de ping.",
                sync,
            ) { sync = it; Prefs.setGamePauseSync(ctx, it) }
            if (active) {
                PrimaryButton("Encerrar modo jogo", { GameMode.stop(ctx) })
            } else {
                SecondaryButton("Testar agora", { GameMode.start(ctx, dnd, sync) })
            }
        }

        if (powerSave) {
            Note("Economia de energia está ligada: ela reduz o desempenho e pode travar a tela em 60 Hz. Desligue antes de jogar.")
        }

        HubCard {
            CardTitle("Mais desempenho no jogo")
            Bullet("Use o botão JOGAR do Hub: ele faz o Boost, liga o Modo jogo e abre o jogo.")
            Bullet("Jogue com a bateria acima de 20% e longe do carregador se o celular esquentar: calor faz o Android reduzir a velocidade.")
            Bullet("Tire a capinha em partidas longas para o celular esfriar melhor.")
            Bullet("Dentro do jogo, prefira gráficos Suave/Baixo com FPS alto: a partida fica mais estável.")
            Bullet("Reinicie o celular uma vez por dia se joga muito: limpa a memória por completo.")
        }
    }
}
