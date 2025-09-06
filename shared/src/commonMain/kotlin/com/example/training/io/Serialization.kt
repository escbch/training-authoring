package com.example.training.io

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
object Serde {
    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = false
        explicitNulls = false
        encodeDefaults = true
    }

    val yaml = Yaml(configuration = Yaml.default.configuration.copy(
        encodeDefaults = true
    ))
}