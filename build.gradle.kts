plugins {
    // Kotlin plugins (do NOT apply here)
    kotlin("jvm") version "2.0.0" apply false
    kotlin("multiplatform") version "2.0.0" apply false
    kotlin("plugin.serialization") version "2.0.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false

    // Compose Multiplatform Gradle plugin (do NOT apply here)
    id("org.jetbrains.compose") version "1.6.11" apply false

    // SQLDelight (do NOT apply here)
    id("com.squareup.sqldelight") version "1.5.5" apply false
}

allprojects {
    group = "com.example.training"
    version = "0.1.0"
}
