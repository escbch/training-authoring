package ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.training.model.ExerciseDef
import com.example.training.model.Muscles

@Composable
fun ExerciseDialog(
    onDismiss: () -> Unit,
    onCreate: (ExerciseDef) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var tempo by remember { mutableStateOf("") }
    var primary by remember { mutableStateOf("Chest") }
    var secondary by remember { mutableStateOf("") }
    var tertiary by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }

    val id = remember(name) { name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_') }
    val valid = name.isNotBlank() && primary.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                onCreate(
                    ExerciseDef(
                        id = id.ifBlank { "ex_${System.currentTimeMillis()}" },
                        name = name.trim(),
                        tempo = tempo.ifBlank { null },
                        aliases = emptyList(),
                        muscles = Muscles(
                            primary = primary.trim(),
                            secondary = secondary.ifBlank { null },
                            tertiary = tertiary.ifBlank { null }
                        ),
                        tags = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    )
                )
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("New Exercise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                OutlinedTextField(value = tempo, onValueChange = { tempo = it }, label = { Text("Tempo (optional)") })
                OutlinedTextField(value = primary, onValueChange = { primary = it }, label = { Text("Primary muscle") })
                OutlinedTextField(value = secondary, onValueChange = { secondary = it }, label = { Text("Secondary muscle (optional)") })
                OutlinedTextField(value = tertiary, onValueChange = { tertiary = it }, label = { Text("Tertiary muscle (optional)") })
                OutlinedTextField(value = tags, onValueChange = { tags = it }, label = { Text("Tags (comma-separated)") })
                Text("ID (auto): $id", style = MaterialTheme.typography.bodySmall)
            }
        }
    )
}
