/*
 * Hub Ultimate · Android
 * © 2026 Gomes.dev — Todos os direitos reservados.
 * Proibida a cópia, redistribuição ou modificação sem autorização do autor.
 */
package dev.gomes.hubultimate.mobile.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object Prefs {
    /** Todas as configurações ficam criptografadas (ver SecureStore). */
    private fun sp(ctx: Context) = SecureStore.of(ctx, "hub_prefs")

    fun boostKill(ctx: Context) = sp(ctx).getBoolean("boost_kill", true)
    fun setBoostKill(ctx: Context, v: Boolean) = sp(ctx).putBoolean("boost_kill", v)

    fun boostMaxRate(ctx: Context) = sp(ctx).getBoolean("boost_max_rate", true)
    fun setBoostMaxRate(ctx: Context, v: Boolean) = sp(ctx).putBoolean("boost_max_rate", v)

    /** Modo leve: ligado automaticamente na primeira abertura em celulares fracos. */
    fun liteMode(ctx: Context): Boolean {
        val p = sp(ctx)
        if (!p.contains("lite_mode")) p.putBoolean("lite_mode", DeviceProfile.isWeak(ctx))
        return p.getBoolean("lite_mode", false)
    }
    fun setLiteMode(ctx: Context, v: Boolean) = sp(ctx).putBoolean("lite_mode", v)

    fun gameDnd(ctx: Context) = sp(ctx).getBoolean("game_dnd", true)
    fun setGameDnd(ctx: Context, v: Boolean) = sp(ctx).putBoolean("game_dnd", v)

    fun gamePauseSync(ctx: Context) = sp(ctx).getBoolean("game_pause_sync", true)
    fun setGamePauseSync(ctx: Context, v: Boolean) = sp(ctx).putBoolean("game_pause_sync", v)

    fun termsAccepted(ctx: Context) = sp(ctx).getInt("terms_version", 0) >= TERMS_VERSION
    fun acceptTerms(ctx: Context) = sp(ctx).putInt("terms_version", TERMS_VERSION)
    const val TERMS_VERSION = 1

    fun monitorFps(ctx: Context) = sp(ctx).getBoolean("monitor_fps", true)
    fun setMonitorFps(ctx: Context, v: Boolean) = sp(ctx).putBoolean("monitor_fps", v)
}

object Perms {
    fun canOverlay(ctx: Context): Boolean = Settings.canDrawOverlays(ctx)

    fun overlayIntent(ctx: Context) =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}"))

    fun usageIntent() = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    fun appDetails(pkg: String) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))

    fun open(ctx: Context, intent: Intent): Boolean =
        runCatching { ctx.startActivity(intent) }.isSuccess
}
