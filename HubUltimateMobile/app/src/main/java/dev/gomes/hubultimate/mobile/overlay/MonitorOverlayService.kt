/*
 * Hub Ultimate · Android
 * © 2026 Gomes.dev — Todos os direitos reservados.
 * Proibida a cópia, redistribuição ou modificação sem autorização do autor.
 */
package dev.gomes.hubultimate.mobile.overlay

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import dev.gomes.hubultimate.mobile.MainActivity
import dev.gomes.hubultimate.mobile.R
import dev.gomes.hubultimate.mobile.core.Prefs
import dev.gomes.hubultimate.mobile.core.RefreshRateManager
import dev.gomes.hubultimate.mobile.core.SystemInfo
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Locale
import kotlin.math.roundToInt

/** Estado compartilhado com a interface. */
object OverlayState {
    val monitorOn = MutableStateFlow(false)
    val fpsOn = MutableStateFlow(false)
    val lockedRate = MutableStateFlow<Int?>(null)
}

/**
 * Serviço em primeiro plano com duas janelas opcionais, ambas do próprio Hub:
 *  1) Monitor flutuante (FPS, Hz, RAM, temperatura) — arrastável.
 *  2) Trava de taxa: janela de 1 px que pede ao Android o modo de tela escolhido.
 * Nenhuma das duas interage com o jogo: só ficam por cima dele, como uma bolha de chat.
 */
class MonitorOverlayService : Service() {

    companion object {
        private const val ACTION_START_MONITOR = "hub.START_MONITOR"
        private const val ACTION_STOP_MONITOR = "hub.STOP_MONITOR"
        private const val ACTION_LOCK_RATE = "hub.LOCK_RATE"
        private const val ACTION_UNLOCK_RATE = "hub.UNLOCK_RATE"
        private const val ACTION_STOP_ALL = "hub.STOP_ALL"
        private const val EXTRA_RESULT_CODE = "result_code"
        private const val EXTRA_RESULT_DATA = "result_data"
        private const val EXTRA_MODE_ID = "mode_id"
        private const val EXTRA_RATE = "rate"
        private const val CHANNEL = "hub_monitor"
        private const val NOTIF_ID = 1001

        /** data != null liga também o FPS (consentimento de captura já dado). */
        fun startMonitor(ctx: Context, resultCode: Int, data: Intent?) {
            val i = Intent(ctx, MonitorOverlayService::class.java)
                .setAction(ACTION_START_MONITOR)
                .putExtra(EXTRA_RESULT_CODE, resultCode)
            if (data != null) i.putExtra(EXTRA_RESULT_DATA, data)
            ContextCompat.startForegroundService(ctx, i)
        }

        fun stopMonitor(ctx: Context) {
            if (!OverlayState.monitorOn.value) return
            ctx.startService(Intent(ctx, MonitorOverlayService::class.java).setAction(ACTION_STOP_MONITOR))
        }

        fun lockRate(ctx: Context, modeId: Int, rate: Int) {
            val i = Intent(ctx, MonitorOverlayService::class.java)
                .setAction(ACTION_LOCK_RATE)
                .putExtra(EXTRA_MODE_ID, modeId)
                .putExtra(EXTRA_RATE, rate)
            ContextCompat.startForegroundService(ctx, i)
        }

        fun unlockRate(ctx: Context) {
            if (OverlayState.lockedRate.value == null) return
            ctx.startService(Intent(ctx, MonitorOverlayService::class.java).setAction(ACTION_UNLOCK_RATE))
        }
    }

    private lateinit var wm: WindowManager
    private val handler = Handler(Looper.getMainLooper())

    private var monitorView: TextView? = null
    private var monitorParams: WindowManager.LayoutParams? = null
    private var lockView: View? = null
    private var lockParams: WindowManager.LayoutParams? = null
    private var lockedModeId = 0
    private var lockedRate = 0

    private var lite = false
    private var projection: MediaProjection? = null
    private var fpsMeter: FpsMeter? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WindowManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL, "Monitor do Hub", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_MONITOR -> {
                val code = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val data = IntentCompat.getParcelableExtra(intent, EXTRA_RESULT_DATA, Intent::class.java)
                goForeground(withProjection = data != null || projection != null)
                showMonitor()
                if (data != null) startFps(code, data)
            }
            ACTION_LOCK_RATE -> {
                goForeground(withProjection = projection != null)
                applyLock(intent.getIntExtra(EXTRA_MODE_ID, 0), intent.getIntExtra(EXTRA_RATE, 0))
            }
            ACTION_STOP_MONITOR -> hideMonitor()
            ACTION_UNLOCK_RATE -> removeLock()
            ACTION_STOP_ALL -> { hideMonitor(); removeLock() }
        }
        stopIfIdle()
        return START_NOT_STICKY
    }

    // ---------- Primeiro plano ----------

    private fun goForeground(withProjection: Boolean) {
        val n = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var type = 0
            if (Build.VERSION.SDK_INT >= 34) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            if (withProjection) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            startForeground(NOTIF_ID, n, type)
        } else {
            startForeground(NOTIF_ID, n)
        }
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, MonitorOverlayService::class.java).setAction(ACTION_STOP_ALL),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Hub Ultimate ativo")
            .setContentText("Monitor e/ou trava de taxa ligados. Toque em Desligar para parar.")
            .setSubText("© Gomes.dev")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(open)
            .addAction(0, "Desligar", stop)
            .build()
    }

    private fun stopIfIdle() {
        if (monitorView == null && lockView == null) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    // ---------- Janelas ----------

    private fun overlayParams(w: Int, h: Int, touchable: Boolean) = WindowManager.LayoutParams(
        w, h,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or (
            if (touchable) WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            else WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            ),
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        if (lockedModeId != 0) {
            preferredDisplayModeId = lockedModeId
            preferredRefreshRate = lockedRate.toFloat()
        }
    }

    private fun showMonitor() {
        if (monitorView != null || !Settings.canDrawOverlays(this)) return
        lite = Prefs.liteMode(this)
        val dp = resources.displayMetrics.density
        val tv = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setPadding((10 * dp).toInt(), (6 * dp).toInt(), (10 * dp).toInt(), (6 * dp).toInt())
            background = GradientDrawable().apply {
                cornerRadius = 10 * dp
                setColor(0xCC0D1110.toInt())
                setStroke((1 * dp).toInt(), 0x992BFF88)
            }
            text = "Hub · iniciando"
        }
        val p = overlayParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            touchable = true,
        ).apply {
            x = (12 * dp).toInt()
            y = (80 * dp).toInt()
        }
        tv.setOnTouchListener(DragListener(p))
        wm.addView(tv, p)
        monitorView = tv
        monitorParams = p
        OverlayState.monitorOn.value = true
        handler.post(tick)
    }

    private fun hideMonitor() {
        handler.removeCallbacks(tick)
        monitorView?.let { runCatching { wm.removeView(it) } }
        monitorView = null
        monitorParams = null
        stopFps()
        OverlayState.monitorOn.value = false
    }

    private fun applyLock(modeId: Int, rate: Int) {
        if (modeId == 0 || !Settings.canDrawOverlays(this)) return
        lockedModeId = modeId
        lockedRate = rate
        val existing = lockView
        val lp = lockParams
        if (existing != null && lp != null) {
            lp.preferredDisplayModeId = modeId
            lp.preferredRefreshRate = rate.toFloat()
            wm.updateViewLayout(existing, lp)
        } else {
            val v = View(this)
            val p = overlayParams(1, 1, touchable = false)
            wm.addView(v, p)
            lockView = v
            lockParams = p
        }
        // A janela do monitor fica por cima, então ela também precisa pedir o mesmo modo.
        monitorView?.let { mv ->
            monitorParams?.let { mp ->
                mp.preferredDisplayModeId = modeId
                mp.preferredRefreshRate = rate.toFloat()
                wm.updateViewLayout(mv, mp)
            }
        }
        OverlayState.lockedRate.value = rate
    }

    private fun removeLock() {
        lockView?.let { runCatching { wm.removeView(it) } }
        lockView = null
        lockParams = null
        lockedModeId = 0
        lockedRate = 0
        monitorView?.let { mv ->
            monitorParams?.let { mp ->
                mp.preferredDisplayModeId = 0
                mp.preferredRefreshRate = 0f
                wm.updateViewLayout(mv, mp)
            }
        }
        OverlayState.lockedRate.value = null
    }

    // ---------- FPS ----------

    private fun startFps(code: Int, data: Intent) {
        stopFps()
        val mpm = getSystemService(MediaProjectionManager::class.java)
        val proj = runCatching { mpm.getMediaProjection(code, data) }.getOrNull() ?: return
        proj.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                handler.post {
                    if (projection === proj) {
                        fpsMeter?.stop()
                        fpsMeter = null
                        projection = null
                        OverlayState.fpsOn.value = false
                    }
                }
            }
        }, handler)
        projection = proj
        fpsMeter = runCatching { FpsMeter(this, proj, lite = Prefs.liteMode(this)).also { it.start() } }.getOrNull()
        OverlayState.fpsOn.value = fpsMeter != null
    }

    private fun stopFps() {
        fpsMeter?.stop()
        fpsMeter = null
        val p = projection
        projection = null
        p?.let { runCatching { it.stop() } }
        OverlayState.fpsOn.value = false
    }

    // ---------- Atualização do monitor ----------

    private val tick = object : Runnable {
        override fun run() {
            val v = monitorView ?: return
            v.text = buildText()
            // Modo leve: atualiza a cada 2 s (o FPS continua sendo a média do período).
            handler.postDelayed(this, if (lite) 2000L else 1000L)
        }
    }

    private fun buildText(): String {
        val sb = StringBuilder()
        fpsMeter?.let { sb.append("FPS ").append(it.sample()).append("   ") }
        sb.append(RefreshRateManager.currentRate(this)).append(" Hz")
        val mem = SystemInfo.memory(this)
        sb.append("\nRAM ").append((mem.usedFraction * 100).roundToInt()).append("%")
        SystemInfo.batteryTempC(this)?.let {
            sb.append("   ").append(String.format(Locale.US, "%.1f°C", it))
        }
        return sb.toString()
    }

    private inner class DragListener(private val p: WindowManager.LayoutParams) : View.OnTouchListener {
        private var startX = 0
        private var startY = 0
        private var touchX = 0f
        private var touchY = 0f

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, e: MotionEvent): Boolean {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = p.x; startY = p.y; touchX = e.rawX; touchY = e.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    p.x = startX + (e.rawX - touchX).toInt()
                    p.y = startY + (e.rawY - touchY).toInt()
                    runCatching { wm.updateViewLayout(v, p) }
                }
            }
            return true
        }
    }

    override fun onDestroy() {
        hideMonitor()
        removeLock()
        super.onDestroy()
    }
}
