# Hub Ultimate · © 2026 Gomes.dev — regras de proteção do código (R8)

# Embaralha nomes e junta tudo num pacote só: dificulta copiar ou entender o código.
-repackageclasses ''
-allowaccessmodification
-overloadaggressively

# Remove logs do APK final (nada de informação interna vazando pelo logcat).
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# Componentes declarados no manifesto (o R8 já mantém, reforçado aqui).
-keep class dev.gomes.hubultimate.mobile.MainActivity
-keep class dev.gomes.hubultimate.mobile.overlay.MonitorOverlayService
-keep class dev.gomes.hubultimate.mobile.core.GameModeReceiver
