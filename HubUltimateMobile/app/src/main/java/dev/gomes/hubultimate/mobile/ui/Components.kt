/*
 * Hub Ultimate · Android
 * © 2026 Gomes.dev — Todos os direitos reservados.
 * Proibida a cópia, redistribuição ou modificação sem autorização do autor.
 */
package dev.gomes.hubultimate.mobile.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gomes.hubultimate.mobile.ui.theme.HubColors
import kotlinx.coroutines.delay

/** Muda toda vez que o app volta para a frente (ex.: após conceder uma permissão). */
val LocalResumeTick = compositionLocalOf { 0 }

/** Modo leve ativo: a interface atualiza menos vezes e carrega menos coisas. */
val LocalLiteMode = compositionLocalOf { false }

@Composable
fun rememberTicker(periodMs: Long = 2000L): Int {
    var t by remember { mutableIntStateOf(0) }
    // No modo leve, as leituras da tela são 2,5x menos frequentes (menos CPU e bateria).
    val period = if (LocalLiteMode.current) (periodMs * 5 / 2) else periodMs
    LaunchedEffect(period) {
        while (true) {
            delay(period)
            t++
        }
    }
    return t
}

@Composable
fun ScreenColumn(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (title.isNotEmpty()) ScreenHeader(title, subtitle)
        content()
        Footer()
    }
}

@Composable
fun ScreenHeader(title: String, subtitle: String) {
    Column(Modifier.padding(top = 8.dp, bottom = 4.dp)) {
        Text(
            title.uppercase(), color = HubColors.Text, fontSize = 22.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(2.dp))
        Text(subtitle, color = HubColors.Muted, fontSize = 13.sp)
    }
}

@Composable
fun HubCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(HubColors.Surface)
            .border(1.dp, HubColors.Border, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
fun CardTitle(text: String, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text, color = HubColors.Text, fontSize = 15.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

@Composable
fun BodyText(text: String, color: Color = HubColors.Muted) {
    Text(text, color = color, fontSize = 13.sp, lineHeight = 18.sp)
}

/** Rótulo de seção em caixa alta, como "JOGAR COM BOOST". */
@Composable
fun SectionLabel(text: String) {
    Text(
        text.uppercase(), color = HubColors.Muted, fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
    )
}

@Composable
fun Bullet(text: String) {
    Row {
        Text("•  ", color = HubColors.Accent, fontSize = 13.sp)
        BodyText(text)
    }
}

@Composable
fun Badge(text: String, color: Color) {
    Text(
        text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
fun MetricBar(label: String, value: String, fraction: Float, color: Color = HubColors.Accent) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(label, color = HubColors.Muted, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(value, color = HubColors.Text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(HubColors.SurfaceHi),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(color),
            )
        }
    }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = HubColors.Text) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .clip(shape)
            .background(HubColors.Surface)
            .border(1.dp, HubColors.Border, shape)
            .padding(14.dp),
    ) {
        Text(label, color = HubColors.Muted, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = valueColor, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(46.dp),
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = HubColors.Accent,
            contentColor = HubColors.OnAccent,
            disabledContainerColor = HubColors.SurfaceHi,
            disabledContentColor = HubColors.Muted,
        ),
    ) { Text(text.uppercase(), fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(46.dp),
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, HubColors.Border),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = HubColors.Text),
    ) { Text(text) }
}

@Composable
fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = if (enabled) HubColors.Text else HubColors.Muted, fontSize = 14.sp)
            Text(description, color = HubColors.Muted, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = HubColors.OnAccent,
                checkedTrackColor = HubColors.Accent,
                uncheckedThumbColor = HubColors.Muted,
                uncheckedTrackColor = HubColors.SurfaceHi,
                uncheckedBorderColor = HubColors.Border,
            ),
        )
    }
}

@Composable
fun Note(text: String, color: Color = HubColors.Warn) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(10.dp),
    ) {
        Box(Modifier.width(3.dp).height(16.dp).background(color))
        Spacer(Modifier.width(8.dp))
        Text(text, color = HubColors.Text, fontSize = 12.sp, lineHeight = 17.sp)
    }
}

@Composable
fun Footer() {
    val ctx = LocalContext.current
    val version = remember {
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: ""
    }
    Column(
        Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "© 2026 Gomes.dev · Todos os direitos reservados",
            color = HubColors.Muted.copy(alpha = 0.8f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            "Hub Ultimate Android v$version",
            color = HubColors.Muted.copy(alpha = 0.5f),
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
        )
    }
}
