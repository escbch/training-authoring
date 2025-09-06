package state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.training.model.*
import java.io.File
import java.util.*

sealed class Selection {
    data object None : Selection()
    data class Week(val index: Int) : Selection()
    data class Day(val weekIdx: Int, val dayIdx: Int) : Selection()
    data class Exercise(val weekIdx: Int, val dayIdx: Int, val sessionIdx: Int) : Selection()
    data class SetSpecSel(val w: Int, val d: Int, val s: Int, val setIdx: Int) : Selection()
}

class AppState {
    // PLAN
    var currentPlanFile: File? by mutableStateOf(null)
    var plan: TrainingPlan? by mutableStateOf(null)

    // LIBRARY
    var currentLibraryFile: File? by mutableStateOf(null)
    var library: List<ExerciseDef> by mutableStateOf(emptyList())

    var selection: Selection by mutableStateOf(Selection.None)

    fun newEmptyPlan(name: String = "Untitled", weeks: Int = 1, daysPerWeek: Int = 1) {
        val week1Days = (1..daysPerWeek).map { i -> PlanDay(i, "Day $i", emptyList()) }
        val weeksList = (1..weeks).map { w -> if (w == 1) PlanWeek(w, week1Days) else PlanWeek(w, emptyList()) }
        plan = TrainingPlan(
            plan = Plan(
                id = slugify(name),
                name = name,
                duration_weeks = weeks,
                days_per_week = daysPerWeek,
                weeks = weeksList
            ),
            exercise_library = emptyList()
        )
        selection = Selection.None
    }

    fun copyWeek1ToAllWeeks() {
        val tp = plan ?: return
        val w1 = tp.plan.weeks.firstOrNull() ?: return
        if (w1.days.isEmpty()) return
        val cloned = tp.plan.weeks.mapIndexed { idx, w ->
            if (idx == 0) w else w.copy(days = w1.days.map { it.copy(sessions = it.sessions.map { s -> s.copy(sets = s.sets.toList()) }) })
        }
        plan = tp.copy(plan = tp.plan.copy(weeks = cloned))
    }

    fun addExerciseToSelectedDay(exerciseId: String, template: SetTemplate) {
        val tp = plan ?: return
        val sel = selection as? Selection.Day ?: return
        val p = tp.plan
        val week = p.weeks[sel.weekIdx]
        val day = week.days[sel.dayIdx]

        val sets = buildSets(template)
        val newSess = com.example.training.model.Prescription(
            exercise_id = exerciseId,
            notes = null,
            sets = sets
        )

        val newDays = week.days.toMutableList()
        newDays[sel.dayIdx] = day.copy(sessions = day.sessions + newSess)
        val newWeeks = p.weeks.toMutableList()
        newWeeks[sel.weekIdx] = week.copy(days = newDays)

        plan = tp.copy(plan = p.copy(weeks = newWeeks))
        selection = Selection.Exercise(sel.weekIdx, sel.dayIdx, day.sessions.size) // select new exercise
    }

    fun addExerciseToLibrary(def: ExerciseDef) {
        val ids = library.map { it.id }.toSet()
        val unique = if (def.id in ids) def.copy(id = ensureUniqueId(def.id, ids)) else def
        library = library + unique
    }

    private fun ensureUniqueId(base: String, existing: Set<String>): String {
        var id = base; var n = 2
        while (existing.contains(id)) { id = "${base}_$n"; n++ }
        return id
    }
    private fun slugify(s: String): String =
        s.lowercase(Locale.getDefault()).replace(Regex("[^a-z0-9]+"), "_").trim('_')
}
