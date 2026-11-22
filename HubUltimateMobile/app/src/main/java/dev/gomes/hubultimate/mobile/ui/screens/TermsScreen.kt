/*
 * Hub Ultimate · Android
 * © 2026 Gomes.dev — Todos os direitos reservados.
 * Proibida a cópia, redistribuição ou modificação sem autorização do autor.
 */
package dev.gomes.hubultimate.mobile.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gomes.hubultimate.mobile.ui.BodyText
import dev.gomes.hubultimate.mobile.ui.HubCard
import dev.gomes.hubultimate.mobile.ui.PrimaryButton
import dev.gomes.hubultimate.mobile.ui.ScreenColumn
import dev.gomes.hubultimate.mobile.ui.SecondaryButton
import dev.gomes.hubultimate.mobile.ui.theme.HubColors

/** Atualize CONTACT com o seu e-mail de contato. */
private const val CONTACT = "[SEU E-MAIL DE CONTATO]"
private const val UPDATED = "22 de novembro de 2026"

private val TERMS: List<Pair<String, String>> = listOf(
    "1. Aceitação" to
        "Ao instalar ou usar o Hub Ultimate (\"App\"), desenvolvido por Gomes.dev, você concorda com estes Termos de Uso e com a Política de Privacidade abaixo. Se não concordar, não use o App.",
    "2. O que o App faz" to
        "O App oferece ferramentas de otimização para o seu aparelho Android: encerrar apps parados em segundo plano, mostrar e ajudar a limpar cache e arquivos desnecessários, pedir ao sistema uma taxa de atualização da tela, exibir um monitor de desempenho (FPS, Hz, RAM e temperatura), ativar o Modo jogo (Não perturbe e pausa da sincronização) e testar a qualidade da conexão.",
    "3. O que o App não faz" to
        "O App não injeta código, não lê nem altera a memória, os arquivos ou os dados de jogos, não modifica jogos, não usa root, não usa VPN e não intercepta o tráfego de rede. O App não promete aumento de FPS ou redução de ping: os resultados variam conforme o aparelho, o jogo e a conexão.",
    "4. Regras dos jogos" to
        "Cada jogo tem as próprias regras sobre aplicativos de terceiros. Você é responsável por conhecer e respeitar essas regras. A Gomes.dev não tem controle sobre os sistemas antitrapaça dos jogos e não se responsabiliza por advertências, suspensões ou banimentos aplicados por terceiros. Recomendamos não usar o monitor flutuante em partidas ranqueadas.",
    "5. Limpeza de arquivos" to
        "A limpeza profunda apaga apenas as categorias que você escolher e somente depois da sua confirmação. Arquivos apagados não podem ser recuperados. A pasta Android (dados de apps e jogos), fotos, vídeos, músicas e documentos não são apagados pelo App. Faça backup do que for importante.",
    "6. Permissões" to
        "O App pede permissões (sobreposição, acesso ao uso, Não perturbe, acesso aos arquivos, captura de tela para o FPS e notificações) apenas para as funções descritas e só quando você as usa. Você pode revogá-las a qualquer momento nas configurações do Android.",
    "7. Privacidade" to
        "O App não coleta, não armazena em servidores e não vende dados pessoais. Não há cadastro, anúncios nem rastreamento. As medições acontecem apenas no seu aparelho. A captura de tela serve somente para contar quadros por segundo: nenhuma imagem é salva ou enviada. O teste de ping apenas abre e fecha conexões com servidores públicos (Cloudflare, Google e Quad9) para medir o tempo de resposta, sem enviar dados seus.",
    "8. Doações" to
        "Doações via Pix são voluntárias, vão diretamente para a conta do desenvolvedor e não dão acesso a recursos extras. O App nunca pede senhas, dados de cartão ou códigos bancários.",
    "9. Isenção de garantias" to
        "O App é fornecido \"no estado em que se encontra\", sem garantias de qualquer tipo. Na máxima extensão permitida pela lei, a Gomes.dev não se responsabiliza por perdas de dados, danos ao aparelho, mau funcionamento de outros apps ou quaisquer prejuízos decorrentes do uso do App.",
    "10. Propriedade intelectual" to
        "O App, seu código, design, marca, ícones e textos pertencem à Gomes.dev. Todos os direitos reservados. É proibido copiar, modificar, redistribuir, vender ou criar versões derivadas sem autorização por escrito.",
    "11. Alterações" to
        "Estes Termos podem ser atualizados. Quando houver mudança importante, o App pedirá que você aceite a nova versão.",
    "12. Lei aplicável e contato" to
        "Estes Termos seguem as leis da República Federativa do Brasil, incluindo o Código de Defesa do Consumidor e a Lei Geral de Proteção de Dados (LGPD). Dúvidas: $CONTACT.",
)

@Composable
private fun TermsBody() {
    BodyText("Última atualização: $UPDATED")
    TERMS.forEach { (title, body) ->
        HubCard {
            Text(title, color = HubColors.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            BodyText(body)
        }
    }
}

/** Termos dentro da aba Mais (só leitura). */
@Composable
fun TermsScreen() {
    ScreenColumn("Termos de uso", "e Política de Privacidade") { TermsBody() }
}

/** Tela de aceite mostrada na primeira abertura (e quando os termos mudarem). */
@Composable
fun TermsGate(onAccept: () -> Unit, onDecline: () -> Unit) {
    Column(Modifier.fillMaxSize().background(HubColors.Bg)) {
        Column(Modifier.weight(1f)) {
            ScreenColumn("Termos de uso", "Leia antes de usar o Hub Ultimate") { TermsBody() }
        }
        Row(
            Modifier.fillMaxWidth().background(HubColors.Surface).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SecondaryButton("Recusar e sair", onDecline, Modifier.weight(1f))
            PrimaryButton("Aceitar", onAccept, Modifier.weight(1f))
        }
    }
}
