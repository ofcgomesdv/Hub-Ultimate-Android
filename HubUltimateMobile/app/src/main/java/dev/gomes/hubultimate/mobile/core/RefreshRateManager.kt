/*
 * Hub Ultimate · Android
 * © 2026 Gomes.dev — Todos os direitos reservados.
 * Proibida a cópia, redistribuição ou modificação sem autorização do autor.
 */
package dev.gomes.hubultimate.mobile.core

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
import kotlin.math.roundToInt

data class RateOption(val rate: Int, val modeId: Int)

/**
 * Lê os modos de tela suportados.
 * A trava em si é feita pelo MonitorOverlayService: uma janela invisível de 1 px
 * que pede ao Android o modo escolhido (preferredDisplayModeId). É a mesma API
 * pública que qualquer app usa para pedir uma taxa — sem root, sem ADB e sem
 * tocar no jogo.
 */
object RefreshRateManager {

    private fun display(ctx: Context): Display =
        ctx.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)

    /** Taxas suportadas na resolução atual, uma por valor (ex.: 60, 90, 120). */
    fun options(ctx: Context): List<RateOption> {
        val d = display(ctx)
        val cur = d.mode
        val all = d.supportedModes.toList()
        val sameRes = all.filter {
            it.physicalWidth == cur.physicalWidth && it.physicalHeight == cur.physicalHeight
        }.ifEmpty { all }
        return sameRes
            .groupBy { it.refreshRate.roundToInt() }
            .map { (rate, modes) -> RateOption(rate, modes.first().modeId) }
            .filter { it.rate >= 30 }
            .sortedBy { it.rate }
    }

    fun maxRate(ctx: Context): Int = options(ctx).maxOfOrNull { it.rate } ?: 60

    fun best(ctx: Context): RateOption? = options(ctx).maxByOrNull { it.rate }

    fun currentRate(ctx: Context): Int = display(ctx).refreshRate.roundToInt()

    /** O recurso de taxa só é liberado para telas acima de 60 Hz. */
    fun isCompatible(ctx: Context): Boolean = maxRate(ctx) > 60
}
