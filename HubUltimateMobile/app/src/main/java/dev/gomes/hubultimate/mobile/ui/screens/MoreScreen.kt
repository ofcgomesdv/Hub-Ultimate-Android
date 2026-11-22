/*
 * Hub Ultimate · Android
 * © 2026 Gomes.dev — Todos os direitos reservados.
 * Proibida a cópia, redistribuição ou modificação sem autorização do autor.
 */
package dev.gomes.hubultimate.mobile.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gomes.hubultimate.mobile.ui.ScreenColumn
import dev.gomes.hubultimate.mobile.ui.theme.HubColors

enum class MoreSection(val title: String, val subtitle: String, val icon: ImageVector) {
    Display("Taxa da tela", "Travar 90/120/144 Hz durante o jogo", Icons.Rounded.Speed),
    Monitor("Monitor de FPS", "FPS, Hz, RAM e temperatura por cima do jogo", Icons.Rounded.Insights),
    GameMode("Modo jogo", "Não perturbe e pausa da sincronização", Icons.Rounded.SportsEsports),
    Donate("Apoie o Hub", "Doação voluntária via Pix", Icons.Rounded.Favorite),
    Terms("Termos de uso", "Termos e Política de Privacidade", Icons.Rounded.Description),
}

@Composable
fun MoreScreen(
    section: MoreSection?,
    onSection: (MoreSection?) -> Unit,
    onRequestFps: () -> Unit,
) {
    if (section != null) {
        BackHandler { onSection(null) }
        Column(Modifier.fillMaxSize()) {
            TextButton(onClick = { onSection(null) }, modifier = Modifier.padding(start = 4.dp, top = 4.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, tint = HubColors.Accent)
                Spacer(Modifier.width(6.dp))
                Text("Voltar", color = HubColors.Accent)
            }
            Box(Modifier.weight(1f)) {
                when (section) {
                    MoreSection.Display -> DisplayScreen()
                    MoreSection.Monitor -> MonitorScreen(onRequestFps)
                    MoreSection.GameMode -> GameModeScreen()
                    MoreSection.Donate -> DonateScreen()
                    MoreSection.Terms -> TermsScreen()
                }
            }
        }
        return
    }

    ScreenColumn("Mais", "Recursos e informações") {
        MoreSection.entries.forEach { s -> MoreRow(s) { onSection(s) } }
    }
}

@Composable
private fun MoreRow(s: MoreSection, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(HubColors.Surface)
            .border(1.dp, HubColors.Border, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(HubColors.SurfaceHi),
            contentAlignment = Alignment.Center,
        ) {
            Icon(s.icon, contentDescription = null, tint = HubColors.Accent)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(s.title, color = HubColors.Text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(s.subtitle, color = HubColors.Muted, fontSize = 12.sp)
        }
        Text("›", color = HubColors.Accent, fontSize = 22.sp)
    }
}
