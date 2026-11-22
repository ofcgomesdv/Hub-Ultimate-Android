/*
 * Hub Ultimate · Android
 * © 2026 Gomes.dev — Todos os direitos reservados.
 * Proibida a cópia, redistribuição ou modificação sem autorização do autor.
 */
package dev.gomes.hubultimate.mobile.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.gomes.hubultimate.mobile.core.DonationConfig
import dev.gomes.hubultimate.mobile.core.Integrity
import dev.gomes.hubultimate.mobile.core.Pix
import dev.gomes.hubultimate.mobile.ui.BodyText
import dev.gomes.hubultimate.mobile.ui.Bullet
import dev.gomes.hubultimate.mobile.ui.CardTitle
import dev.gomes.hubultimate.mobile.ui.HubCard
import dev.gomes.hubultimate.mobile.ui.PrimaryButton
import dev.gomes.hubultimate.mobile.ui.ScreenColumn
import dev.gomes.hubultimate.mobile.ui.SecondaryButton
import dev.gomes.hubultimate.mobile.ui.theme.HubColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonateScreen() {
    val ctx = LocalContext.current
    val amounts = listOf<Double?>(5.0, 10.0, 20.0, 50.0, null)
    var amount by remember { mutableStateOf<Double?>(10.0) }

    ScreenColumn("Apoie o Hub", "Doação voluntária via Pix") {
        HubCard {
            CardTitle("Gostou do Hub Ultimate?")
            BodyText(
                "O app é gratuito e sem anúncios. Se ele te ajudou, você pode apoiar o desenvolvimento com qualquer valor. " +
                    "A doação vai direto para a conta do desenvolvedor (Gomes.dev), sem intermediários.",
            )
        }

        val official = remember { Integrity.isOfficial(ctx) || Integrity.isDevBuild() }
        if (!official) {
            HubCard {
                CardTitle("Doações bloqueadas")
                BodyText(
                    "Esta cópia do Hub Ultimate foi modificada ou não veio da Gomes.dev. Para sua segurança, as doações foram " +
                        "desativadas: a chave Pix desta versão pode ter sido trocada. Baixe o app oficial.",
                    HubColors.Bad,
                )
            }
        } else if (!DonationConfig.isConfigured) {
            HubCard { BodyText("Doações em breve.") }
        } else {
            val payload = remember(amount) {
                Pix.payload(DonationConfig.PIX_KEY, DonationConfig.RECEIVER_NAME, DonationConfig.RECEIVER_CITY, amount)
            }
            val qr = remember(payload) { Pix.qr(payload).asImageBitmap() }

            HubCard {
                CardTitle("Escolha o valor")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    amounts.forEach { a ->
                        val sel = amount == a
                        FilterChip(
                            selected = sel,
                            onClick = { amount = a },
                            label = { Text(a?.let { "R$ ${it.toInt()}" } ?: "Outro valor") },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = HubColors.SurfaceHi, labelColor = HubColors.Text,
                                selectedContainerColor = HubColors.Accent, selectedLabelColor = HubColors.OnAccent,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true, selected = sel,
                                borderColor = HubColors.Border, selectedBorderColor = HubColors.Accent,
                            ),
                        )
                    }
                }
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Image(
                        qr, contentDescription = "QR Code Pix para doação",
                        modifier = Modifier.size(220.dp).clip(RoundedCornerShape(12.dp)).background(Color.White).padding(8.dp),
                    )
                }
                BodyText("Abra o app do seu banco › Pix › Ler QR Code. Ou copie o código abaixo e use \"Pix copia e cola\".")
                PrimaryButton("Copiar Pix copia e cola", {
                    copy(ctx, "Pix Hub Ultimate", payload)
                    Toast.makeText(ctx, "Código Pix copiado", Toast.LENGTH_SHORT).show()
                })
                SecondaryButton("Copiar só a chave Pix", {
                    copy(ctx, "Chave Pix", DonationConfig.PIX_KEY)
                    Toast.makeText(ctx, "Chave Pix copiada", Toast.LENGTH_SHORT).show()
                })
            }
        }

        HubCard {
            CardTitle("Importante")
            Bullet("A doação é voluntária e não libera recursos extras: o app é igual para todos.")
            Bullet("Confira o nome do recebedor no app do banco antes de confirmar.")
            Bullet("O Hub nunca pede senha, dados de cartão ou código do banco.")
        }
    }
}

private fun copy(ctx: android.content.Context, label: String, text: String) {
    ctx.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(label, text))
}
