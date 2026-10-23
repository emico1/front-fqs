// Top-level build file
//
// Versiones elegidas para que el proyecto abra y compile con cualquier Android Studio desde
// Otter 3 Feature Drop (2025.2.3) y con cualquier JDK del 17 al 25, incluido el que trae
// Android Studio. Gradle 9.1.0 es el primero que corre con Java 25 y el plugin de Android 9.0
// es el primero que funciona con Gradle 9.
//
// El plugin de Android 9 ya compila Kotlin por sí mismo (Kotlin 2.2.10), por eso aquí no va
// org.jetbrains.kotlin.android. Los plugins de Compose y de serialización deben tener la misma
// versión de Kotlin.
plugins {
    id("com.android.application") version "9.0.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.21" apply false
    // Genera el código de Room. Desde la 2.3 su versión ya no depende de la de Kotlin
    id("com.google.devtools.ksp") version "2.3.6" apply false
    // Lee app/google-services.json para conectar la app con Firebase (notificaciones push)
    id("com.google.gms.google-services") version "4.4.4" apply false
}
