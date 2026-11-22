/*
 * Hub Ultimate · Android
 * © 2026 Gomes.dev — Todos os direitos reservados.
 * Proibida a cópia, redistribuição ou modificação sem autorização do autor.
 */
package dev.gomes.hubultimate.mobile.core

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import java.util.Locale

data class MemInfo(val totalBytes: Long, val availBytes: Long, val lowMemory: Boolean) {
    val usedBytes: Long get() = totalBytes - availBytes
    val usedFraction: Float get() = if (totalBytes <= 0) 0f else usedBytes.toFloat() / totalBytes
}

data class StorageInfo(val totalBytes: Long, val freeBytes: Long) {
    val usedBytes: Long get() = totalBytes - freeBytes
    val usedFraction: Float get() = if (totalBytes <= 0) 0f else usedBytes.toFloat() / totalBytes
}

/** Leitura de informações do aparelho. Somente leitura: nada aqui altera o sistema. */
object SystemInfo {

    fun memory(ctx: Context): MemInfo {
        val am = ctx.getSystemService(ActivityManager::class.java)
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        return MemInfo(mi.totalMem, mi.availMem, mi.lowMemory)
    }

    fun storage(): StorageInfo {
        val s = StatFs(Environment.getDataDirectory().path)
        return StorageInfo(s.totalBytes, s.availableBytes)
    }

    fun batteryTempC(ctx: Context): Float? {
        val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return null
        val t = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        return if (t == Int.MIN_VALUE) null else t / 10f
    }

    fun batteryLevel(ctx: Context): Int {
        val bm = ctx.getSystemService(BatteryManager::class.java)
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    fun isPowerSave(ctx: Context): Boolean =
        ctx.getSystemService(PowerManager::class.java).isPowerSaveMode

    /** Estado térmico informado pelo Android (10+). */
    fun thermalLabel(ctx: Context): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val pm = ctx.getSystemService(PowerManager::class.java)
        return when (pm.currentThermalStatus) {
            PowerManager.THERMAL_STATUS_NONE -> "Normal"
            PowerManager.THERMAL_STATUS_LIGHT -> "Leve"
            PowerManager.THERMAL_STATUS_MODERATE -> "Moderado"
            PowerManager.THERMAL_STATUS_SEVERE -> "Alto"
            else -> "Crítico"
        }
    }

    fun formatBytes(b: Long): String {
        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024
        val l = Locale.getDefault()
        return when {
            b >= gb -> String.format(l, "%.1f GB", b / gb)
            b >= mb -> String.format(l, "%.0f MB", b / mb)
            else -> String.format(l, "%.0f KB", b / kb)
        }
    }
}
