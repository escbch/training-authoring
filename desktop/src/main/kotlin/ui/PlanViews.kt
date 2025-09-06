package ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import state.AppState
import state.Selection

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
            Text("Week ${w.week_index}", style = MaterialTheme.typography.titleMedium)
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
                        "    • ${sess.exercise_id}",
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
            Column(
                Modifier.fillMaxWidth().padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Day ${day.day_index}", style = MaterialTheme.typography.titleLarge)
                // TODO: make editable in next step
                Text("Name: ${day.name ?: "-"}")
                Text("Sessions: ${day.sessions.size}")
            }
        }

        is Selection.Exercise -> {
            val sess = tp.plan.weeks[sel.weekIdx].days[sel.dayIdx].sessions[sel.sessionIdx]
            Column(
                Modifier.fillMaxWidth().padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Exercise: ${sess.exercise_id}", style = MaterialTheme.typography.titleLarge)
                Text("Sets: ${sess.sets.size}")
                Text("Notes: ${sess.notes ?: "-"}")
            }
        }

        is Selection.SetSpecSel -> {
            val set = tp.plan.weeks[sel.w].days[sel.d].sessions[sel.s].sets[sel.setIdx]
            Column(
                Modifier.fillMaxWidth().padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Set ${sel.setIdx + 1}", style = MaterialTheme.typography.titleLarge)
                Text("Reps: ${set.reps}")
                Text("Target RPE: ${set.target_rpe ?: "-"}")
                Text("Rule: ${set.rule::class.simpleName}")
            }
        }

        is Selection.Week -> {
            // Placeholder for future week-level inspector
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Week ${sel.index + 1}")
            }
        }
    }
}
