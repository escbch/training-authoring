// desktop/src/main/kotlin/Main.kt
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ui.AppShell

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Training Plan Authoring"
    ) {
        AppShell()   // your Scaffold/Material3 UI lives INSIDE the Window
    }
}
