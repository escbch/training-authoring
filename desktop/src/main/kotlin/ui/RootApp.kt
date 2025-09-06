
package ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import data.PlanIndex
import platform.Settings
import java.io.File

sealed interface Screen {
    data object Overview: Screen
    data class Editor(val planFile: File): Screen
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootApp() {
    var screen by remember { mutableStateOf<Screen>(Screen.Overview) }

    when (val sc = screen) {
        Screen.Overview -> {
            OverviewScreen(
                onCreatePlan = { params ->
                    val f = PlanIndex.createNewPlan(params.name, params.durationWeeks, params.daysPerWeek, params.e1rmModel)
                    screen = Screen.Editor(f)
                },
                onEditPlan = { f ->
                    Settings.setLastPlan(f)
                    screen = Screen.Editor(f)
                },
                onCopyPlan = { f ->
                    val nf = PlanIndex.copyPlan(f)
                    screen = Screen.Editor(nf)
                }
            )
        }
        is Screen.Editor -> {
            Scaffold(
                topBar = {
                    SmallTopAppBar(
                        title = { Text("Editing: " + PlanIndex.readPlanTitle(sc.planFile)) },
                        navigationIcon = {
                            TextButton(onClick = { screen = Screen.Overview }) { Text("← Back") }
                        }
                    )
                }
            ) { padding ->
                AppShell() // existing editor uses Settings.lastPlanFile()
            }
        }
    }
}
