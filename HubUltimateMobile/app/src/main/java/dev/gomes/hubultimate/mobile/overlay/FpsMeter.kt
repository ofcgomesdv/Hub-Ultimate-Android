/*
 * Hub Ultimate · Android
 * © 2026 Gomes.dev — Todos os direitos reservados.
 * Proibida a cópia, redistribuição ou modificação sem autorização do autor.
 */
package dev.gomes.hubultimate.mobile.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt

/**
 * Estima o FPS contando quantos quadros NOVOS o Android compõe na tela por segundo.
 *
 * Como funciona: espelha a tela numa tela virtual minúscula (1/4 ou 1/8 da resolução).
 * O sistema só entrega uma nova imagem quando algo na tela muda, então
 * "imagens por segundo" ≈ "quadros por segundo" do jogo.
 * Cada imagem é descartada na hora: nada é salvo, analisado ou enviado.
 * O jogo não é lido, modificado nem injetado.
 */
class FpsMeter(
    private val ctx: Context,
    private val projection: MediaProjection,
    /** Modo leve: espelho em 1/8 da resolução e menos buffers (menos GPU e RAM). */
    private val lite: Boolean = false,
) {

    private val thread = HandlerThread("hub-fps").apply { start() }
    private val handler = Handler(thread.looper)
    private var reader: ImageReader? = null
    private var display: VirtualDisplay? = null
    private val frames = AtomicInteger(0)
    private var lastTs = SystemClock.elapsedRealtime()

    fun start() {
        val m = ctx.resources.displayMetrics
        val div = if (lite) 8 else 4
        val w = (m.widthPixels / div).coerceAtLeast(64)
        val h = (m.heightPixels / div).coerceAtLeast(64)
        val r = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, if (lite) 2 else 4)
        r.setOnImageAvailableListener({ rd ->
            while (true) {
                val img = try { rd.acquireNextImage() } catch (_: IllegalStateException) { null } ?: break
                img.close()
                frames.incrementAndGet()
            }
        }, handler)
        reader = r
        lastTs = SystemClock.elapsedRealtime()
        display = projection.createVirtualDisplay(
            "HubUltimateFps", w, h, m.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            r.surface, null, handler,
        )
    }

    /** Retorna o FPS desde a última chamada (chamado 1x por segundo). */
    fun sample(): Int {
        val now = SystemClock.elapsedRealtime()
        val dt = (now - lastTs).coerceAtLeast(1)
        lastTs = now
        return (frames.getAndSet(0) * 1000f / dt).roundToInt()
    }

    fun stop() {
        runCatching { display?.release() }
        runCatching { reader?.close() }
        display = null
        reader = null
        thread.quitSafely()
    }
}
