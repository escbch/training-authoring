// desktop/src/main/kotlin/platform/Settings.kt
package platform

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Paths

object Settings {
    private val appDir = Paths.get(System.getProperty("user.home"), ".training-authoring").toFile()
    private val plansDir = File(appDir, "plans")
    private val settingsFile = File(appDir, "settings.json")

    // Configure JSON once
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Serializable
    data class AppSettings(
        val lastPlanPath: String? = null,
        val lastLibraryPath: String? = null
    )

    fun load(): AppSettings {
        if (!settingsFile.exists()) return AppSettings()
        val text = settingsFile.readText()
        return try {
            val payload = if (text.isBlank()) "{}" else text
            json.decodeFromString(AppSettings.serializer(), payload)
        } catch (_: Exception) {
            AppSettings()
        }
    }

    fun save(s: AppSettings) {
        appDir.mkdirs()
        val payload = json.encodeToString(AppSettings.serializer(), s)
        settingsFile.writeText(payload)
    }

    fun setLastPlan(file: File?) {
        val current: AppSettings = load()
        save(current.copy(lastPlanPath = file?.absolutePath))
    }

    fun setLastLibrary(file: File?) {
        val current: AppSettings = load()
        save(current.copy(lastLibraryPath = file?.absolutePath))
    }

    fun lastPlanFile(): File? =
        load().lastPlanPath?.let { File(it) }?.takeIf { it.exists() }

    fun lastLibraryFile(): File? =
        load().lastLibraryPath?.let { File(it) }?.takeIf { it.exists() }

    fun defaultLibraryFile(): File = File(appDir, "library.yml")

    fun plansDirectory(): File = plansDir.also { it.mkdirs() }
}
