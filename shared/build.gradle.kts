plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.squareup.sqldelight")
}

kotlin {
    jvm()
    jvmToolchain(17)

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("com.charleskorn.kaml:kaml:0.60.0")
                implementation("com.networknt:json-schema-validator:1.4.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
                implementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.1")
            }
        }
    }
}

sqldelight {
    database("AuthoringDatabase") { packageName = "com.example.training.data" }
}
