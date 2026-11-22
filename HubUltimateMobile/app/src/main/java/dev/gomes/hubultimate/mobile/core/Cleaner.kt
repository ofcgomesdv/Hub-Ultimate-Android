/*
 * Hub Ultimate · Android
 * © 2026 Gomes.dev — Todos os direitos reservados.
 * Proibida a cópia, redistribuição ou modificação sem autorização do autor.
 */
package dev.gomes.hubultimate.mobile.core

import android.Manifest
import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.os.storage.StorageManager
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

data class CleanResult(val processed: Int, val freedBytes: Long)
data class AppCache(val packageName: String, val label: String, val cacheBytes: Long)

object Cleaner {

    /** Launcher, teclado e o próprio Hub nunca são encerrados. */
    private fun protectedPackages(ctx: Context): Set<String> {
        val s = mutableSetOf(ctx.packageName)
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        ctx.packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName?.let(s::add)
        Settings.Secure.getString(ctx.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.substringBefore('/')?.let(s::add)
        return s
    }

    /**
     * Usa ActivityManager.killBackgroundProcesses: o Android só encerra processos
     * que já estão parados em segundo plano (em cache). Apps em uso, serviços
     * essenciais e o sistema não são afetados.
     */
    suspend fun killBackground(ctx: Context, exclude: Set<String> = emptySet()): CleanResult =
        withContext(Dispatchers.IO) {
            val am = ctx.getSystemService(ActivityManager::class.java)
            val before = SystemInfo.memory(ctx).availBytes
            val targets = AppRepository.launchablePackages(ctx) - exclude - protectedPackages(ctx)
            targets.forEach { runCatching { am.killBackgroundProcesses(it) } }
            delay(900) // dá tempo do sistema recolher a memória
            val after = SystemInfo.memory(ctx).availBytes
            CleanResult(targets.size, (after - before).coerceAtLeast(0))
        }

    /** Apaga apenas o cache do próprio Hub Ultimate. */
    suspend fun clearOwnCache(ctx: Context): Long = withContext(Dispatchers.IO) {
        var total = 0L
        listOfNotNull(ctx.cacheDir, ctx.externalCacheDir).forEach { dir ->
            dir.listFiles()?.forEach { f ->
                total += f.walkBottomUp().filter { it.isFile }.sumOf { it.length() }
                f.deleteRecursively()
            }
        }
        total
    }

    fun hasUsageAccess(ctx: Context): Boolean {
        val aom = ctx.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            aom.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
        } else {
            @Suppress("DEPRECATION")
            aom.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
        }
        return if (mode == AppOpsManager.MODE_DEFAULT) {
            ctx.checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            mode == AppOpsManager.MODE_ALLOWED
        }
    }

    /** Tamanho do cache de cada app (só leitura). Requer "Acesso ao uso". */
    suspend fun appCaches(ctx: Context): List<AppCache> = withContext(Dispatchers.IO) {
        val ssm = ctx.getSystemService(StorageStatsManager::class.java)
        val pm = ctx.packageManager
        val user = Process.myUserHandle()
        AppRepository.launchableInfos(ctx).mapNotNull { ai ->
            runCatching {
                val st = ssm.queryStatsForPackage(StorageManager.UUID_DEFAULT, ai.packageName, user)
                AppCache(ai.packageName, pm.getApplicationLabel(ai).toString(), st.cacheBytes)
            }.getOrNull()
        }.filter { it.cacheBytes > 0 }.sortedByDescending { it.cacheBytes }
    }
}
