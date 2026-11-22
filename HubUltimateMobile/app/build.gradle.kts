plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "dev.gomes.hubultimate.mobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.gomes.hubultimate.mobile"
        minSdk = 26          // Android 8.0+
        targetSdk = 35
        versionCode = 6
        versionName = "1.4.0"
        // Só português e inglês no APK: arquivo menor e instalação mais rápida.
        resourceConfigurations += listOf("pt", "en")
    }

    // A chave de assinatura NÃO fica no repositório: o GitHub Actions monta o arquivo
    // a partir dos Secrets (HUB_KEYSTORE_BASE64 etc.). Sem ela (no seu PC), o release
    // usa a chave de debug e o app mostra "versão não oficial".
    val hubKeystore = System.getenv("HUB_KEYSTORE_PATH")?.takeIf { file(it).exists() }
    signingConfigs {
        if (hubKeystore != null) {
            create("hub") {
                storeFile = file(hubKeystore)
                storePassword = System.getenv("HUB_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("HUB_KEY_ALIAS")
                keyPassword = System.getenv("HUB_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true          // R8: encolhe e OFUSCA o código
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (hubKeystore != null) signingConfigs.getByName("hub")
            else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    // Instala os perfis de inicialização do Compose: o app abre mais rápido em celulares fracos.
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
    // QR Code do Pix de doação
    implementation("com.google.zxing:core:3.5.3")
}
