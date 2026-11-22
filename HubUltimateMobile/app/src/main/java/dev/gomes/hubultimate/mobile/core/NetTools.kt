/*
 * Hub Ultimate · Android
 * © 2026 Gomes.dev — Todos os direitos reservados.
 * Proibida a cópia, redistribuição ou modificação sem autorização do autor.
 */
package dev.gomes.hubultimate.mobile.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.abs
import kotlin.math.roundToInt

data class NetInfo(
    val type: String,          // "Wi-Fi", "Dados móveis", "Cabo", "Sem conexão"
    val wifiRssi: Int? = null, // dBm
    val wifiFreqMhz: Int? = null,
    val linkMbps: Int? = null,
    val metered: Boolean = false,
) {
    val band: String? get() = wifiFreqMhz?.let {
        when {
            it >= 5925 -> "6 GHz"
            it >= 4900 -> "5 GHz"
            else -> "2,4 GHz"
        }
    }
    val signalLabel: String? get() = wifiRssi?.let {
        when {
            it >= -55 -> "Excelente"
            it >= -67 -> "Bom"
            it >= -75 -> "Regular"
            else -> "Fraco"
        }
    }
}

data class LatencyResult(
    val name: String,
    val avgMs: Int?,
    val minMs: Int?,
    val jitterMs: Int?,
    val lossPercent: Int,
)

/**
 * Diagnóstico de rede, só leitura. O teste de latência abre uma conexão TCP
 * com servidores públicos (Cloudflare, Google, Quad9) e mede o tempo de resposta.
 * Nenhum dado é enviado: a conexão é aberta e fechada em seguida.
 */
object NetTools {

    val targets = listOf(
        Triple("Cloudflare", "1.1.1.1", 443),
        Triple("Google", "8.8.8.8", 443),
        Triple("Quad9", "9.9.9.9", 443),
    )

    fun info(ctx: Context): NetInfo {
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        val net = cm.activeNetwork ?: return NetInfo("Sem conexão")
        val caps = cm.getNetworkCapabilities(net) ?: return NetInfo("Sem conexão")
        val metered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                val wi: WifiInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    caps.transportInfo as? WifiInfo
                } else {
                    @Suppress("DEPRECATION")
                    ctx.applicationContext.getSystemService(WifiManager::class.java).connectionInfo
                }
                NetInfo(
                    "Wi-Fi",
                    wi?.rssi?.takeIf { it in -126..0 },
                    wi?.frequency?.takeIf { it > 0 },
                    wi?.linkSpeed?.takeIf { it > 0 },
                    metered,
                )
            }
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetInfo("Dados móveis", metered = metered)
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetInfo("Cabo", metered = metered)
            else -> NetInfo("Outra", metered = metered)
        }
    }

    suspend fun latency(name: String, host: String, port: Int, samples: Int = 8): LatencyResult =
        withContext(Dispatchers.IO) {
            val times = mutableListOf<Int>()
            repeat(samples) {
                val t0 = SystemClock.elapsedRealtimeNanos()
                val ok = runCatching {
                    Socket().use { s -> s.connect(InetSocketAddress(host, port), 1500) }
                }.isSuccess
                if (ok) times += ((SystemClock.elapsedRealtimeNanos() - t0) / 1_000_000).toInt()
                delay(120)
            }
            val loss = ((samples - times.size) * 100f / samples).roundToInt()
            if (times.isEmpty()) return@withContext LatencyResult(name, null, null, null, loss)
            val jitter = if (times.size > 1) {
                times.zipWithNext { a, b -> abs(b - a) }.average().roundToInt()
            } else 0
            LatencyResult(name, times.average().roundToInt(), times.min(), jitter, loss)
        }

    /** Nota geral da conexão para jogos, a partir do melhor servidor medido. */
    fun verdict(best: LatencyResult?): Pair<String, String> {
        val avg = best?.avgMs ?: return "Sem resposta" to "Não foi possível medir. Verifique a internet."
        val jitter = best.jitterMs ?: 0
        return when {
            best.lossPercent > 10 -> "Instável" to "Há perda de pacotes: espere travadas e teleportes no jogo."
            avg <= 40 && jitter <= 10 -> "Ótima para jogos" to "Latência baixa e estável."
            avg <= 80 && jitter <= 25 -> "Boa para jogos" to "Deve jogar bem; picos ocasionais são possíveis."
            avg <= 130 -> "Regular" to "Pode haver atraso em jogos de tiro. Veja as dicas abaixo."
            else -> "Alta latência" to "Conexão lenta para jogos online. Veja as dicas abaixo."
        }
    }
}
