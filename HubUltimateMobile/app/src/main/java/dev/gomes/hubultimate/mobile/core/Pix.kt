/*
 * Hub Ultimate · Android
 * © 2026 Gomes.dev — Todos os direitos reservados.
 * Proibida a cópia, redistribuição ou modificação sem autorização do autor.
 */
package dev.gomes.hubultimate.mobile.core

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.text.Normalizer
import java.util.Locale

/**
 * ======= CONFIGURE AQUI A SUA CHAVE PIX =======
 * A doação cai direto na sua conta bancária (sem intermediários).
 * Chave: CPF, CNPJ, e-mail, celular (+5531...) ou chave aleatória.
 */
object DonationConfig {
    const val PIX_KEY = "SUA_CHAVE_PIX_AQUI"
    const val RECEIVER_NAME = "GOMES DEV"        // até 25 letras, sem acento
    const val RECEIVER_CITY = "BELO HORIZONTE"   // até 15 letras, sem acento

    val isConfigured: Boolean get() = PIX_KEY.isNotBlank() && PIX_KEY != "SUA_CHAVE_PIX_AQUI"
}

/** Gera o "Pix copia e cola" (BR Code estático, padrão do Banco Central) e o QR Code. */
object Pix {

    private fun field(id: String, value: String): String =
        id + value.length.toString().padStart(2, '0') + value

    private fun clean(s: String, max: Int): String =
        Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
            .replace(Regex("[^A-Za-z0-9 ]"), "")
            .uppercase(Locale.ROOT)
            .take(max)

    /** amount = null deixa a pessoa digitar o valor no app do banco. */
    fun payload(key: String, name: String, city: String, amount: Double? = null): String {
        val account = field("00", "br.gov.bcb.pix") + field("01", key.trim())
        val sb = StringBuilder()
            .append(field("00", "01"))
            .append(field("26", account))
            .append(field("52", "0000"))
            .append(field("53", "986"))
        if (amount != null && amount > 0) sb.append(field("54", String.format(Locale.US, "%.2f", amount)))
        sb.append(field("58", "BR"))
            .append(field("59", clean(name, 25)))
            .append(field("60", clean(city, 15)))
            .append(field("62", field("05", "***")))
            .append("6304")
        return sb.toString() + crc16(sb.toString())
    }

    /** CRC16-CCITT (polinômio 0x1021, início 0xFFFF), exigido pelo padrão Pix. */
    private fun crc16(s: String): String {
        var crc = 0xFFFF
        for (b in s.toByteArray(Charsets.UTF_8)) {
            crc = crc xor ((b.toInt() and 0xFF) shl 8)
            repeat(8) {
                crc = if (crc and 0x8000 != 0) (crc shl 1) xor 0x1021 else crc shl 1
                crc = crc and 0xFFFF
            }
        }
        return String.format(Locale.ROOT, "%04X", crc)
    }

    fun qr(text: String, size: Int = 640): Bitmap {
        val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, mapOf(EncodeHintType.MARGIN to 1))
        val pixels = IntArray(size * size) { i -> if (m[i % size, i / size]) Color.BLACK else Color.WHITE }
        return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    }
}
