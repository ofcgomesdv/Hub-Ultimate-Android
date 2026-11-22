/*
 * Hub Ultimate · Android
 * © 2026 Gomes.dev — Todos os direitos reservados.
 * Proibida a cópia, redistribuição ou modificação sem autorização do autor.
 */
package dev.gomes.hubultimate.mobile.ui.screens

import android.Manifest
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.TextButton
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gomes.hubultimate.mobile.core.AppCache
import dev.gomes.hubultimate.mobile.core.Cleaner
import dev.gomes.hubultimate.mobile.core.JunkCleaner
import dev.gomes.hubultimate.mobile.core.JunkKind
import dev.gomes.hubultimate.mobile.core.JunkReport
import dev.gomes.hubultimate.mobile.core.Perms
import dev.gomes.hubultimate.mobile.core.SystemInfo.formatBytes
import dev.gomes.hubultimate.mobile.ui.Badge
import dev.gomes.hubultimate.mobile.ui.BodyText
import dev.gomes.hubultimate.mobile.ui.CardTitle
import dev.gomes.hubultimate.mobile.ui.HubCard
import dev.gomes.hubultimate.mobile.ui.LocalResumeTick
import dev.gomes.hubultimate.mobile.ui.Note
import dev.gomes.hubultimate.mobile.ui.PrimaryButton
import dev.gomes.hubultimate.mobile.ui.ScreenColumn
import dev.gomes.hubultimate.mobile.ui.SecondaryButton
import dev.gomes.hubultimate.mobile.ui.theme.HubColors
import kotlinx.coroutines.launch

@Composable
fun CleanScreen() {
    val ctx = LocalContext.current
    val resume = LocalResumeTick.current
    val scope = rememberCoroutineScope()

    val hasUsage = remember(resume) { Cleaner.hasUsageAccess(ctx) }
    val caches by produceState<List<AppCache>?>(null, hasUsage, resume) {
        value = if (hasUsage) Cleaner.appCaches(ctx) else null
    }

    var bgBusy by remember { mutableStateOf(false) }
    var bgResult by remember { mutableStateOf<String?>(null) }
    var ownResult by remember { mutableStateOf<String?>(null) }

    ScreenColumn("Limpeza", "Memória, cache e arquivos") {
        DeepCleanCard()

        HubCard {
            CardTitle("Apps em segundo plano")
            BodyText(
                "Encerra processos de apps que estão parados em segundo plano para liberar RAM. " +
                    "O app em uso, o teclado, a tela inicial e os serviços do sistema não são afetados.",
            )
            PrimaryButton(
                if (bgBusy) "Limpando..." else "Limpar segundo plano",
                enabled = !bgBusy,
                onClick = {
                    scope.launch {
                        bgBusy = true
                        val r = Cleaner.killBackground(ctx)
                        bgResult = "${r.processed} apps processados · ${formatBytes(r.freedBytes)} de RAM liberados"
                        bgBusy = false
                    }
                },
            )
            bgResult?.let { BodyText(it, HubColors.Ok) }
        }

        HubCard {
            CardTitle("Cache do Hub")
            BodyText("Apaga os arquivos temporários do próprio Hub Ultimate.")
            SecondaryButton("Limpar cache do Hub", {
                scope.launch { ownResult = "${formatBytes(Cleaner.clearOwnCache(ctx))} apagados" }
            })
            ownResult?.let { BodyText(it, HubColors.Ok) }
        }

        HubCard {
            val total = caches?.sumOf { it.cacheBytes }
            CardTitle("Cache dos apps", trailing = { total?.let { Badge(formatBytes(it), HubColors.Accent) } })
            BodyText(
                "Por segurança, desde o Android 6 nenhum app comum pode apagar o cache de outros apps. " +
                    "O Hub mostra quem ocupa mais espaço e abre a tela certa: toque no app e depois em " +
                    "\"Armazenamento\" › \"Limpar cache\". (Não use \"Limpar dados\", isso apaga login e progresso.)",
            )
            when {
                !hasUsage -> PrimaryButton("Permitir acesso ao uso", { Perms.open(ctx, Perms.usageIntent()) })
                caches == null -> BodyText("Calculando...")
                caches!!.isEmpty() -> BodyText("Nenhum cache relevante encontrado.")
                else -> caches!!.take(15).forEachIndexed { i, c ->
                    if (i > 0) HorizontalDivider(color = HubColors.Border)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { Perms.open(ctx, Perms.appDetails(c.packageName)) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            c.label, color = HubColors.Text, fontSize = 14.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                        )
                        Text(formatBytes(c.cacheBytes), color = HubColors.Muted, fontSize = 13.sp)
                        Text("  ›", color = HubColors.Accent, fontSize = 16.sp)
                    }
                }
            }
            SecondaryButton("Abrir armazenamento do sistema", {
                Perms.open(ctx, Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
            })
        }
    }
}

@Composable
private fun DeepCleanCard() {
    val ctx = LocalContext.current
    val resume = LocalResumeTick.current
    val scope = rememberCoroutineScope()
    val hasAccess = remember(resume) { JunkCleaner.hasAccess(ctx) }
    var scanning by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf<JunkReport?>(null) }
    var selected by remember { mutableStateOf(setOf(JunkKind.APK, JunkKind.THUMBS, JunkKind.TEMP, JunkKind.EMPTY_DIRS)) }
    var confirm by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf<String?>(null) }
    val legacyPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    HubCard {
        CardTitle("Limpeza profunda")
        BodyText(
            "Procura arquivos desnecessários no armazenamento: instaladores APK, miniaturas, temporários e pastas vazias. " +
                "Fotos, vídeos, músicas, documentos e os dados dos jogos (pasta Android) nunca são apagados.",
        )
        if (!hasAccess) {
            Note("O Hub precisa de \"Acesso a todos os arquivos\" para procurar os arquivos. Ele só lê e apaga o que você confirmar.")
            PrimaryButton("Permitir acesso aos arquivos", {
                val i = JunkCleaner.accessIntent(ctx)
                if (i != null) Perms.open(ctx, i) else legacyPerm.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            })
        } else {
        PrimaryButton(
            if (scanning) "Analisando..." else if (report == null) "Analisar armazenamento" else "Analisar de novo",
            enabled = !scanning,
            onClick = {
                scope.launch {
                    scanning = true; done = null
                    report = JunkCleaner.scan()
                    scanning = false
                }
            },
        )
        val r = report
        if (r != null) {
            BodyText("${r.scanned} arquivos verificados.")
            r.groups.forEach { g ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = g.kind in selected,
                        onCheckedChange = { on -> selected = if (on) selected + g.kind else selected - g.kind },
                        enabled = g.files.isNotEmpty(),
                        colors = CheckboxDefaults.colors(checkedColor = HubColors.Accent, checkmarkColor = HubColors.OnAccent),
                    )
                    Column(Modifier.weight(1f)) {
                        Text(g.kind.title, color = HubColors.Text, fontSize = 14.sp)
                        Text(g.kind.description, color = HubColors.Muted, fontSize = 11.sp, lineHeight = 15.sp)
                    }
                    Text(
                        if (g.kind == JunkKind.EMPTY_DIRS) "${g.files.size}" else formatBytes(g.bytes),
                        color = HubColors.Text, fontSize = 13.sp,
                    )
                }
            }
            val chosen = r.groups.filter { it.kind in selected && it.files.isNotEmpty() }
            val total = chosen.sumOf { it.bytes }
            PrimaryButton(
                "Apagar selecionados (${formatBytes(total)})",
                enabled = chosen.isNotEmpty(),
                onClick = { confirm = true },
            )
            if (r.large.isNotEmpty()) {
                BodyText("Arquivos grandes (o Hub não apaga; revise no app de arquivos):", HubColors.Text)
                r.large.forEach { lf ->
                    Row(Modifier.fillMaxWidth()) {
                        Text(lf.file.name, color = HubColors.Muted, fontSize = 12.sp, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text(formatBytes(lf.bytes), color = HubColors.Muted, fontSize = 12.sp)
                    }
                }
            }
            if (confirm) {
                val n = chosen.sumOf { it.files.size }
                AlertDialog(
                    onDismissRequest = { confirm = false },
                    containerColor = HubColors.Surface,
                    title = { Text("Apagar $n itens?", color = HubColors.Text) },
                    text = {
                        Text(
                            "${formatBytes(total)} serão apagados para sempre: " +
                                chosen.joinToString(", ") { it.kind.title.lowercase() } +
                                ". Não dá para desfazer.",
                            color = HubColors.Muted,
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            confirm = false
                            scope.launch {
                                val (count, bytes) = JunkCleaner.delete(chosen)
                                done = "$count itens apagados · ${formatBytes(bytes)} liberados"
                                report = null
                            }
                        }) { Text("Apagar", color = HubColors.Bad) }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirm = false }) { Text("Cancelar", color = HubColors.Text) }
                    },
                )
            }
        }
        done?.let { BodyText(it, HubColors.Accent) }
        }
    }
}
