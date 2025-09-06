package platform

import java.awt.FileDialog
import java.awt.Frame
import java.io.File

fun openFileDialog(parent: Frame? = null, title: String, extensions: List<String>): File? {
    val fd = FileDialog(parent, title, FileDialog.LOAD)
    fd.isVisible = true
    val file = if (fd.file != null && fd.directory != null) File(fd.directory, fd.file) else null
    if (file != null && extensions.any { file.name.endsWith(".$it", ignoreCase = true) }) return file
    return file
}

fun saveFileDialog(parent: Frame? = null, title: String, suggestedName: String): File? {
    val fd = FileDialog(parent, title, FileDialog.SAVE)
    fd.file = suggestedName
    fd.isVisible = true
    return if (fd.file != null && fd.directory != null) File(fd.directory, fd.file) else null
}
