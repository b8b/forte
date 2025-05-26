import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString

actual fun readTests(fileName: String): String {
    return SystemFileSystem
        .source(Path("kotlin/$fileName"))
        .buffered()
        .use { it.readString() }
}
