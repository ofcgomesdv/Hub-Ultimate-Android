/*
 * Hub Ultimate · Android
 * © 2026 Gomes.dev — Todos os direitos reservados.
 * Proibida a cópia, redistribuição ou modificação sem autorização do autor.
 */
package dev.gomes.hubultimate.mobile.core

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import dev.gomes.hubultimate.mobile.BuildConfig
import java.security.MessageDigest

/**
 * Anti-adulteração: o app confere com qual chave foi assinado.
 * Só a Gomes.dev tem a chave oficial (guardada nos Secrets do GitHub). Se alguém
 * modificar o app (por exemplo, trocar a chave Pix) e assinar de novo, a assinatura
 * muda e o Hub bloqueia as doações e avisa o usuário.
 */
object Integrity {
    /** SHA-256 do certificado oficial da Gomes.dev. */
    private const val OFFICIAL_CERT_SHA256 =
        "fbce6684c45caf52dd7143ec8b4c5a944247158d6859ed2574d75b95310a321d"

    @Volatile private var cached: Boolean? = null

    fun isOfficial(ctx: Context): Boolean {
        cached?.let { return it }
        val ok = certDigests(ctx).let { it.isNotEmpty() && it.all { d -> d == OFFICIAL_CERT_SHA256 } }
        cached = ok
        return ok
    }

    /** Builds de teste no seu PC (debug) não são bloqueadas. */
    fun isDevBuild(): Boolean = BuildConfig.DEBUG

    private fun certDigests(ctx: Context): List<String> = runCatching {
        val pm = ctx.packageManager
        val sigs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = pm.getPackageInfo(ctx.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            val si = info.signingInfo ?: return@runCatching emptyList()
            if (si.hasMultipleSigners()) si.apkContentsSigners else si.signingCertificateHistory
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(ctx.packageName, PackageManager.GET_SIGNATURES).signatures
        }
        sigs.orEmpty().map { sig ->
            MessageDigest.getInstance("SHA-256").digest(sig.toByteArray())
                .joinToString("") { "%02x".format(it) }
        }
    }.getOrDefault(emptyList())
}
