package ui.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CopyWeekMultiDialog(
    totalWeeks: Int,
    sourceWeek: Int, // 1-based
    onCancel: () -> Unit,
    onConfirm: (targets: List<Int>) -> Unit
) {
    val candidates = remember(totalWeeks, sourceWeek) { (1..totalWeeks).filter { it != sourceWeek } }
    var selected by remember { mutableStateOf(setOf<Int>()) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Copy Week $sourceWeek → Multiple") },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (candidates.isEmpty()) {
                    Text("No other weeks to copy to.")
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { selected = candidates.toSet() }, enabled = candidates.isNotEmpty()) { Text("Select all") }
                        TextButton(onClick = { selected = emptySet() }, enabled = selected.isNotEmpty()) { Text("Clear") }
                    }
                    Divider()
                    LazyColumn(Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 280.dp)) {
                        items(candidates) { w ->
                            val checked = w in selected
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Week $w", modifier = Modifier.weight(1f))
                                Checkbox(checked = checked, onCheckedChange = {
                                    selected = if (it == true) selected + w else selected - w
                                })
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selected.sorted()) }, enabled = selected.isNotEmpty()) { Text("Copy") }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } }
    )
}