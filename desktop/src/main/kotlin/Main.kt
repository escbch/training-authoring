// desktop/src/main/kotlin/Main.kt
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ui.RootApp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Training Plan Authoring"
    ) {
        RootApp()
    }
}
