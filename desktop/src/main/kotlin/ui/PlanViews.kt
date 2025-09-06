package ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import state.AppState
import state.Selection

// Resolve a human-friendly exercise name from the global library; fallback to ID if unknown
private fun AppState.exerciseNameById(id: String): String =
    library.firstOrNull { it.id == id }?.name ?: id


@Composable
fun PlanTree(app: AppState) {
    val tp = app.plan
    if (tp == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Create or open a plan")
        }
        return
    }
    val plan = tp.plan
    LazyColumn(Modifier.fillMaxSize().padding(8.dp)) {
        itemsIndexed(plan.weeks) { wi, w ->
            Text("Week ${w.week_index}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.clickable { app.selection = Selection.Week(wi) })
            Spacer(Modifier.height(4.dp))

            if (w.days.isEmpty()) {
                Text(
                    "  (no days yet)",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            w.days.forEachIndexed { di, day ->
                Text(
                    "  Day ${day.day_index} • ${day.name ?: "Unnamed"}",
                    modifier = Modifier
                        .clickable { app.selection = Selection.Day(wi, di) }
                        .padding(4.dp)
                )

                day.sessions.forEachIndexed { si, sess ->
                    Text(
                        "    • ${app.exerciseNameById(sess.exercise_id)}",
                        modifier = Modifier
                            .clickable { app.selection = Selection.Exercise(wi, di, si) }
                            .padding(start = 12.dp, top = 2.dp, bottom = 2.dp)
                    )

                    sess.sets.forEachIndexed { seti, set ->
                        Text(
                            "       - ${set.reps} reps @ ${set.target_rpe ?: "RPE?"} (${set.rule::class.simpleName})",
                            modifier = Modifier
                                .clickable { app.selection = Selection.SetSpecSel(wi, di, si, seti) }
                                .padding(start = 20.dp, top = 1.dp, bottom = 1.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Divider()
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun Inspector(app: AppState) {
    val tp = app.plan ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No plan loaded") }
        return
    }

    when (val sel = app.selection) {
        is Selection.None -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Select something")
            }
        }

        is Selection.Day -> {
            val day = tp.plan.weeks[sel.weekIdx].days[sel.dayIdx]
            var nameText by remember(day.name) { mutableStateOf(day.name ?: "") }
            Column(
                Modifier.fillMaxWidth().padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Day ${day.day_index}", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    singleLine = true,
                    label = { Text("Day name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { app.updateDayName(sel.weekIdx, sel.dayIdx, nameText) }) { Text("Save name") }
                    TextButton(onClick = {
                        nameText = ""; app.updateDayName(
                        sel.weekIdx,
                        sel.dayIdx,
                        null
                    )
                    }) { Text("Clear") }
                }
                Divider()
                Text("Sessions: ${day.sessions.size}", style = MaterialTheme.typography.bodyMedium)
            }
        }

        is Selection.Exercise -> {
            val sess = tp.plan.weeks[sel.weekIdx].days[sel.dayIdx].sessions[sel.sessionIdx]
            var notesText by remember(sess.notes) { mutableStateOf(sess.notes ?: "") }
            Column(
                Modifier.fillMaxWidth().padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Exercise: ${app.exerciseNameById(sess.exercise_id)}", style = MaterialTheme.typography.titleLarge)

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        app.updateExerciseNotes(
                            sel.weekIdx,
                            sel.dayIdx,
                            sel.sessionIdx,
                            notesText
                        )
                    }) { Text("Save notes") }
                    TextButton(onClick = {
                        notesText = ""; app.updateExerciseNotes(
                        sel.weekIdx,
                        sel.dayIdx,
                        sel.sessionIdx,
                        null
                    )
                    }) { Text("Clear") }
                }

                Divider()
                Text("Sets: ${sess.sets.size}", style = MaterialTheme.typography.titleMedium)
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    sess.sets.forEachIndexed { idx, set ->
                        Column(verticalArrangement = Arrangement.SpaceBetween) {
                            Text("Set ${idx + 1}: ${set.reps} reps @ ${set.target_rpe ?: "RPE?"} (${set.rule::class.simpleName})")
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                OutlinedButton(onClick = {
                                    app.duplicateSet(
                                        sel.weekIdx,
                                        sel.dayIdx,
                                        sel.sessionIdx,
                                        idx
                                    )
                                }) { Text("Duplicate") }
                                TextButton(onClick = {
                                    app.removeSet(
                                        sel.weekIdx,
                                        sel.dayIdx,
                                        sel.sessionIdx,
                                        idx
                                    )
                                }) { Text("Delete") }
                            }
                        }
                    }
                }
                Button(onClick = {
                    app.addSetDuplicateLast(
                        sel.weekIdx,
                        sel.dayIdx,
                        sel.sessionIdx
                    )
                }) { Text("Add set") }
            }
        }


        is Selection.SetSpecSel -> {
            val set = tp.plan.weeks[sel.w].days[sel.d].sessions[sel.s].sets[sel.setIdx]
            var repsText by remember(set.reps) { mutableStateOf(set.reps.toString()) }
            var rpeText by remember(set.target_rpe) { mutableStateOf(set.target_rpe?.toString() ?: "") }
            Column(
                Modifier.fillMaxWidth().padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Set ${sel.setIdx + 1}", style = MaterialTheme.typography.titleLarge)
                Text("Rule: ${set.rule::class.simpleName}", style = MaterialTheme.typography.bodySmall)

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = repsText,
                        onValueChange = { repsText = it.filter { c -> c.isDigit() }.take(4) },
                        singleLine = true,
                        label = { Text("Reps") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = rpeText,
                        onValueChange = { rpeText = it },
                        singleLine = true,
                        label = { Text("Target RPE (blank = free)") },
                        supportingText = { Text("Typical 5.0–10.0; ',' or '.' allowed") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        repsText.toIntOrNull()?.let { app.updateSetReps(sel.w, sel.d, sel.s, sel.setIdx, it) }
                        val rpeVal = rpeText.trim().replace(',', '.').toDoubleOrNull()
                        app.updateSetRpe(sel.w, sel.d, sel.s, sel.setIdx, rpeVal)
                    }) { Text("Save") }

                    TextButton(onClick = {
                        repsText = set.reps.toString()
                        rpeText = set.target_rpe?.toString() ?: ""
                    }) { Text("Reset") }
                }
            }
        }

        is Selection.Week -> {
            val totalWeeks = tp.plan.weeks.size
            val week1Based = sel.index + 1

            var showCopySingle by remember { mutableStateOf(false) }
            var showCopyMulti by remember { mutableStateOf(false) }

            Column(
                Modifier.fillMaxWidth().padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Week $week1Based", style = MaterialTheme.typography.titleLarge)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { showCopySingle = true }) { Text("Copy to week…") }
                    OutlinedButton(onClick = { showCopyMulti = true }) { Text("Copy to weeks…") }
                    if (sel.index == 0 && totalWeeks > 1) {
                        TextButton(onClick = { app.copyWeek1ToAllWeeks() }) { Text("Copy to all weeks") }
                    }
                }

                Divider()
                Text(
                    "Tip: you can click a week title in the tree to select it.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (showCopySingle) {
                ui.dialog.CopyWeekDialog(
                    totalWeeks = totalWeeks,
                    defaultFrom = week1Based,
                    onCancel = { showCopySingle = false },
                    onConfirm = { from, to ->
                        app.copyWeek(from, to)
                        showCopySingle = false
                    }
                )
            }

            if (showCopyMulti) {
                ui.dialog.CopyWeekMultiDialog(
                    totalWeeks = totalWeeks,
                    sourceWeek = week1Based,
                    onCancel = { showCopyMulti = false },
                    onConfirm = { targets ->
                        targets.forEach { t -> app.copyWeek(week1Based, t) }
                        showCopyMulti = false
                    }
                )
            }
        }
    }
}
