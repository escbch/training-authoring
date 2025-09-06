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

    fun copyWeek(fromWeekIdx1Based: Int, toWeekIdx1Based: Int) {
        val tp = plan ?: return
        val p = tp.plan
        val fromIdx = fromWeekIdx1Based - 1
        val toIdx = toWeekIdx1Based - 1
        if (fromIdx !in p.weeks.indices || toIdx !in p.weeks.indices) return
        val source = p.weeks[fromIdx]

        // Deep-ish copy: new lists for days/sessions/sets
        val newDays = source.days.map { day ->
            day.copy(
                sessions = day.sessions.map { s ->
                    s.copy(
                        sets = s.sets.map { it.copy() } // SetSpec is a data class; rule objects are fine as-is
                    )
                }
            )
        }

        val newWeeks = p.weeks.toMutableList()
        newWeeks[toIdx] = p.weeks[toIdx].copy(days = newDays)
        plan = tp.copy(plan = p.copy(weeks = newWeeks))
    }

    fun copySelectedWeekTo(targetWeekIdx1Based: Int) {
        val current = (selection as? Selection.Week)?.index ?: return
        copyWeek(current + 1, targetWeekIdx1Based)
    }

    fun copyWeek1ToAllWeeks() {
        val tp = plan ?: return
        val p = tp.plan
        if (p.weeks.isEmpty()) return
        val w1 = p.weeks.first()

        val newDays = w1.days.map { day ->
            day.copy(
                sessions = day.sessions.map { s ->
                    s.copy(
                        sets = s.sets.map { it.copy() }
                    )
                }
            )
        }

        val newWeeks = p.weeks.mapIndexed { idx, w ->
            if (idx == 0) w else w.copy(days = newDays)
        }
        plan = tp.copy(plan = p.copy(weeks = newWeeks))
    }


    fun updateDayName(weekIdx: Int, dayIdx: Int, newName: String?) {
        val tp = plan ?: return
        val p = tp.plan
        if (weekIdx !in p.weeks.indices) return
        val week = p.weeks[weekIdx]
        if (dayIdx !in week.days.indices) return
        val old = week.days[dayIdx]
        val newDay = old.copy(name = newName?.ifBlank { null })
        val newDays = week.days.toMutableList().also { it[dayIdx] = newDay }
        val newWeeks = p.weeks.toMutableList().also { it[weekIdx] = week.copy(days = newDays) }
        plan = tp.copy(plan = p.copy(weeks = newWeeks))
    }

    fun updateExerciseNotes(weekIdx: Int, dayIdx: Int, sessionIdx: Int, notes: String?) {
        val tp = plan ?: return
        val p = tp.plan
        if (weekIdx !in p.weeks.indices) return
        val week = p.weeks[weekIdx]
        if (dayIdx !in week.days.indices) return
        val day = week.days[dayIdx]
        if (sessionIdx !in day.sessions.indices) return
        val sess = day.sessions[sessionIdx]
        val newSess = sess.copy(notes = notes?.ifBlank { null })
        val newSessions = day.sessions.toMutableList().also { it[sessionIdx] = newSess }
        val newDays = week.days.toMutableList().also { it[dayIdx] = day.copy(sessions = newSessions) }
        val newWeeks = p.weeks.toMutableList().also { it[weekIdx] = week.copy(days = newDays) }
        plan = tp.copy(plan = p.copy(weeks = newWeeks))
    }

    fun addSetDuplicateLast(weekIdx: Int, dayIdx: Int, sessionIdx: Int) {
        val tp = plan ?: return
        val p = tp.plan
        if (weekIdx !in p.weeks.indices) return
        val week = p.weeks[weekIdx]
        if (dayIdx !in week.days.indices) return
        val day = week.days[dayIdx]
        if (sessionIdx !in day.sessions.indices) return
        val sess = day.sessions[sessionIdx]
        val newSet = sess.sets.lastOrNull()?.copy() ?: SetSpec(reps = 5, target_rpe = null, rule = LoadRule.FREE_RPE)
        val newSessions = day.sessions.toMutableList().also { it[sessionIdx] = sess.copy(sets = sess.sets + newSet) }
        val newDays = week.days.toMutableList().also { it[dayIdx] = day.copy(sessions = newSessions) }
        val newWeeks = p.weeks.toMutableList().also { it[weekIdx] = week.copy(days = newDays) }
        plan = tp.copy(plan = p.copy(weeks = newWeeks))
    }

    fun duplicateSet(weekIdx: Int, dayIdx: Int, sessionIdx: Int, setIdx: Int) {
        val tp = plan ?: return
        val p = tp.plan
        if (weekIdx !in p.weeks.indices) return
        val week = p.weeks[weekIdx]
        if (dayIdx !in week.days.indices) return
        val day = week.days[dayIdx]
        if (sessionIdx !in day.sessions.indices) return
        val sess = day.sessions[sessionIdx]
        if (setIdx !in sess.sets.indices) return
        val clone = sess.sets[setIdx].copy()
        val newSetList = sess.sets.toMutableList().also { it.add(setIdx + 1, clone) }
        val newSessions = day.sessions.toMutableList().also { it[sessionIdx] = sess.copy(sets = newSetList) }
        val newDays = week.days.toMutableList().also { it[dayIdx] = day.copy(sessions = newSessions) }
        val newWeeks = p.weeks.toMutableList().also { it[weekIdx] = week.copy(days = newDays) }
        plan = tp.copy(plan = p.copy(weeks = newWeeks))
    }

    fun removeSet(weekIdx: Int, dayIdx: Int, sessionIdx: Int, setIdx: Int) {
        val tp = plan ?: return
        val p = tp.plan
        if (weekIdx !in p.weeks.indices) return
        val week = p.weeks[weekIdx]
        if (dayIdx !in week.days.indices) return
        val day = week.days[dayIdx]
        if (sessionIdx !in day.sessions.indices) return
        val sess = day.sessions[sessionIdx]
        if (setIdx !in sess.sets.indices) return
        if (sess.sets.size <= 1) {
            // allow removing last set? yes, resulting exercise can have zero sets
        }
        val newSetList = sess.sets.toMutableList().also { it.removeAt(setIdx) }
        val newSessions = day.sessions.toMutableList().also { it[sessionIdx] = sess.copy(sets = newSetList) }
        val newDays = week.days.toMutableList().also { it[dayIdx] = day.copy(sessions = newSessions) }
        val newWeeks = p.weeks.toMutableList().also { it[weekIdx] = week.copy(days = newDays) }
        plan = tp.copy(plan = p.copy(weeks = newWeeks))
    }

    fun updateSetReps(weekIdx: Int, dayIdx: Int, sessionIdx: Int, setIdx: Int, reps: Int) {
        val tp = plan ?: return
        val p = tp.plan
        if (weekIdx !in p.weeks.indices) return
        val week = p.weeks[weekIdx]
        if (dayIdx !in week.days.indices) return
        val day = week.days[dayIdx]
        if (sessionIdx !in day.sessions.indices) return
        val sess = day.sessions[sessionIdx]
        if (setIdx !in sess.sets.indices) return

        val newSet = sess.sets[setIdx].copy(reps = reps.coerceAtLeast(1))
        val newSets = sess.sets.toMutableList().also { it[setIdx] = newSet }
        val newSessions = day.sessions.toMutableList().also { it[sessionIdx] = sess.copy(sets = newSets) }
        val newDays = week.days.toMutableList().also { it[dayIdx] = day.copy(sessions = newSessions) }
        val newWeeks = p.weeks.toMutableList().also { it[weekIdx] = week.copy(days = newDays) }
        plan = tp.copy(plan = p.copy(weeks = newWeeks))
    }

    fun updateSetRpe(weekIdx: Int, dayIdx: Int, sessionIdx: Int, setIdx: Int, rpe: Double?) {
        val tp = plan ?: return
        val p = tp.plan
        if (weekIdx !in p.weeks.indices) return
        val week = p.weeks[weekIdx]
        if (dayIdx !in week.days.indices) return
        val day = week.days[dayIdx]
        if (sessionIdx !in day.sessions.indices) return
        val sess = day.sessions[sessionIdx]
        if (setIdx !in sess.sets.indices) return

        val newSet = sess.sets[setIdx].copy(target_rpe = rpe)
        val newSets = sess.sets.toMutableList().also { it[setIdx] = newSet }
        val newSessions = day.sessions.toMutableList().also { it[sessionIdx] = sess.copy(sets = newSets) }
        val newDays = week.days.toMutableList().also { it[dayIdx] = day.copy(sessions = newSessions) }
        val newWeeks = p.weeks.toMutableList().also { it[weekIdx] = week.copy(days = newDays) }
        plan = tp.copy(plan = p.copy(weeks = newWeeks))
    }

}
