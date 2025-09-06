package data

import com.example.training.io.Serde
import com.example.training.model.TrainingPlan
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.File

object PlanStore {
    fun loadFromString(text: String): TrainingPlan =
        if (text.trimStart().startsWith("{"))
            Serde.json.decodeFromString(text)
        else
            Serde.yaml.decodeFromString(text)

    fun loadFromFile(file: File): TrainingPlan = loadFromString(file.readText())

    fun save(plan: TrainingPlan, file: File) {
        file.parentFile?.mkdirs()
        val planNoLib = plan.copy(exercise_library = emptyList()) // keep plans lean
        val text = if (file.name.endsWith(".json", true))
            Serde.json.encodeToString(planNoLib)
        else
            Serde.yaml.encodeToString(TrainingPlan.serializer(), planNoLib)
        file.writeText(text)
    }
}
