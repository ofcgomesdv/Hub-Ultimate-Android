/*
 * Hub Ultimate · Android
 * © 2026 Gomes.dev — Todos os direitos reservados.
 * Proibida a cópia, redistribuição ou modificação sem autorização do autor.
 */
package dev.gomes.hubultimate.mobile.core

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Armazenamento criptografado do Hub.
 *  - Chave AES-256 gerada e guardada no Android Keystore (hardware seguro quando o
 *    celular tem): ela nunca sai do aparelho e nem o próprio app consegue exportá-la.
 *  - Cada valor é cifrado com AES-GCM, que também detecta alteração: se alguém mexer
 *    no arquivo, o valor é descartado e volta ao padrão.
 *  - O nome de cada configuração também fica embaralhado (hash), e o nome vai junto
 *    na verificação, então não dá para copiar um valor de uma configuração para outra.
 */
class SecureStore private constructor(private val sp: SharedPreferences) {

    companion object {
        private const val KEY_ALIAS = "hub_ultimate_store_v1"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private val stores = HashMap<String, SecureStore>()

        fun of(ctx: Context, file: String): SecureStore = synchronized(stores) {
            stores.getOrPut(file) {
                SecureStore(ctx.applicationContext.getSharedPreferences("sec_$file", Context.MODE_PRIVATE))
            }
        }

        @Volatile private var cachedKey: SecretKey? = null

        /** A referência à chave é guardada em memória: o Keystore só é aberto uma vez. */
        private fun key(): SecretKey = cachedKey ?: synchronized(this) {
            cachedKey ?: loadOrCreateKey().also { cachedKey = it }
        }

        private fun loadOrCreateKey(): SecretKey {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            (ks.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
            val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            gen.init(
                KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
            return gen.generateKey()
        }
    }

    private fun slot(name: String): String =
        MessageDigest.getInstance("SHA-256").digest(name.toByteArray())
            .joinToString("") { "%02x".format(it) }.take(24)

    private fun encrypt(name: String, plain: String): String {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, key())
        c.updateAAD(name.toByteArray())
        val out = c.iv + c.doFinal(plain.toByteArray())
        return Base64.encodeToString(out, Base64.NO_WRAP)
    }

    private fun decrypt(name: String, stored: String): String? = runCatching {
        val raw = Base64.decode(stored, Base64.NO_WRAP)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, raw, 0, 12))
        c.updateAAD(name.toByteArray())
        String(c.doFinal(raw, 12, raw.size - 12))
    }.getOrNull()

    private fun read(name: String): String? = sp.getString(slot(name), null)?.let { decrypt(name, it) }

    private fun write(name: String, value: String) {
        val enc = runCatching { encrypt(name, value) }.getOrNull() ?: return
        sp.edit().putString(slot(name), enc).apply()
    }

    fun contains(name: String): Boolean = read(name) != null
    fun getBoolean(name: String, def: Boolean): Boolean = read(name)?.toBooleanStrictOrNull() ?: def
    fun getInt(name: String, def: Int): Int = read(name)?.toIntOrNull() ?: def
    fun putBoolean(name: String, v: Boolean) = write(name, v.toString())
    fun putInt(name: String, v: Int) = write(name, v.toString())
    fun remove(name: String) = sp.edit().remove(slot(name)).apply()
    fun clear() = sp.edit().clear().apply()
}
