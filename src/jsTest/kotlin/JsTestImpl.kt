import org.cikit.forte.core.UPath

private val fs = js("require('fs')")

private fun UPath.readText(charset: String = "utf8"): String {
    return fs.readFileSync(this.toString(), charset) as String
}

actual fun readTests(fileName: String): String {
    return UPath("kotlin/$fileName").readText()
}
