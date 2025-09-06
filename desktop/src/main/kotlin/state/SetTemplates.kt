package state

import com.example.training.model.LoadRule
import com.example.training.model.SetSpec

sealed class SetTemplate {
    /** All sets are RPE-based with freely chosen weight. */
    data class FreeRpeAll(
        val sets: Int,
        val reps: Int,
        val targetRpe: Double
    ) : SetTemplate()

    /** First set anchors E1RM via target RPE; remaining sets are % of that E1RM. */
    data class AnchorThenPercent(
        val totalSets: Int,
        val reps: Int,
        val anchorRpe: Double,
        /** e.g. 0.70 for 70% */
        val percentOfE1rm: Double? = null,
        val percents: List<Double>? = null,
        val roundTo: Double = 2.5,
        val anchorIndex: Int = 1
    ) : SetTemplate()
}

fun buildSets(template: SetTemplate): List<SetSpec> = when (template) {
    is SetTemplate.FreeRpeAll -> List(template.sets) {
        SetSpec(
            reps = template.reps,
            target_rpe = template.targetRpe,
            rule = LoadRule.FREE_RPE
        )
    }
    is SetTemplate.AnchorThenPercent -> buildList {
        // Anchor set (index 1)
        add(
            SetSpec(
                reps = template.reps,
                target_rpe = template.anchorRpe,
                rule = LoadRule.ANCHOR_RPE_THEN_PERCENT_E1RM(
                    anchor_set_index = template.anchorIndex,
                    percent_of_e1rm = null, // anchor itself uses free weight + target RPE
                    round_to = template.roundTo
                )
            )
        )

        val followCount = (template.totalSets - 1).coerceAtLeast(0)
        val perList: List<Double> = when {
            !template.percents.isNullOrEmpty() -> template.percents
            template.percentOfE1rm != null -> List(followCount) { template.percentOfE1rm }
            else -> emptyList()
        }

        repeat(followCount) { i ->
            val pct = perList.getOrNull(i) ?: perList.lastOrNull()
            add(
                SetSpec(
                    reps = template.reps,
                    target_rpe = null,
                    rule = LoadRule.ANCHOR_RPE_THEN_PERCENT_E1RM(
                        anchor_set_index = template.anchorIndex,
                        percent_of_e1rm = pct,
                        round_to = template.roundTo
                    )
                )
            )
        }
    }
}
