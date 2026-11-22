/*
 * Hub Ultimate · Android
 * © 2026 Gomes.dev — Todos os direitos reservados.
 * Proibida a cópia, redistribuição ou modificação sem autorização do autor.
 */
package dev.gomes.hubultimate.mobile

import android.Manifest
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import dev.gomes.hubultimate.mobile.overlay.MonitorOverlayService
import dev.gomes.hubultimate.mobile.core.AppRepository
import dev.gomes.hubultimate.mobile.core.GameMode
import dev.gomes.hubultimate.mobile.core.Prefs
import dev.gomes.hubultimate.mobile.ui.LocalLiteMode
import dev.gomes.hubultimate.mobile.ui.LocalResumeTick
import dev.gomes.hubultimate.mobile.ui.screens.CleanScreen
import dev.gomes.hubultimate.mobile.ui.screens.GamesScreen
import dev.gomes.hubultimate.mobile.ui.screens.HomeScreen
import dev.gomes.hubultimate.mobile.ui.screens.MoreScreen
import dev.gomes.hubultimate.mobile.ui.screens.MoreSection
import dev.gomes.hubultimate.mobile.ui.screens.NetworkScreen
import dev.gomes.hubultimate.mobile.ui.screens.TermsGate
import dev.gomes.hubultimate.mobile.ui.theme.HubColors
import dev.gomes.hubultimate.mobile.ui.theme.HubTheme

enum class Tab(val label: String, val icon: ImageVector) {
    Home("Boost", Icons.Rounded.Bolt),
    Games("Jogos", Icons.Rounded.SportsEsports),
    Network("Rede", Icons.Rounded.NetworkCheck),
    Clean("Limpeza", Icons.Rounded.CleaningServices),
    More("Mais", Icons.Rounded.MoreHoriz),
}

class MainActivity : ComponentActivity() {

    private val resumeTick = mutableIntStateOf(0)
    private val liteMode = mutableStateOf(false)
    private val termsOk = mutableStateOf(false)

    /** Consentimento de captura de tela, usado só para contar FPS. */
    private val projectionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            if (res.resultCode == RESULT_OK && res.data != null) {
                MonitorOverlayService.startMonitor(this, res.resultCode, res.data)
            } else {
                MonitorOverlayService.startMonitor(this, 0, null)
                Toast.makeText(this, "FPS não autorizado: monitor ligado sem FPS.", Toast.LENGTH_LONG).show()
            }
        }

    private val notifLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private fun requestFpsMonitor() {
        val mpm = getSystemService(MediaProjectionManager::class.java)
        projectionLauncher.launch(mpm.createScreenCaptureIntent())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        liteMode.value = Prefs.liteMode(this)
        termsOk.value = Prefs.termsAccepted(this)
        setContent {
            HubTheme {
                CompositionLocalProvider(
                    LocalResumeTick provides resumeTick.intValue,
                    LocalLiteMode provides liteMode.value,
                ) {
                    if (!termsOk.value) {
                        TermsGate(
                            onAccept = { Prefs.acceptTerms(this); termsOk.value = true },
                            onDecline = { finish() },
                        )
                    } else {
                        HubApp(
                            onRequestFps = ::requestFpsMonitor,
                            onLiteChange = { liteMode.value = it; Prefs.setLiteMode(this, it) },
                        )
                    }
                }
            }
        }
    }

    /** O Android avisa quando a RAM está apertada: o Hub solta o cache de ícones na hora. */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        AppRepository.trimMemory()
    }

    override fun onResume() {
        super.onResume()
        GameMode.refresh(this)
        resumeTick.intValue++
    }
}

@Composable
private fun HubApp(onRequestFps: () -> Unit, onLiteChange: (Boolean) -> Unit) {
    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    var more by rememberSaveable { mutableStateOf<MoreSection?>(null) }
    Scaffold(
        containerColor = HubColors.Bg,
        bottomBar = {
            NavigationBar(containerColor = HubColors.Surface) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { if (t == Tab.More && tab == Tab.More) more = null; tab = t },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = HubColors.Accent,
                            selectedTextColor = HubColors.Accent,
                            indicatorColor = HubColors.Accent.copy(alpha = 0.12f),
                            unselectedIconColor = HubColors.Muted,
                            unselectedTextColor = HubColors.Muted,
                        ),
                    )
                }
            }
        },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            when (tab) {
                Tab.Home -> HomeScreen(
                    onOpenGames = { tab = Tab.Games },
                    onOpenMonitor = { more = MoreSection.Monitor; tab = Tab.More },
                    onLiteChange = onLiteChange,
                )
                Tab.Games -> GamesScreen()
                Tab.Network -> NetworkScreen(onOpenGameMode = { more = MoreSection.GameMode; tab = Tab.More })
                Tab.Clean -> CleanScreen()
                Tab.More -> MoreScreen(more, onSection = { more = it }, onRequestFps = onRequestFps)
            }
        }
    }
}
