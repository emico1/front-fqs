plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
}

// Direcciones del backend. Se pueden cambiar sin tocar código, en gradle.properties
// (o con -P al compilar).
//   fqs.api.debug    para el emulador es http://10.0.2.2:3000/ (la computadora anfitriona)
//   fqs.api.release  dirección pública del backend, SIEMPRE con https
val apiDebug = (findProperty("fqs.api.debug") as String?) ?: "http://10.0.2.2:3000/"
val apiRelease = (findProperty("fqs.api.release") as String?) ?: "https://api.familiasquesuman.mx/"
require(apiRelease.startsWith("https://")) { "fqs.api.release debe usar https" }
require(apiDebug.endsWith("/") && apiRelease.endsWith("/")) { "Las URL del backend deben terminar en /" }

// Fijación de certificado (opcional). Huellas SHA-256 de la llave pública del servidor
// separadas por coma, por ejemplo
//   fqs.api.pins=sha256/AAAA...=,sha256/BBBB...=
// Se exigen al menos dos (la actual y una de respaldo) para no dejar la app sin servicio
// cuando se renueve el certificado. Vacío significa sin fijación, solo la confianza del sistema.
// Ver docs/seguridad/fijacion-certificado.md en el backend para obtener las huellas.
val apiPins = ((findProperty("fqs.api.pins") as String?) ?: "")
    .split(",").map { it.trim() }.filter { it.isNotEmpty() }
require(apiPins.isEmpty() || apiPins.size >= 2) { "fqs.api.pins necesita al menos dos huellas, la actual y una de respaldo" }
require(apiPins.all { Regex("^sha256/[A-Za-z0-9+/]{43}=$").matches(it) }) { "Cada huella de fqs.api.pins debe tener el formato sha256/...=" }

android {
    namespace = "mx.familiasquesuman.app"
    // compileSdk 36 porque Room 2.8 y WorkManager 2.10 se compilan con SDKs nuevos. targetSdk se
    // queda en 34 para no cambiar el comportamiento de las pantallas (borde a borde obligatorio)
    compileSdk = 36

    defaultConfig {
        applicationId = "mx.familiasquesuman.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 4
        versionName = "0.3.1"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"$apiDebug\"")
            // En el emulador se habla con la computadora por HTTP, no hay certificado que fijar
            buildConfigField("String", "API_PINS", "\"\"")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"$apiRelease\"")
            buildConfigField("String", "API_PINS", "\"${apiPins.joinToString(",")}\"")
            // Ofusca y elimina código sin uso, dificulta la ingeniería inversa del APK
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")

    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Imágenes del backend (usa el mismo cliente HTTP autenticado que la API)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Red y serialización
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Cupos en tiempo real por Server-Sent Events (RF-05)
    implementation("com.squareup.okhttp3:okhttp-sse:4.12.0")

    // Notificaciones push (RF16)
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    // Uso sin conexión. Room guarda la última copia de cada consulta y los envíos pendientes
    // (cifrados con el Keystore), WorkManager los manda cuando vuelve la red
    val room = "2.8.4"
    implementation("androidx.room:room-runtime:$room")
    ksp("androidx.room:room-compiler:$room")
    implementation("androidx.work:work-runtime:2.10.5")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
