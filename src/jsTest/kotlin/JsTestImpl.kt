import okio.Path
import okio.Path.Companion.toPath
import org.cikit.forte.JsRuntime
import org.cikit.forte.Runtime

private val fs = js("require('fs')")

private fun Path.readText(charset: String = "utf8"): String {
    return fs.readFileSync(this.toString(), charset) as String
}

actual fun readTests(fileName: String): String {
    return "kotlin/$fileName".toPath().readText()
}

actual fun createRuntime(): Runtime {
    return JsRuntime()
}
