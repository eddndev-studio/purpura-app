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

// Maps Android API key. Orden de fuentes: local.properties (gitignored, build local) -> variable
// de entorno MAPS_API_KEY (CI/runner). NUNCA se commitea: el repo es publico y la key, aunque
// restringida por SHA-1+paquete, no debe vivir en el. Si falta queda "" y el mapa no renderiza,
// pero el build NO rompe (los tiles fallan en runtime): util para CI y para quien no tenga la key.
// Ver [[purpura-gcloud-setup]].
val localProps: Properties = Properties().apply {
    val propsFile = rootProject.file("local.properties")
    if (propsFile.exists()) FileInputStream(propsFile).use { load(it) }
}
val mapsApiKey: String = localProps.getProperty("MAPS_API_KEY")
    ?: System.getenv("MAPS_API_KEY")
    ?: ""

// Versionado: en los releases por tag, el CI inyecta APP_VERSION_NAME / APP_VERSION_CODE
// derivados del propio tag (el tag vX.Y.Z es la UNICA fuente de verdad, ver release.yml). En
// builds locales y en el CI de debug no estan definidas y se usan estos defaults. Asi el nombre
// del Release, el archivo y la versionName/versionCode dentro del APK nunca se desincronizan.
val defaultVersionName = "0.1.0"
val defaultVersionCode = 1
val appVersionName: String = System.getenv("APP_VERSION_NAME")?.takeIf { it.isNotBlank() }
    ?: defaultVersionName
val appVersionCode: Int = System.getenv("APP_VERSION_CODE")?.toIntOrNull()
    ?: defaultVersionCode

android {
    namespace = "com.eddndev.purpura"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.eddndev.purpura"
        minSdk = 26
        targetSdk = 34
        versionCode = appVersionCode
        versionName = appVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // La key se inyecta como placeholder del manifest (com.google.android.geo.API_KEY), no como
        // recurso ni BuildConfig, para no exponerla en el codigo.
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey

        // Base URL de la API Go (debe terminar en /). Debug apunta por defecto al loopback del host
        // del emulador (10.0.2.2): requiere el backend corriendo localmente. Para probar en un device
        // fisico contra prod, pasa -PdebugApiBaseUrl=https://api.purpura.eddn.dev/api/v1/ (o define
        // debugApiBaseUrl en gradle.properties). Release siempre usa el dominio de prod (mas abajo).
        val debugApiBaseUrl =
            (project.findProperty("debugApiBaseUrl") as String?) ?: "http://10.0.2.2:8080/api/v1/"
        buildConfigField("String", "API_BASE_URL", "\"$debugApiBaseUrl\"")
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

    // El SDK de Drive (google-http-client y transitivas) trae metadatos en META-INF que chocan al
    // empaquetar varios JARs. Se excluyen los no-codigo (licencias, avisos, indices, DEPENDENCIES).
    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/INDEX.LIST",
            )
        }
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
    implementation(libs.play.services.maps)

    // Respaldo/Restauracion programatica en Google Drive (REQ-BACKUP, API de Drive). El SDK arrastra
    // httpcomponents/guava-jdk5 (legacy, en conflicto con el resto) -> se excluyen; usamos el transporte
    // NetHttp del propio google-http-client. La autorizacion la da el GoogleSignInAccount (scope
    // drive.file: la app solo ve los archivos que ella crea). Ver [[purpura-gcloud-setup]].
    implementation(libs.google.api.client.android) {
        exclude(group = "org.apache.httpcomponents")
        exclude(module = "guava-jdk5")
    }
    implementation(libs.google.api.services.drive) {
        exclude(group = "org.apache.httpcomponents")
        exclude(module = "guava-jdk5")
    }

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
