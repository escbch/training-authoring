
package ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.training.model.ExerciseDef
import com.example.training.model.Muscles
import data.ExerciseLibraryStore
import platform.Settings
import java.io.File
import java.util.UUID

@Composable
fun LibraryEditor(
    modifier: Modifier = Modifier
) {
    val file = remember { ensureLibraryFile() }
    var library by remember { mutableStateOf(load(file)) }
    var showDialog by remember { mutableStateOf(false) }
    var editing: ExerciseDef? by remember { mutableStateOf(null) }

    fun saveLib(newList: List<ExerciseDef>) {
        library = newList
        ExerciseLibraryStore.save(newList, file)
        Settings.setLastLibrary(file)
    }

    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Exercise Library", style = MaterialTheme.typography.titleLarge)
            Button(onClick = { editing = null; showDialog = true }) { Text("+ Exercise") }
        }
        Divider()

        if (library.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No exercises yet.")
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(library, key = { it.id }) { ex ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(ex.name, style = MaterialTheme.typography.titleMedium)
                            val subs = listOfNotNull(ex.muscles.secondary, ex.muscles.tertiary)
                                .filter { it.isNotBlank() }
                                .joinToString(" • ")
                            Text(ex.muscles.primary + if (subs.isNotBlank()) " — $subs" else "", style = MaterialTheme.typography.bodySmall)
                            if (ex.tags.isNotEmpty()) Text(ex.tags.joinToString(", "), style = MaterialTheme.typography.labelSmall)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { editing = ex; showDialog = true }) { Text("Edit") }
                            TextButton(onClick = { saveLib(library.filter { it.id != ex.id }) }) { Text("Delete") }
                        }
                    }
                    Divider()
                }
            }
        }
    }

    if (showDialog) {
        ExerciseDialog(
            initial = editing,
            onCancel = { showDialog = false },
            onSave = { updated ->
                val list = if (editing == null) library + updated else library.map { if (it.id == updated.id) updated else it }
                saveLib(list.sortedBy { it.name.lowercase() })
                showDialog = false
                editing = null
            }
        )
    }
}

private fun ensureLibraryFile(): File {
    val f = Settings.lastLibraryFile() ?: Settings.defaultLibraryFile().also {
        it.parentFile.mkdirs()
        if (!it.exists()) ExerciseLibraryStore.save(emptyList(), it)
        Settings.setLastLibrary(it)
    }
    return f
}

private fun load(file: File): List<ExerciseDef> =
    runCatching { ExerciseLibraryStore.loadFromFile(file) }
        .getOrElse { emptyList() }

@Composable
private fun ExerciseDialog(
    initial: ExerciseDef?,
    onCancel: () -> Unit,
    onSave: (ExerciseDef) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var primary by remember { mutableStateOf(initial?.muscles?.primary ?: "") }
    var secondary by remember { mutableStateOf(initial?.muscles?.secondary ?: "") }
    var tertiary by remember { mutableStateOf(initial?.muscles?.tertiary ?: "") }
    var tagsText by remember { mutableStateOf(initial?.tags?.joinToString(", ") ?: "") }
    var tempo by remember { mutableStateOf(initial?.tempo ?: "") }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(if (initial == null) "New Exercise" else "Edit Exercise") },
        confirmButton = {
            Button(onClick = {
                val id = initial?.id ?: UUID.randomUUID().toString()
                val tags = tagsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                onSave(
                    ExerciseDef(
                        id = id,
                        name = name.trim().ifBlank { "Untitled Exercise" },
                        tempo = tempo.ifBlank { null },
                        muscles = Muscles(
                            primary = primary.trim().ifBlank { "Other" },
                            secondary = secondary.ifBlank { null },
                            tertiary = tertiary.ifBlank { null }
                        ),
                        tags = tags
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(value = tempo, onValueChange = { tempo = it }, label = { Text("Tempo (optional)") }, singleLine = true)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = primary, onValueChange = { primary = it }, label = { Text("Primary muscle") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = secondary, onValueChange = { secondary = it }, label = { Text("Secondary (optional)") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = tertiary, onValueChange = { tertiary = it }, label = { Text("Tertiary (optional)") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = tagsText, onValueChange = { tagsText = it }, label = { Text("Tags (comma separated)") })
            }
        }
    )
}
