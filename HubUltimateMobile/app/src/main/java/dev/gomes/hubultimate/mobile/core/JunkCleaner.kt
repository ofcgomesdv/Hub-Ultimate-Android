/*
 * Hub Ultimate · Android
 * © 2026 Gomes.dev — Todos os direitos reservados.
 * Proibida a cópia, redistribuição ou modificação sem autorização do autor.
 */
package dev.gomes.hubultimate.mobile.core

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

enum class JunkKind(val title: String, val description: String) {
    APK("Instaladores APK", "Arquivos .apk/.xapk de apps que você já instalou ou baixou."),
    THUMBS("Miniaturas", "Prévias de fotos e vídeos. O Android cria de novo quando precisar."),
    TEMP("Temporários e logs", "Arquivos .tmp, .log, .bak e downloads que ficaram pela metade."),
    EMPTY_DIRS("Pastas vazias", "Pastas sem nenhum arquivo dentro."),
}

data class JunkGroup(val kind: JunkKind, val files: List<File>, val bytes: Long)
data class LargeFile(val file: File, val bytes: Long)
data class JunkReport(val groups: List<JunkGroup>, val large: List<LargeFile>, val scanned: Int)

/**
 * Limpeza profunda do armazenamento compartilhado.
 * Regras de segurança:
 *  - NUNCA entra na pasta "Android" (Android/data e Android/obb guardam dados e
 *    downloads dos jogos e apps; mexer ali pode quebrar jogos).
 *  - Não apaga fotos, vídeos, músicas nem documentos: só as categorias acima.
 *  - Arquivos grandes são só listados, para você decidir.
 *  - Nada é apagado sem a confirmação do usuário.
 */
object JunkCleaner {
    private const val MAX_FILES = 150_000
    private const val MAX_DEPTH = 14
    private const val LARGE_BYTES = 100L * 1024 * 1024

    private val apkExt = setOf("apk", "apks", "xapk", "apkm")
    private val tempExt = setOf("tmp", "temp", "log", "bak", "old", "crdownload", "part", "partial", "dmp")

    fun hasAccess(ctx: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ctx.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

    /** Android 11+: tela "Acesso a todos os arquivos". Antes disso, a permissão comum de armazenamento. */
    fun accessIntent(ctx: Context): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${ctx.packageName}"))
        } else null

    suspend fun scan(): JunkReport = withContext(Dispatchers.IO) {
        val root = Environment.getExternalStorageDirectory()
        val apk = mutableListOf<File>()
        val thumbs = mutableListOf<File>()
        val temp = mutableListOf<File>()
        val emptyDirs = mutableListOf<File>()
        val large = mutableListOf<LargeFile>()
        var count = 0

        fun walk(dir: File, depth: Int, inThumbs: Boolean): Boolean {
            // Retorna true se a pasta ficou sem arquivos (vazia ou só com pastas vazias).
            if (depth > MAX_DEPTH || count > MAX_FILES) return false
            val children = dir.listFiles() ?: return false
            var empty = true
            for (f in children) {
                if (depth == 0 && f.name.equals("Android", ignoreCase = true)) { empty = false; continue }
                if (f.isDirectory) {
                    val thumbsHere = inThumbs || f.name.equals(".thumbnails", ignoreCase = true)
                    val childEmpty = walk(f, depth + 1, thumbsHere)
                    if (childEmpty) emptyDirs += f else empty = false
                } else {
                    empty = false
                    count++
                    val ext = f.extension.lowercase()
                    val len = f.length()
                    when {
                        inThumbs -> thumbs += f
                        ext in apkExt -> apk += f
                        ext in tempExt -> temp += f
                        len >= LARGE_BYTES -> large += LargeFile(f, len)
                    }
                }
            }
            return empty
        }

        walk(root, 0, false)
        // Só as pastas vazias "de cima": apagar a de cima já remove as de dentro.
        val topEmpty = emptyDirs.filter { d -> emptyDirs.none { o -> o != d && d.path.startsWith(o.path + File.separator) } }

        JunkReport(
            groups = listOf(
                JunkGroup(JunkKind.APK, apk, apk.sumOf { it.length() }),
                JunkGroup(JunkKind.THUMBS, thumbs, thumbs.sumOf { it.length() }),
                JunkGroup(JunkKind.TEMP, temp, temp.sumOf { it.length() }),
                JunkGroup(JunkKind.EMPTY_DIRS, topEmpty, 0L),
            ),
            large = large.sortedByDescending { it.bytes }.take(10),
            scanned = count,
        )
    }

    /** Apaga as categorias escolhidas. Retorna (itens apagados, bytes liberados). */
    suspend fun delete(groups: List<JunkGroup>): Pair<Int, Long> = withContext(Dispatchers.IO) {
        val androidDir = File(Environment.getExternalStorageDirectory(), "Android").canonicalPath
        var n = 0
        var bytes = 0L
        for (g in groups) for (f in g.files) {
            // Proteção extra: nunca apaga nada dentro de Android/.
            if (runCatching { f.canonicalPath.startsWith(androidDir) }.getOrDefault(true)) continue
            val len = if (f.isFile) f.length() else 0L
            val ok = if (f.isDirectory) {
                // Só apaga a pasta se ela continua sem nenhum arquivo dentro.
                f.walkTopDown().none { it.isFile } && f.deleteRecursively()
            } else f.delete()
            if (ok) { n++; bytes += len }
        }
        n to bytes
    }
}
