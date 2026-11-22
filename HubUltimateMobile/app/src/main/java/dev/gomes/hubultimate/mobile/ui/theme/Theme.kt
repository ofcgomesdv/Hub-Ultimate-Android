/*
 * Hub Ultimate · Android
 * © 2026 Gomes.dev — Todos os direitos reservados.
 * Proibida a cópia, redistribuição ou modificação sem autorização do autor.
 */
package dev.gomes.hubultimate.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object HubColors {
    // Estilo "Gamer neon": preto profundo com destaque verde neon.
    val Bg = Color(0xFF050607)
    val Surface = Color(0xFF0D1110)
    val SurfaceHi = Color(0xFF1A221E)
    val Border = Color(0xFF1E2622)
    val Text = Color(0xFFF2F4F3)
    val Muted = Color(0xFFA3ABA7)
    val Accent = Color(0xFF2BFF88)
    /** Texto em cima do verde neon (preto esverdeado, para ter contraste). */
    val OnAccent = Color(0xFF04150B)
    val Ok = Color(0xFF2BFF88)
    val Warn = Color(0xFFFFB020)
    val Bad = Color(0xFFFF4D6A)

    fun forLoad(f: Float): Color = when {
        f >= 0.85f -> Bad
        f >= 0.70f -> Warn
        else -> Accent
    }

    fun forTemp(c: Float?): Color = when {
        c == null -> Text
        c >= 43f -> Bad
        c >= 38f -> Warn
        else -> Ok
    }
}

@Composable
fun HubTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = HubColors.Accent,
            onPrimary = HubColors.OnAccent,
            background = HubColors.Bg,
            onBackground = HubColors.Text,
            surface = HubColors.Surface,
            onSurface = HubColors.Text,
            surfaceVariant = HubColors.SurfaceHi,
            onSurfaceVariant = HubColors.Muted,
            outline = HubColors.Border,
            secondaryContainer = HubColors.SurfaceHi,
            onSecondaryContainer = HubColors.Text,
        ),
        typography = Typography(),
        content = content,
    )
}
