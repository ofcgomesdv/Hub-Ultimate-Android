# Hub Ultimate · Android

Otimizador para jogos no Android, com visual Gamer neon (preto + verde neon) e botão BOOST redondo na tela inicial.
Kotlin + Jetpack Compose · Android 8.0+ · sem root · sem ADB.

© 2026 Gomes.dev · Todos os direitos reservados. Veja o arquivo [LICENSE](LICENSE).

## Abas (v1.3)

| Aba | O que faz |
|---|---|
| **Boost** | Botão BOOST redondo, RAM livre, temperatura, jogos com botão JOGAR (Boost + Modo jogo + abre o jogo) e Modo leve. |
| **Jogos** | Todos os jogos detectados, com busca e opções do Boost. |
| **Rede** | Teste de ping (média, jitter e perda), sinal e faixa do Wi-Fi (2,4/5/6 GHz) e dicas para melhorar o ping. |
| **Limpeza** | Limpeza profunda (APKs, miniaturas, temporários, pastas vazias), apps em segundo plano, cache do Hub e cache dos apps. |
| **Mais** | Taxa da tela, Monitor de FPS, Modo jogo, Apoie o Hub (Pix) e Termos de uso. |

### Modo jogo
Liga sozinho ao abrir um jogo pelo Hub: ativa o **Não perturbe** (alarmes e chamadas permitidas continuam) e **pausa a sincronização automática**,
que usa internet em segundo plano e causa picos de ping. Tudo volta como estava em "Encerrar" (notificação ou tela Boost).

### Ping
O teste mede a qualidade da SUA conexão até Cloudflare, Google e Quad9. Nenhum app consegue diminuir o ping até o servidor do jogo;
o Hub ajuda a achar e cortar o que atrapalha (Wi-Fi 2,4 GHz, sinal fraco, jitter, sincronização em segundo plano).

### Limpeza profunda
Pede "Acesso a todos os arquivos". **Nunca entra na pasta Android** (dados e downloads dos jogos) e não apaga fotos, vídeos,
músicas ou documentos. Arquivos grandes são só listados. Nada é apagado sem confirmação.

### Doações (configure antes de publicar)
Abra `app/src/main/java/dev/gomes/hubultimate/mobile/core/Pix.kt` e troque `SUA_CHAVE_PIX_AQUI` pela sua chave Pix
(e confira nome e cidade). Enquanto a chave não for trocada, a tela mostra "Doações em breve".

### Termos de uso
Aparecem na primeira abertura (é preciso aceitar) e ficam em Mais › Termos de uso.
Troque `[SEU E-MAIL DE CONTATO]` em `ui/screens/TermsScreen.kt`.

## Celulares mais fracos (v1.1)

Na primeira abertura o Hub classifica o aparelho (Básico, Intermediário ou Avançado) pela RAM e pelo número de núcleos.
Em aparelhos **Básicos** (até 3 GB de RAM, 4 núcleos ou menos, ou Android Go) o **Modo leve** já vem ligado.
Ele pode ser ligado ou desligado na aba Início.

O Modo leve faz o Hub gastar menos:
- a interface atualiza as leituras 2,5x menos vezes;
- ícones carregados só quando aparecem na tela, em tamanho menor, com cache limitado (~4 MB), que é solto quando o Android avisa que falta RAM;
- o monitor atualiza a cada 2 s e o FPS é medido em 1/8 da resolução, com menos buffers (menos GPU e RAM).

O que vale para todos os aparelhos:
- perfis de inicialização (ProfileInstaller): o app abre mais rápido;
- APK menor: só os idiomas português e inglês, código e recursos reduzidos pelo R8.

Em aparelhos Básicos, a aba Início também mostra dicas: Boost antes de jogar, gráficos baixos e FPS 30/40 no jogo,
espaço livre, animações do sistema em 0,5x (com botão para abrir as Opções do desenvolvedor) e cuidado com o aquecimento.

## Respeito às diretrizes dos jogos

O app **não injeta nada nos jogos**: não lê nem altera a memória deles, não modifica arquivos, não mexe em
arquivos de configuração (.ini, shaders, etc.), não usa root, não usa VPN e não intercepta a rede.
Tudo é feito com APIs públicas do Android, as mesmas que gravadores de tela e bolhas de chat usam:

- **Trava de Hz**: uma janela invisível de 1 px do próprio Hub pede ao Android um modo de tela
  (`preferredDisplayModeId`). O sistema decide aplicar ou não.
- **FPS**: a tela é espelhada numa tela virtual pequena e o Hub conta quantos quadros novos chegam por segundo.
  Cada imagem é descartada na hora: nada é salvo nem enviado.
- **Segundo plano**: `killBackgroundProcesses`, que só encerra processos que já estão parados.
  Launcher, teclado e o app em uso nunca são encerrados.

## Limites do Android (importante)

- **Cache de outros apps**: desde o Android 6, nenhum app comum pode apagar o cache de outros apps.
  O Hub mostra o tamanho e abre a tela certa para você tocar em "Limpar cache".
- **Taxa de Hz**: o Hub não passa do limite que você escolheu nas configurações do sistema. Ative
  "Taxa de atualização alta" / "Suavidade de movimento". Economia de energia, bateria fraca e aquecimento
  podem forçar 60 Hz. Alguns jogos também têm limite de FPS próprio (ajuste dentro do jogo).
- **FPS**: o valor é uma estimativa bem próxima do real. Em menus parados ele cai porque nada é redesenhado.
  Ao ligar, o Android mostra o aviso de compartilhamento de tela — escolha **"Tela inteira"**.

## Permissões

| Permissão | Para quê | Como liberar |
|---|---|---|
| Sobrepor a outros apps | Monitor flutuante e trava de Hz | O app abre a tela certa |
| Acesso ao uso | Ler o tamanho do cache dos apps | O app abre a tela certa |
| Captura de tela | Somente contar FPS | O Android pergunta ao ligar o FPS |
| Notificações | Notificação do monitor e do Modo jogo | Pedida ao abrir o app |
| Acesso ao Não perturbe | Modo jogo | O app abre a tela certa |
| Acesso a todos os arquivos | Limpeza profunda | O app abre a tela certa |
| Internet / estado da rede e do Wi-Fi | Teste de ping e sinal do Wi-Fi | Automática |
| Sincronização | Pausar e restaurar a sincronização no Modo jogo | Automática |
| Encerrar processos em 2º plano | Boost / Limpeza | Automática |

## Segurança (v1.4)

- **Chave de assinatura secreta**: não fica no repositório. O GitHub Actions monta a chave a partir dos *Secrets*
  (`HUB_KEYSTORE_BASE64`, `HUB_KEYSTORE_PASSWORD`, `HUB_KEY_ALIAS`, `HUB_KEY_PASSWORD`) e apaga depois do build.
- **Anti-adulteração**: o app confere a própria assinatura. Uma cópia modificada (ex.: com outra chave Pix)
  mostra um aviso e fica com as doações bloqueadas.
- **Dados criptografados**: configurações e estado do Modo jogo são cifrados com AES-256-GCM, chave no Android Keystore.
  Alteração no arquivo é detectada e descartada.
- **Código ofuscado** (R8) no APK oficial, sem logs; tráfego sem criptografia (HTTP) bloqueado; backup do app desligado.
- Recomendado: deixar o repositório **privado**.

## Como gerar o APK

1. Instale o **Android Studio** (Ladybug 2024.2 ou mais novo).
2. *File › Open* e escolha a pasta `HubUltimateMobile`. Espere o Gradle sincronizar (baixa as dependências).
3. Para testar: ligue a **Depuração USB** no celular, conecte no PC e clique em **Run ▶**.
4. Para gerar o APK: *Build › Build App Bundle(s) / APK(s) › Build APK(s)*.
   O arquivo sai em `app/build/outputs/apk/`.

Pelo terminal: `./gradlew assembleRelease` (Windows: `gradlew.bat assembleRelease`).

> O APK de release está assinado com a chave de debug para instalar direto no celular.
> Para publicar na Play Store, gere uma chave própria em *Build › Generate Signed Bundle/APK*.

## Estrutura

```
app/src/main/java/dev/gomes/hubultimate/mobile/
├── MainActivity.kt            navegação das 5 abas
├── core/                      SystemInfo, RefreshRateManager, Cleaner, AppRepository, Prefs
├── overlay/                   MonitorOverlayService (janela flutuante + trava de Hz), FpsMeter
└── ui/                        tema Minimal pro, componentes e telas
```
