package com.example.training.model

import kotlinx.serialization.Serializable

@Serializable
data class TrainingPlan(
    val schema_version: String = "v1.0",
    val plan: Plan,
    val exercise_library: List<ExerciseDef> = emptyList()
)

@Serializable
data class ExerciseDef(
    val id: String,
    val name: String,
    val tempo: String? = null,
    val aliases: List<String> = emptyList(),
    val muscles: Muscles,
    val tags: List<String> = emptyList()
)


@Serializable
data class Muscles(
    val primary: String,
    val secondary: String? = null,
    val tertiary: String? = null
)

@Serializable
data class Plan(
    val id: String,
    val name: String,
    val duration_weeks: Int,
    val days_per_week: Int,
    val e1rm_model: E1rmModel = E1rmModel.RpeTable(),
    val weeks: List<PlanWeek>
)

@Serializable data class PlanWeek(val week_index: Int, val days: List<PlanDay>)
@Serializable data class PlanDay(val day_index: Int, val name: String? = null, val sessions: List<Prescription>)
@Serializable data class Prescription(val exercise_id: String, val notes: String? = null, val sets: List<SetSpec>)

@Serializable
data class SetSpec(
    val reps: Int,
    val target_rpe: Double? = null,
    val rule: LoadRule
)

@Serializable
sealed class LoadRule {
    @Serializable
    data object FREE_RPE : LoadRule()

    @Serializable
    data class ANCHOR_RPE_THEN_PERCENT_E1RM(
        val anchor_set_index: Int = 1,
        val percent_of_e1rm: Double? = null,
        val round_to: Double? = 2.5
    ) : LoadRule()
}

@Serializable
sealed class E1rmModel {
    @Serializable data class RpeTable(val source: String = "default_rir_to_percent_table") : E1rmModel()
    @Serializable data class Epley(val unused: Boolean = false) : E1rmModel()
    @Serializable data class Brzycki(val unused: Boolean = false) : E1rmModel()
}