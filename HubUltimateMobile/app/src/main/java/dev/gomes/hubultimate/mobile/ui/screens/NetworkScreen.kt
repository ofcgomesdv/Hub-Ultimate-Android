/*
 * Hub Ultimate · Android
 * © 2026 Gomes.dev — Todos os direitos reservados.
 * Proibida a cópia, redistribuição ou modificação sem autorização do autor.
 */
package dev.gomes.hubultimate.mobile.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gomes.hubultimate.mobile.core.LatencyResult
import dev.gomes.hubultimate.mobile.core.NetTools
import dev.gomes.hubultimate.mobile.core.Perms
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
import dev.gomes.hubultimate.mobile.ui.StatTile
import dev.gomes.hubultimate.mobile.ui.rememberTicker
import dev.gomes.hubultimate.mobile.ui.theme.HubColors
import kotlinx.coroutines.launch

@Composable
fun NetworkScreen(onOpenGameMode: () -> Unit) {
    val ctx = LocalContext.current
    val tick = rememberTicker(3000L)
    val resume = LocalResumeTick.current
    val scope = rememberCoroutineScope()
    val net = remember(tick, resume) { NetTools.info(ctx) }

    var testing by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<LatencyResult>>(emptyList()) }
    val best = results.filter { it.avgMs != null }.minByOrNull { it.avgMs!! }

    ScreenColumn("Rede", "Ping, estabilidade e Wi-Fi") {
        HubCard {
            CardTitle("Conexão", trailing = { Badge(net.type, if (net.type == "Sem conexão") HubColors.Bad else HubColors.Accent) })
            if (net.type == "Wi-Fi") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Sinal", net.signalLabel ?: "—", Modifier.weight(1f), signalColor(net.wifiRssi))
                    StatTile("Faixa", net.band ?: "—", Modifier.weight(1f))
                }
                BodyText(
                    listOfNotNull(
                        net.wifiRssi?.let { "$it dBm" },
                        net.linkMbps?.let { "velocidade do link: $it Mbps" },
                    ).joinToString(" · "),
                )
                if (net.band == "2,4 GHz") {
                    Note("Você está no Wi-Fi 2,4 GHz. Se o roteador tiver rede 5 GHz, use-a para jogar: menos interferência e ping mais estável.")
                }
                if ((net.wifiRssi ?: 0) < -70) {
                    Note("Sinal fraco: chegue mais perto do roteador ou tire obstáculos entre vocês.")
                }
            } else if (net.type == "Dados móveis") {
                BodyText("Em dados móveis o ping varia com o sinal da operadora. Se puder, jogue no Wi-Fi 5 GHz.")
            }
        }

        HubCard {
            CardTitle("Teste de ping")
            BodyText("Mede o tempo de resposta e a estabilidade da sua internet até servidores rápidos no mundo todo.")
            PrimaryButton(
                if (testing) "Testando..." else "Testar ping",
                enabled = !testing && net.type != "Sem conexão",
                onClick = {
                    scope.launch {
                        testing = true
                        results = emptyList()
                        val out = mutableListOf<LatencyResult>()
                        for ((name, host, port) in NetTools.targets) {
                            out += NetTools.latency(name, host, port)
                            results = out.toList()
                        }
                        testing = false
                    }
                },
            )
            if (results.isNotEmpty()) {
                val (title, detail) = NetTools.verdict(best)
                Text(title, color = verdictColor(title), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                BodyText(detail)
                results.forEachIndexed { i, r ->
                    if (i > 0) HorizontalDivider(color = HubColors.Border)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(r.name, color = HubColors.Text, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text(
                            if (r.avgMs == null) "sem resposta"
                            else "${r.avgMs} ms · jitter ${r.jitterMs} ms · perda ${r.lossPercent}%",
                            color = HubColors.Muted, fontSize = 12.sp,
                        )
                    }
                }
            }
            Note(
                "Este teste mostra a qualidade da SUA conexão. O ping dentro do jogo depende também da distância até o servidor do jogo. " +
                    "Nenhum app consegue baixar esse ping: os \"redutores de ping\" não funcionam ou usam VPN, que pode até gerar banimento.",
                HubColors.Muted,
            )
        }

        HubCard {
            CardTitle("Como melhorar o ping")
            Bullet("Escolha no jogo o servidor/região mais próximo (América do Sul / Brasil).")
            Bullet("Use Wi-Fi 5 GHz e fique perto do roteador. Paredes e micro-ondas atrapalham o 2,4 GHz.")
            Bullet("Pause downloads, atualizações da Play Store e vídeos em outros aparelhos da casa durante a partida.")
            Bullet("Ligue o Modo jogo: ele pausa a sincronização em segundo plano, que causa picos de ping.")
            Bullet("Jitter alto (acima de 20 ms) dá mais travadas que ping alto: reinicie o roteador se ficar assim.")
            Bullet("Não use VPN para jogar: aumenta o ping e é proibida em vários jogos.")
            SecondaryButton("Configurar Modo jogo", onOpenGameMode)
            SecondaryButton("Abrir configurações de Wi-Fi", { Perms.open(ctx, Intent(Settings.ACTION_WIFI_SETTINGS)) })
        }
    }
}

private fun signalColor(rssi: Int?) = when {
    rssi == null -> HubColors.Text
    rssi >= -67 -> HubColors.Accent
    rssi >= -75 -> HubColors.Warn
    else -> HubColors.Bad
}

private fun verdictColor(title: String) = when (title) {
    "Ótima para jogos", "Boa para jogos" -> HubColors.Accent
    "Regular" -> HubColors.Warn
    else -> HubColors.Bad
}
