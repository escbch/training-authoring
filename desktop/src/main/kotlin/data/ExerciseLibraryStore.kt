package data

import com.example.training.io.Serde
import com.example.training.model.ExerciseDef
import kotlinx.serialization.builtins.ListSerializer
import java.io.File

object ExerciseLibraryStore {
    fun loadFromString(text: String): List<ExerciseDef> =
        if (text.trimStart().startsWith("{"))
            Serde.json.decodeFromString(ListSerializer(ExerciseDef.serializer()), text)
        else
            Serde.yaml.decodeFromString(ListSerializer(ExerciseDef.serializer()), text)

    fun loadFromFile(file: File): List<ExerciseDef> = loadFromString(file.readText())

    fun save(list: List<ExerciseDef>, file: File) {
        file.parentFile?.mkdirs()
        val yaml = Serde.yaml.encodeToString(ListSerializer(ExerciseDef.serializer()), list)
        file.writeText(yaml)
    }
}
