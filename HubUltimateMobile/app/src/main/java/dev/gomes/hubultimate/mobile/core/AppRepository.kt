/*
 * Hub Ultimate · Android
 * © 2026 Gomes.dev — Todos os direitos reservados.
 * Proibida a cópia, redistribuição ou modificação sem autorização do autor.
 */
package dev.gomes.hubultimate.mobile.core

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppEntry(
    val packageName: String,
    val label: String,
    val isGame: Boolean,
)

object AppRepository {

    /** Cache de ícones limitado (~4 MB): evita carregar todos os ícones na RAM de uma vez. */
    private val iconCache = object : LruCache<String, ImageBitmap>(4 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }

    /** Carrega o ícone sob demanda; no modo leve usa uma versão menor. */
    suspend fun icon(ctx: Context, pkg: String, lite: Boolean): ImageBitmap? {
        val size = if (lite) 48 else 96
        val key = "$pkg@$size"
        iconCache.get(key)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                ctx.packageManager.getApplicationIcon(pkg).toBitmap(size, size).asImageBitmap()
            }.getOrNull()?.also { iconCache.put(key, it) }
        }
    }

    fun trimMemory() {
        iconCache.evictAll()
        cachedApps = null
    }

    @Suppress("DEPRECATION")
    private fun launcherActivities(ctx: Context): List<ResolveInfo> {
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return ctx.packageManager.queryIntentActivities(i, 0)
    }

    fun launchablePackages(ctx: Context): Set<String> =
        launcherActivities(ctx).map { it.activityInfo.packageName }.toSet()

    fun launchableInfos(ctx: Context): List<ApplicationInfo> =
        launcherActivities(ctx).map { it.activityInfo.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName != ctx.packageName }

    @Suppress("DEPRECATION")
    fun isGame(ai: ApplicationInfo): Boolean =
        ai.category == ApplicationInfo.CATEGORY_GAME ||
            (ai.flags and ApplicationInfo.FLAG_IS_GAME) != 0

    @Volatile private var cachedApps: List<AppEntry>? = null
    @Volatile private var cachedAt = 0L

    /** Lista guardada por 2 minutos: trocar de aba não relê todos os apps do sistema. */
    suspend fun loadApps(ctx: Context): List<AppEntry> {
        val now = android.os.SystemClock.elapsedRealtime()
        cachedApps?.let { if (now - cachedAt < 120_000) return it }
        return readApps(ctx).also { cachedApps = it; cachedAt = now }
    }

    private suspend fun readApps(ctx: Context): List<AppEntry> = withContext(Dispatchers.IO) {
        val pm = ctx.packageManager
        launcherActivities(ctx)
            .distinctBy { it.activityInfo.packageName }
            .filter { it.activityInfo.packageName != ctx.packageName }
            .map { ri ->
                val ai = ri.activityInfo.applicationInfo
                AppEntry(
                    packageName = ai.packageName,
                    label = ri.loadLabel(pm).toString(),
                    isGame = isGame(ai),
                )
            }
            .sortedWith(compareByDescending<AppEntry> { it.isGame }.thenBy { it.label.lowercase() })
    }

    fun launch(ctx: Context, pkg: String): Boolean {
        val i = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { ctx.startActivity(i) }.isSuccess
    }
}
