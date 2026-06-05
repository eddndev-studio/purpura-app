import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Credenciales de firma del release. Orden de fuentes: variables de entorno (CI) ->
// keystore.properties en la raiz (gitignored, para builds locales). NUNCA se commitea el
// keystore ni las contrasenas. Si falta alguna, el release queda sin firmar (no rompe a quien
// no tiene la llave). Claves esperadas: storeFile, storePassword, keyAlias, keyPassword.
val releaseKeystoreProps: Properties = Properties().apply {
    val propsFile = rootProject.file("keystore.properties")
    if (propsFile.exists()) FileInputStream(propsFile).use { load(it) }
}

fun signingValue(env: String, prop: String): String? =
    System.getenv(env) ?: releaseKeystoreProps.getProperty(prop)

android {
    namespace = "com.eddndev.purpura"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.eddndev.purpura"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Base URL de la API Go (debe terminar en /). Debug apunta al loopback del host del
        // emulador (10.0.2.2): requiere el backend corriendo localmente para probar en vivo.
        buildConfigField(
            "String",
            "API_BASE_URL",
            "\"http://10.0.2.2:8080/api/v1/\"",
        )
    }

    signingConfigs {
        create("release") {
            val storeFilePath = signingValue("RELEASE_STORE_FILE", "storeFile")
            val storePass = signingValue("RELEASE_STORE_PASSWORD", "storePassword")
            val alias = signingValue("RELEASE_KEY_ALIAS", "keyAlias")
            val keyPass = signingValue("RELEASE_KEY_PASSWORD", "keyPassword")
            if (storeFilePath != null && storePass != null && alias != null && keyPass != null) {
                storeFile = rootProject.file(storeFilePath)
                storePassword = storePass
                keyAlias = alias
                keyPassword = keyPass
            }
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
        }
        release {
            // Firma con la config release solo si hay credenciales; si no, queda sin firmar.
            signingConfig = signingConfigs.getByName("release").takeIf { it.storeFile != null }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Dominio de produccion (decidido): api.purpura.eddn.dev bajo Cloudflare. Pendiente:
            // crear el registro DNS (A api.purpura -> vps2, proxied) y desplegar el backend a vps2
            // para que resuelva en vivo. Debug sigue usando 10.0.2.2 para pruebas locales.
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"https://api.purpura.eddn.dev/api/v1/\"",
            )
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
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.runtime)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi.kotlin)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.security.crypto)
    implementation(libs.play.services.auth)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
