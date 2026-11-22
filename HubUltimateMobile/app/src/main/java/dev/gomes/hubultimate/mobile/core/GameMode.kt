/*
 * Hub Ultimate · Android
 * © 2026 Gomes.dev — Todos os direitos reservados.
 * Proibida a cópia, redistribuição ou modificação sem autorização do autor.
 */
package dev.gomes.hubultimate.mobile.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.gomes.hubultimate.mobile.R
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Modo jogo: enquanto você joga, o Hub
 *  - liga o "Não perturbe" (só prioridades: alarmes e chamadas que você permitiu);
 *  - pausa a sincronização automática das contas (e-mail, fotos, backup), que usa
 *    internet em segundo plano e pode causar picos de ping.
 * Tudo volta como estava ao tocar em "Encerrar" na notificação ou no Hub.
 * Nada disso mexe no jogo.
 */
object GameMode {
    private const val CHANNEL = "hub_game_mode"
    private const val NOTIF_ID = 2001
    const val ACTION_STOP = "dev.gomes.hubultimate.mobile.STOP_GAME_MODE"

    val active = MutableStateFlow(false)

    /** Estado do Modo jogo (o que restaurar depois) guardado criptografado. */
    private fun sp(ctx: Context) = SecureStore.of(ctx, "hub_game_mode")

    fun refresh(ctx: Context) { active.value = sp(ctx).getBoolean("active", false) }

    fun canDnd(ctx: Context): Boolean =
        ctx.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted

    fun dndIntent() = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)

    /** Liga o Modo jogo. Retorna o que foi aplicado, para mostrar ao usuário. */
    fun start(ctx: Context, dnd: Boolean, pauseSync: Boolean): List<String> {
        val done = mutableListOf<String>()
        val p = sp(ctx)
        val alreadyActive = p.getBoolean("active", false)
        val nm = ctx.getSystemService(NotificationManager::class.java)

        if (dnd && canDnd(ctx)) {
            if (!alreadyActive || !p.contains("prev_filter")) p.putInt("prev_filter", nm.currentInterruptionFilter)
            runCatching { nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY) }
                .onSuccess { done += "Não perturbe ligado" }
        }
        if (pauseSync) {
            runCatching {
                if (!alreadyActive || !p.contains("prev_sync")) {
                    p.putBoolean("prev_sync", ContentResolver.getMasterSyncAutomatically())
                }
                ContentResolver.setMasterSyncAutomatically(false)
            }.onSuccess { done += "sincronização pausada" }
        }
        if (done.isNotEmpty()) {
            p.putBoolean("active", true)
            showNotification(ctx)
            active.value = true
        }
        return done
    }

    /** Desliga o Modo jogo e devolve tudo como estava antes. */
    fun stop(ctx: Context) {
        val p = sp(ctx)
        if (p.contains("prev_filter")) {
            val nm = ctx.getSystemService(NotificationManager::class.java)
            if (canDnd(ctx)) runCatching { nm.setInterruptionFilter(p.getInt("prev_filter", NotificationManager.INTERRUPTION_FILTER_ALL)) }
        }
        if (p.contains("prev_sync")) {
            runCatching { ContentResolver.setMasterSyncAutomatically(p.getBoolean("prev_sync", true)) }
        }
        p.clear()
        NotificationManagerCompat.from(ctx).cancel(NOTIF_ID)
        active.value = false
    }

    private fun showNotification(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Modo jogo", NotificationManager.IMPORTANCE_LOW))
        val stop = PendingIntent.getBroadcast(
            ctx, 3, Intent(ctx, GameModeReceiver::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Modo jogo ativo")
            .setContentText("Não perturbe e sincronização pausada. Toque em Encerrar ao terminar de jogar.")
            .setSubText("© Gomes.dev")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, "Encerrar", stop)
            .build()
        runCatching { NotificationManagerCompat.from(ctx).notify(NOTIF_ID, n) }
    }
}

class GameModeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == GameMode.ACTION_STOP) GameMode.stop(context)
    }
}
