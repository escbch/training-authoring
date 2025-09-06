
package data

import com.example.training.model.*
import java.io.File
import platform.Settings

/**
 * Lightweight index/helper around stored plan files in ~/.training-authoring/plans
 * Also responsible for creating/copying plans with proper week/day skeleton.
 */
object PlanIndex {
    private val dir: File get() = Settings.plansDirectory()

    fun listPlanFiles(): List<File> =
        dir.listFiles { f -> f.isFile && (f.extension.lowercase() in setOf("yml","yaml","json")) }
            ?.sortedBy { it.name.lowercase() } ?: emptyList()

    fun readPlanTitle(file: File): String =
        runCatching { PlanStore.loadFromFile(file).plan.name }.getOrElse { file.nameWithoutExtension }

    fun createNewPlan(name: String, durationWeeks: Int, daysPerWeek: Int, e1rmModel: E1rmModel): File {
        val weeks = (1..durationWeeks).map { w ->
            PlanWeek(
                week_index = w,
                days = (1..daysPerWeek).map { d ->
                    PlanDay(day_index = d, name = null, sessions = emptyList())
                }
            )
        }
        val file = uniqueFileName(name)
        val tp = TrainingPlan(
            plan = Plan(
                id = java.util.UUID.randomUUID().toString(),
                name = name.ifBlank { "Untitled Plan" },
                duration_weeks = durationWeeks,
                days_per_week = daysPerWeek,
                e1rm_model = e1rmModel,
                weeks = weeks
            ),
            exercise_library = emptyList() // kept for backward-compat ONLY; not used
        )
        PlanStore.save(tp, file)
        Settings.setLastPlan(file)
        return file
    }

    fun copyPlan(from: File, newName: String? = null): File {
        val src = PlanStore.loadFromFile(from)
        val renamed = if (!newName.isNullOrBlank())
            src.copy(plan = src.plan.copy(name = newName))
        else src
        val file = uniqueFileName(renamed.plan.name + " Copy")
        PlanStore.save(renamed, file)
        Settings.setLastPlan(file)
        return file
    }

    fun deletePlan(file: File): Boolean {
        val isLast = (platform.Settings.lastPlanFile()?.absolutePath == file.absolutePath)
        val ok = file.exists() && file.delete()
        if (ok && isLast) platform.Settings.setLastPlan(null)
        return ok
    }

    private fun uniqueFileName(baseName: String): File {
        val safe = baseName.trim().ifBlank { "plan" }
            .replace(Regex("""[^\w\-. ]+"""), "_")
        var idx = 0
        while (true) {
            val name = if (idx == 0) "$safe.yaml" else "$safe ($idx).yaml"
            val f = File(dir, name)
            if (!f.exists()) return f
            idx++
        }
    }
}
