package state

import com.example.training.model.LoadRule
import com.example.training.model.SetSpec

sealed class SetTemplate {
    /** All sets are RPE-based with freely chosen weight. If [targetRpe] is null, it's "free" with no explicit target. */
    data class FreeRpeAll(
        val sets: Int,
        val reps: Int,
        val targetRpe: Double?
    ) : SetTemplate()

    /** Provide a (possibly shorter) list of RPEs; if shorter than [sets], pad by repeating the last value. */
    data class FreeRpeSeries(
        val sets: Int,
        val reps: Int,
        val targetRpes: List<Double>
    ) : SetTemplate()

    /** First set anchors E1RM via target RPE; remaining sets are % of that E1RM. You may either give a single percent or a list of per-set percents. */
    data class AnchorThenPercent(
        val totalSets: Int,
        val reps: Int,
        val anchorIndex: Int = 1,
        val percentOfE1rm: Double? = null,
        val percents: List<Double>? = null,
        val roundTo: Double = 2.5
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

    is SetTemplate.FreeRpeSeries -> {
        val list = template.targetRpes
        val lastIdx = (list.size - 1).coerceAtLeast(0)
        List(template.sets) { i ->
            val rpe = if (list.isEmpty()) null else list[i.coerceAtMost(lastIdx)]
            SetSpec(
                reps = template.reps,
                target_rpe = rpe,
                rule = LoadRule.FREE_RPE
            )
        }
    }

    is SetTemplate.AnchorThenPercent -> {
        // Priority: percents list, else single percent. Anchor always uses target_rpe on that set.
        val total = template.totalSets.coerceAtLeast(1)
        val list = template.percents?.takeIf { it.isNotEmpty() }
        val single = template.percentOfE1rm

        buildList {
            repeat(total) { idx0 ->
                val idx = idx0 + 1 // 1-based for anchor_index
                val pctForSet: Double? = when {
                    list != null -> list[(idx0).coerceAtMost(list.size - 1)]
                    else -> single
                }
                add(
                    SetSpec(
                        reps = template.reps,
                        target_rpe = if (idx == template.anchorIndex) (/* let RPE be provided at runtime or via UI */ null) else null,
                        rule = LoadRule.ANCHOR_RPE_THEN_PERCENT_E1RM(
                            anchor_set_index = template.anchorIndex,
                            percent_of_e1rm = pctForSet,
                            round_to = template.roundTo
                        )
                    )
                )
            }
        }
    }
}
