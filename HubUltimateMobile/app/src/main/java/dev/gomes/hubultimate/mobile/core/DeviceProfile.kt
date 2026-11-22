/*
 * Hub Ultimate · Android
 * © 2026 Gomes.dev — Todos os direitos reservados.
 * Proibida a cópia, redistribuição ou modificação sem autorização do autor.
 */
package dev.gomes.hubultimate.mobile.core

import android.app.ActivityManager
import android.content.Context

enum class DeviceTier(val label: String) { WEAK("Básico"), MID("Intermediário"), STRONG("Avançado") }

/** Classifica o aparelho pela RAM e pelo número de núcleos (só leitura). */
object DeviceProfile {
    private const val GB = 1024L * 1024 * 1024

    fun cores(): Int = Runtime.getRuntime().availableProcessors()

    fun tier(ctx: Context): DeviceTier {
        val am = ctx.getSystemService(ActivityManager::class.java)
        val ram = SystemInfo.memory(ctx).totalBytes
        return when {
            am.isLowRamDevice || ram < 3 * GB || cores() <= 4 -> DeviceTier.WEAK
            ram < 6 * GB -> DeviceTier.MID
            else -> DeviceTier.STRONG
        }
    }

    fun isWeak(ctx: Context) = tier(ctx) == DeviceTier.WEAK
}
