import kotlinx.coroutines.await
import kotlin.js.Json
import kotlin.js.Promise
import kotlin.js.json
import kotlin.test.Test

external class Pyodide {
    fun runPythonAsync(
        code: dynamic,
        options: dynamic = definedExternally
    ): Promise<dynamic>

    suspend fun loadPackage(name: String): Promise<dynamic>

    fun toPy(arg: dynamic): dynamic
}

class PyodideRuntime {
    private var pyodide: Pyodide? = null

    suspend fun getOrLoad(): Pyodide {
        return pyodide ?: run {
            val promise = js("require('pyodide').loadPyodide({})")
            pyodide = (promise as Promise<Pyodide>).await()
            pyodide!!.loadPackage("micropip").await()
            pyodide!!.runPythonAsync("""import micropip
                |await micropip.install('Jinja2')
            """.trimMargin()).await()
            pyodide!!
        }
    }

    suspend fun runPython(code: String): dynamic {
        return getOrLoad().runPythonAsync(code).await()
    }

    suspend fun runPython(code: String, globals: Json): dynamic {
        val pyodide = getOrLoad()
        val options = json("globals" to pyodide.toPy(globals))
        return pyodide.runPythonAsync(code, options).await()
    }
}

class TestWithPython {

    companion object {
        val runtime = PyodideRuntime()
        val pySource = readTests("run_test.py")
    }

    @Test
    fun testBasic() = runPyTests("basic.md")

    @Test
    fun testTrim() = runPyTests("trim.md")

    @Test
    fun testPython() = runPyTests("python.md")

    @Test
    fun testStringConcat() = runPyTests("string_concat.md")

    @Test
    fun testNumbers() = runPyTests("numbers.md")

    private fun runPyTests(fileName: String) = runTests(fileName) { testCase ->
        runtime.runPython(
            pySource,
            globals = json(
                "template_src" to testCase,
                "template_name" to fileName
            )
        ) as String
    }

}
