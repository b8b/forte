import kotlin.io.path.readText
import kotlin.io.path.toPath

actual fun readTests(fileName: String): String {
    return TestEval::class.java.getResource(fileName)!!
        .toURI().toPath().readText()
}
