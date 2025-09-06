package ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import state.SetTemplate

enum class Mode { FREE_RPE, ANCHOR_PERCENT }

@Composable
fun AddToDayDialog(
    exerciseName: String,
    onDismiss: () -> Unit,
    onCreate: (SetTemplate) -> Unit
) {
    // Mode
    var mode by remember { mutableStateOf(Mode.ANCHOR_PERCENT) }

    // Common inputs
    var sets by remember { mutableStateOf("3") }
    var reps by remember { mutableStateOf("5") }

    // Free RPE inputs
    var freeTargetRpe by remember { mutableStateOf("7.0") }

    // Anchor → %E1RM inputs
    var anchorRpe by remember { mutableStateOf("7.5") }
    var percent by remember { mutableStateOf("0.70") } // 70% as 0.70
    var percentsText by remember { mutableStateOf("") }
    var roundTo by remember { mutableStateOf("2.5") }

    fun parsePercents(text: String): List<Double>? {
        if (text.isBlank()) return null
        val tokens = text.split(',', ';', ' ')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return null
        val list = tokens.map {
            it.replace(',', '.').toDoubleOrNull() ?: return null
        }
        return list
    }

    fun canConfirm(): Boolean {
        val setCount = sets.toIntOrNull() ?: return false
        val repsCount = reps.toIntOrNull() ?: return false
        if (setCount !in 1..20 || repsCount !in 1..50) return false

        return when (mode) {
            Mode.FREE_RPE ->
                freeTargetRpe.toDoubleOrNull() != null

            Mode.ANCHOR_PERCENT -> {
                val anchorOk = anchorRpe.toDoubleOrNull() != null
                val list = parsePercents(percentsText)
                val listOk = !list.isNullOrEmpty() && list.all { it in 0.3..1.0 }
                val singleOk = percent.replace(',', '.').toDoubleOrNull()?.let { it in 0.3..1.0 } == true

                // If only 1 set (just the anchor), no percents are required.
                anchorOk && (setCount == 1 || listOk || singleOk)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add “$exerciseName” to Day") },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FilterChip(
                        selected = mode == Mode.ANCHOR_PERCENT,
                        onClick = { mode = Mode.ANCHOR_PERCENT },
                        label = { Text("Anchor 1st → %E1RM") }
                    )
                    FilterChip(
                        selected = mode == Mode.FREE_RPE,
                        onClick = { mode = Mode.FREE_RPE },
                        label = { Text("Free RPE (all)") }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(sets, { sets = it }, label = { Text("Sets") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(reps, { reps = it }, label = { Text("Reps") }, modifier = Modifier.weight(1f))
                }

                when (mode) {
                    Mode.FREE_RPE -> {
                        OutlinedTextField(
                            value = freeTargetRpe,
                            onValueChange = { freeTargetRpe = it },
                            label = { Text("Target RPE (all sets)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("All sets use target RPE with freely chosen weight.", style = MaterialTheme.typography.bodySmall)
                    }
                    Mode.ANCHOR_PERCENT -> {
                        OutlinedTextField(
                            value = anchorRpe,
                            onValueChange = { anchorRpe = it },
                            label = { Text("Anchor 1st set – target RPE") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        // Per-set percents (optional)
                        OutlinedTextField(
                            value = percentsText,
                            onValueChange = { percentsText = it },
                            label = { Text("Percents for following sets (e.g. 0.70, 0.60)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        // Single percent fallback
                        OutlinedTextField(
                            value = percent,
                            onValueChange = { percent = it },
                            label = { Text("Single percent for all (e.g. 0.70)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = roundTo,
                                onValueChange = { roundTo = it },
                                label = { Text("Round to (kg)") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Text("If both fields are set, the per-set list wins. If you only want one set, leave percents empty.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = canConfirm(), onClick = {
                val setCount = sets.toInt()
                val repsCount = reps.toInt()
                val list = parsePercents(percentsText)

                val tpl = when (mode) {
                    Mode.FREE_RPE -> SetTemplate.FreeRpeAll(
                        sets = setCount,
                        reps = repsCount,
                        targetRpe = freeTargetRpe.replace(',', '.').toDouble()
                    )
                    Mode.ANCHOR_PERCENT -> SetTemplate.AnchorThenPercent(
                        totalSets = setCount,
                        reps = repsCount,
                        anchorRpe = anchorRpe.replace(',', '.').toDouble(),
                        // if a per-set list is provided, it wins; otherwise use the single percent
                        percents = list,
                        percentOfE1rm = if (list.isNullOrEmpty())
                            percent.replace(',', '.').toDoubleOrNull()
                        else null,
                        roundTo = roundTo.replace(',', '.').toDouble()
                    )
                }
                onCreate(tpl)
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
