package ui.dialog

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
    var mode by remember { mutableStateOf(Mode.FREE_RPE) }

    // Common inputs
    var sets by remember { mutableStateOf("3") }
    var reps by remember { mutableStateOf("5") }

    // FREE_RPE inputs (series)
    var rpesText by remember { mutableStateOf("") } // e.g., "8, 8, 7.5" or "8"

    // ANCHOR_PERCENT inputs
    var anchorIndex by remember { mutableStateOf("1") }
    var percent by remember { mutableStateOf("70") } // fallback if list empty
    var percentsText by remember { mutableStateOf("") } // "70, 65, 60"
    var roundTo by remember { mutableStateOf("2.5") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add “$exerciseName” to Day") },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Mode selector
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(selected = mode == Mode.FREE_RPE, onClick = { mode = Mode.FREE_RPE }, label = { Text("Target RPE per set") })
                    FilterChip(selected = mode == Mode.ANCHOR_PERCENT, onClick = { mode = Mode.ANCHOR_PERCENT }, label = { Text("Anchor → %E1RM") })
                }
                Divider()

                // Sets/Reps
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = sets, onValueChange = { sets = it.filter { c -> c.isDigit() }.take(2) }, singleLine = true, label = { Text("Sets") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = reps, onValueChange = { reps = it.filter { c -> c.isDigit() }.take(3) }, singleLine = true, label = { Text("Reps") }, modifier = Modifier.weight(1f))
                }

                when (mode) {
                    Mode.FREE_RPE -> {
                        OutlinedTextField(
                            value = rpesText,
                            onValueChange = { rpesText = it },
                            label = { Text("Target RPE(s) — comma-separated (e.g., 8, 8, 7.5). One value applies to all.") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Mode.ANCHOR_PERCENT -> {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(value = anchorIndex, onValueChange = { anchorIndex = it.filter { c -> c.isDigit() }.take(2) }, singleLine = true, label = { Text("Anchor set index") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = roundTo, onValueChange = { roundTo = it.filter { c -> c.isDigit() || c=='.' || c==',' }.take(6) }, singleLine = true, label = { Text("Round to (kg)") }, modifier = Modifier.weight(1f))
                        }
                        OutlinedTextField(
                            value = percentsText,
                            onValueChange = { percentsText = it },
                            label = { Text("Percent(s) of E1RM — comma-separated (e.g., 70, 65, 60)") },
                            supportingText = { Text("If left blank, will use single % below for all sets.") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = percent,
                            onValueChange = { percent = it.filter { c -> c.isDigit() || c=='.' || c==',' }.take(6) },
                            singleLine = true,
                            label = { Text("Single % of E1RM (fallback)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val s = sets.toIntOrNull()?.coerceIn(1, 99) ?: 1
                val r = reps.toIntOrNull()?.coerceIn(1, 999) ?: 1
                val tpl: SetTemplate = when (mode) {
                    Mode.FREE_RPE -> {
                        val list = rpesText.split(',', ' ', ';')
                            .mapNotNull { it.trim().replace(',', '.').toDoubleOrNull() }
                            .filter { it > 0.0 }
                        if (list.isEmpty()) {
                            SetTemplate.FreeRpeAll(sets = s, reps = r, targetRpe = null)
                        } else if (list.size == 1) {
                            SetTemplate.FreeRpeAll(sets = s, reps = r, targetRpe = list.first())
                        } else {
                            SetTemplate.FreeRpeSeries(sets = s, reps = r, targetRpes = list)
                        }
                    }
                    Mode.ANCHOR_PERCENT -> {
                        val list = percentsText.split(',', ' ', ';')
                            .mapNotNull { it.trim().replace(',', '.').toDoubleOrNull() }
                            .filter { it > 0.0 }
                        SetTemplate.AnchorThenPercent(
                            totalSets = s,
                            reps = r,
                            anchorIndex = anchorIndex.toIntOrNull()?.coerceIn(1, s) ?: 1,
                            // if a per-set list is provided, it wins; otherwise use the single percent
                            percents = list.ifEmpty { null },
                            percentOfE1rm = if (list.isEmpty()) percent.replace(',', '.').toDoubleOrNull() else null,
                            roundTo = roundTo.replace(',', '.').toDoubleOrNull() ?: 2.5
                        )
                    }
                }
                onCreate(tpl)
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
