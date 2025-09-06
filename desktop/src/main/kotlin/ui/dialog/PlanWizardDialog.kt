package ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PlanWizardDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, durationWeeks: Int, daysPerWeek: Int) -> Unit
) {
    var name by remember { mutableStateOf("New Plan") }
    var weeksText by remember { mutableStateOf("4") }
    var daysText by remember { mutableStateOf("4") }
    val valid = name.isNotBlank() && weeksText.toIntOrNull()?.let { it in 1..52 } == true &&
            daysText.toIntOrNull()?.let { it in 1..7 } == true

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                onCreate(name, weeksText.toInt(), daysText.toInt())
            }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Create Plan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Plan name") })
                OutlinedTextField(value = weeksText, onValueChange = { weeksText = it }, label = { Text("Duration (weeks)") })
                OutlinedTextField(value = daysText, onValueChange = { daysText = it }, label = { Text("Days per week") })
                Text("Tip: week 1 will be created; you can copy it to all weeks later.")
            }
        }
    )
}
