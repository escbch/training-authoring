package com.example.training.validation

import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import com.networknt.schema.ValidationMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

object PlanValidator {
    private val json = Json { ignoreUnknownKeys = false; prettyPrint = false }
    private val schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)

    fun validate(jsonText: String, schemaText: String): Set<ValidationMessage> {
        val jackson = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()
        val schema = schemaFactory.getSchema(schemaText)
        val node = jackson.readTree(jsonText)
        return schema.validate(node)
    }

    fun isJsonObject(text: String): Boolean = try {
        val o = json.parseToJsonElement(text)
        o is JsonObject
    } catch (_: Exception) { false }
}
