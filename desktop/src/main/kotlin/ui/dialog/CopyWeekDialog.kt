
package ui.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CopyWeekDialog(
    totalWeeks: Int,
    defaultFrom: Int? = null, // 1-based
    onCancel: () -> Unit,
    onConfirm: (fromWeek1: Int, toWeek1: Int) -> Unit
) {
    var from by remember { mutableStateOf((defaultFrom ?: 1).coerceIn(1, totalWeeks).toString()) }
    var to by remember { mutableStateOf("2") }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Copy Week") },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Select source and target weeks (1..$totalWeeks).")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = from,
                        onValueChange = { s -> from = s.filter { it.isDigit() }.take(2) },
                        label = { Text("From (week)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = to,
                        onValueChange = { s -> to = s.filter { it.isDigit() }.take(2) },
                        label = { Text("To (week)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val f = from.toIntOrNull()?.coerceIn(1, totalWeeks) ?: 1
                val t = to.toIntOrNull()?.coerceIn(1, totalWeeks) ?: 1
                if (f != t) onConfirm(f, t) else onCancel()
            }) { Text("Copy") }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } }
    )
}
