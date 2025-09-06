package ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.training.model.ExerciseDef
import data.ExerciseLibraryStore
import data.PlanStore
import platform.Settings
import platform.openFileDialog
import platform.saveFileDialog
import state.AppState
import state.Selection
import ui.dialogs.ExerciseDialog
import ui.dialogs.PlanWizardDialog
import java.awt.Frame
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell() {
    val app = remember { AppState() }
    var showPlanWizard by remember { mutableStateOf(false) }
    var showExerciseDialog by remember { mutableStateOf(false) }
    var planMenuOpen by remember { mutableStateOf(false) }
    var showCopyWeekDialog by remember { mutableStateOf(false) }
    var copyDialogDefaultFrom by remember { mutableStateOf<Int?>(null) }
    var libMenuOpen by remember { mutableStateOf(false) }
    var pendingAddExercise: ExerciseDef? by remember { mutableStateOf(null) }


    // Autoload library & plan on first launch
    LaunchedEffect(Unit) {
        // Library
        val libFile = Settings.lastLibraryFile() ?: Settings.defaultLibraryFile().also {
            it.parentFile.mkdirs()
            if (!it.exists()) ExerciseLibraryStore.save(emptyList(), it)
            Settings.setLastLibrary(it)
        }
        runCatching { ExerciseLibraryStore.loadFromFile(libFile) }
            .onSuccess { app.library = it; app.currentLibraryFile = libFile }
            .onFailure { it.printStackTrace() }

        // Plan
        Settings.lastPlanFile()?.let { f ->
            runCatching { PlanStore.loadFromFile(f) }
                .onSuccess { app.plan = it; app.currentPlanFile = f }
                .onFailure { it.printStackTrace() }
        } ?: run {
            // start with no plan; user can create one
            app.plan = null
        }
    }

    // Auto-save library
    LaunchedEffect(app.library, app.currentLibraryFile) {
        val f = app.currentLibraryFile ?: return@LaunchedEffect
        runCatching { ExerciseLibraryStore.save(app.library, f) }.onFailure { it.printStackTrace() }
    }
    // Auto-save plan
    LaunchedEffect(app.plan, app.currentPlanFile) {
        val f = app.currentPlanFile
        val p = app.plan
        if (f != null && p != null) runCatching { PlanStore.save(p, f) }.onFailure { it.printStackTrace() }
    }

    MaterialTheme {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(app.plan?.plan?.name ?: "Training Plan Authoring") },
                    navigationIcon = {
                        // Library menu
                        Box {
                            TextButton(onClick = { libMenuOpen = true }) { Text("Library") }
                            DropdownMenu(expanded = libMenuOpen, onDismissRequest = { libMenuOpen = false }) {
                                DropdownMenuItem(text = { Text("Open Library…") }, onClick = {
                                    openFileDialog(Frame(), "Open Exercise Library", listOf("yml","yaml","json"))?.let { f ->
                                        runCatching { ExerciseLibraryStore.loadFromFile(f) }
                                            .onSuccess { app.library = it; app.currentLibraryFile = f; Settings.setLastLibrary(f) }
                                            .onFailure { it.printStackTrace() }
                                    }
                                    libMenuOpen = false
                                })
                                DropdownMenuItem(text = { Text("Save Library As…") }, onClick = {
                                    val current = app.currentLibraryFile ?: Settings.defaultLibraryFile()
                                    saveFileDialog(Frame(), "Save Library As", current.name)?.let { f ->
                                        ExerciseLibraryStore.save(app.library, f)
                                        app.currentLibraryFile = f
                                        Settings.setLastLibrary(f)
                                    }
                                    libMenuOpen = false
                                })
                                DropdownMenuItem(text = { Text("New Exercise…") }, onClick = {
                                    showExerciseDialog = true; libMenuOpen = false
                                })
                            }
                        }
                    },
                    actions = {
                        // Plan menu
                        Box {
                            TextButton(onClick = { planMenuOpen = true }) { Text("Plan") }
                            DropdownMenu(expanded = planMenuOpen, onDismissRequest = { planMenuOpen = false }) {
                                DropdownMenuItem(text = { Text("Copy selected week…") }, onClick = {
                                    copyDialogDefaultFrom = (app.selection as? state.Selection.Week)?.index?.plus(1)
                                    showCopyWeekDialog = true; planMenuOpen = false
                                })
                                DropdownMenuItem(text = { Text("Copy Week 1 → All weeks") }, onClick = {
                                    app.copyWeek1ToAllWeeks(); planMenuOpen = false
                                })

                                DropdownMenuItem(text = { Text("New Plan…") }, onClick = {
                                    showPlanWizard = true; planMenuOpen = false
                                })
                                DropdownMenuItem(text = { Text("Open Plan…") }, onClick = {
                                    openFileDialog(Frame(), "Open Plan", listOf("yml","yaml","json"))?.let { f ->
                                        runCatching { PlanStore.loadFromFile(f) }
                                            .onSuccess { app.plan = it; app.currentPlanFile = f; Settings.setLastPlan(f) }
                                            .onFailure { it.printStackTrace() }
                                    }
                                    planMenuOpen = false
                                })
                                DropdownMenuItem(text = { Text("Save Plan As…") }, onClick = {
                                    val p = app.plan ?: return@DropdownMenuItem
                                    val defaultDir = Settings.plansDirectory()
                                    val guess = File(defaultDir, "${p.plan.id}.yml")
                                    saveFileDialog(Frame(), "Save Plan As", guess.name)?.let { f ->
                                        PlanStore.save(p, f)
                                        app.currentPlanFile = f
                                        Settings.setLastPlan(f)
                                    }
                                    planMenuOpen = false
                                })
                                if ((app.plan?.plan?.duration_weeks ?: 0) > 1) {
                                    DropdownMenuItem(text = { Text("Copy Week 1 → All") }, onClick = {
                                        app.copyWeek1ToAllWeeks(); planMenuOpen = false
                                    })
                                }
                            }
                        }
                    }
                )
            }
        ) { padding ->
            Row(Modifier.fillMaxSize().padding(padding)) {

                // LEFT: Library grouped by primary muscle
                Surface(tonalElevation = 1.dp, modifier = Modifier.width(320.dp).fillMaxHeight()) {
                    LibraryPane(
                        library = app.library,
                        onNewExercise = { showExerciseDialog = true },
                        canAddToDay = app.selection is Selection.Day,
                        onAddToDayClick = { ex -> pendingAddExercise = ex },
                    )
                }

                // CENTER: Plan tree
                Surface(tonalElevation = 2.dp, modifier = Modifier.weight(1f).fillMaxHeight().padding(8.dp)) {
                    PlanTree(app)
                }

                // RIGHT: Inspector (unchanged for now)
                Surface(tonalElevation = 1.dp, modifier = Modifier.width(360.dp).fillMaxHeight().padding(8.dp)) {
                    Inspector(app)
                }
            }
        }

        if (showPlanWizard) {
            PlanWizardDialog(
                onDismiss = { showPlanWizard = false },
                onCreate = { name, weeks, days ->
                    app.newEmptyPlan(name, weeks, days)
                    val file = File(Settings.plansDirectory(), "${name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')}.yml")
                    app.currentPlanFile = file
                    Settings.setLastPlan(file)
                    showPlanWizard = false
                }
            )
        }

        if (showExerciseDialog) {
            ExerciseDialog(
                onDismiss = { showExerciseDialog = false },
                onCreate = { def ->
                    app.addExerciseToLibrary(def) // autosave handles persistence
                    showExerciseDialog = false
                }
            )
        }

        val exToAdd = pendingAddExercise
        if (showCopyWeekDialog) {
            ui.dialog.CopyWeekDialog(
                totalWeeks = app.plan?.plan?.weeks?.size ?: 0,
                defaultFrom = copyDialogDefaultFrom,
                onCancel = { showCopyWeekDialog = false },
                onConfirm = { from, to -> app.copyWeek(from, to); showCopyWeekDialog = false }
            )
        }

        if (exToAdd != null) {
            ui.dialog.AddToDayDialog(
                exerciseName = exToAdd.name,
                onDismiss = { pendingAddExercise = null },
                onCreate = { template ->
                    app.addExerciseToSelectedDay(exToAdd.id, template)
                    pendingAddExercise = null
                }
            )
        }
    }
}

@Composable
private fun LibraryPane(
    library: List<ExerciseDef>,
    onNewExercise: () -> Unit,
    canAddToDay: Boolean,
    onAddToDayClick: (ExerciseDef) -> Unit
) {
    val grouped = remember(library) { library.groupBy { it.muscles.primary.ifBlank { "Other" } }.toSortedMap() }
    Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Exercise Library", style = MaterialTheme.typography.titleMedium)
            Button(onClick = onNewExercise) { Text("+ Exercise") }
        }
        Divider()
        if (library.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No exercises yet.") }
            return@Column
        }
        LazyColumn(Modifier.fillMaxSize()) {
            grouped.forEach { (muscle, list) ->
                item { Text(muscle, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(vertical = 6.dp)) }
                items(list) { ex ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(ex.name, style = MaterialTheme.typography.bodyMedium)
                            val subs = listOfNotNull(ex.muscles.secondary, ex.muscles.tertiary).joinToString(" • ")
                            if (subs.isNotBlank()) Text(subs, style = MaterialTheme.typography.bodySmall)
                        }
                        Button(
                            enabled = canAddToDay,
                            onClick = { onAddToDayClick(ex) }
                        ) { Text("Add to Day") }
                    }
                    Divider()
                }
            }
        }
    }
}
