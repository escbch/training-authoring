
package ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.training.model.E1rmModel
import data.PlanIndex
import java.io.File

data class NewPlanParams(
    val name: String,
    val durationWeeks: Int,
    val daysPerWeek: Int,
    val e1rmModel: E1rmModel
)

@Composable
fun OverviewScreen(
    modifier: Modifier = Modifier,
    onCreatePlan: (NewPlanParams) -> Unit,
    onEditPlan: (File) -> Unit,
    onCopyPlan: (File) -> Unit
) {
    var plans by remember { mutableStateOf(PlanIndex.listPlanFiles()) }
    var showNew by remember { mutableStateOf(false) }

    fun refresh() { plans = PlanIndex.listPlanFiles() }

    if (showNew) {
        NewPlanDialog(
            onCancel = { showNew = false },
            onCreate = { params ->
                onCreatePlan(params)
                showNew = false
                refresh()
            }
        )
    }

    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Dashboard", style = MaterialTheme.typography.headlineSmall)
        Divider()
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Plans list
            Card(Modifier.weight(2f).fillMaxHeight()) {
                Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Plans", style = MaterialTheme.typography.titleLarge)
                        Button(onClick = { showNew = true }) { Text("New Plan") }
                    }
                    Divider()
                    if (plans.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No plans yet. Click “New Plan” to get started.")
                        }
                    } else {
                        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(plans, key = { it.absolutePath }) { f ->
                                val title = remember(f) { PlanIndex.readPlanTitle(f) }
                                PlanRow(
                                    title = title,
                                    file = f,
                                    onOpen = { onEditPlan(f) },
                                    onCopy = {
                                        onCopyPlan(f)
                                        refresh()
                                    }
                                )
                                Divider()
                            }
                        }
                    }
                }
            }

            // Universal Exercise Library editor on the right
            Card(Modifier.weight(1f).fillMaxHeight()) {
                LibraryEditor(Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun PlanRow(
    title: String,
    file: File,
    onOpen: () -> Unit,
    onCopy: () -> Unit
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).clickable { onOpen() }) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(file.name, style = MaterialTheme.typography.bodySmall)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCopy) { Text("Copy") }
            Button(onClick = onOpen) { Text("Edit") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewPlanDialog(
    onCancel: () -> Unit,
    onCreate: (NewPlanParams) -> Unit
) {
    var name by remember { mutableStateOf("Untitled Plan") }
    var duration by remember { mutableStateOf("4") }
    var days by remember { mutableStateOf("3") }
    var menuOpen by remember { mutableStateOf(false) }
    var e1rmChoice by remember { mutableStateOf("RpeTable") }

    AlertDialog(
        onDismissRequest = onCancel,
        confirmButton = {
            Button(onClick = {
                val durationWeeks = duration.toIntOrNull()?.coerceIn(1, 52) ?: 4
                val daysPerWeek = days.toIntOrNull()?.coerceIn(1, 7) ?: 3
                val model = when (e1rmChoice) {
                    "Epley" -> E1rmModel.Epley()
                    "Brzycki" -> E1rmModel.Brzycki()
                    else -> E1rmModel.RpeTable()
                }
                onCreate(NewPlanParams(
                    name = name.ifBlank { "Untitled Plan" },
                    durationWeeks = durationWeeks,
                    daysPerWeek = daysPerWeek,
                    e1rmModel = model
                ))
            }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } },
        title = { Text("New Plan") },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { s -> duration = s.filter { it.isDigit() }.take(2) },
                        singleLine = true,
                        label = { Text("Duration (weeks)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = days,
                        onValueChange = { s -> days = s.filter { it.isDigit() }.take(1) },
                        singleLine = true,
                        label = { Text("Days / week") },
                        modifier = Modifier.weight(1f)
                    )
                }
                ExposedDropdownMenuBox(
                    expanded = menuOpen,
                    onExpandedChange = { menuOpen = !menuOpen }
                ) {
                    OutlinedTextField(
                        value = e1rmChoice,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("E1RM Model") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuOpen) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        listOf("RpeTable", "Epley", "Brzycki").forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = { e1rmChoice = opt; menuOpen = false }
                            )
                        }
                    }
                }
            }
        }
    )
}
