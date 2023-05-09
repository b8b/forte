import org.cikit.forte.Runtime
import org.cikit.forte.RhinoRuntime
import kotlin.io.path.readText
import kotlin.io.path.toPath

actual fun readTests(fileName: String): String {
    return TestTranspiler::class.java.getResource(fileName)!!
        .toURI().toPath().readText()
}

actual fun createRuntime(): Runtime {
    return RhinoRuntime()
}