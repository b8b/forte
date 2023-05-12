import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import okio.Path.Companion.toPath
import org.cikit.forte.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

expect fun readTests(fileName: String): String
expect fun createRuntime(): Runtime

class TestTranspiler {
    private fun runTest(
        testName: String,
        testSource: String,
        declarations: List<Declarations> = defaultDeclarations,
        customizeRuntime: (Runtime) -> Unit
    ) {
        println("--- $testName ---")
        val rt = createRuntime()
        rt.setCoreExtensions()
        customizeRuntime(rt)
        val expect = testSource.substringBefore("\n~\n")
        val templateSource = testSource.substringAfter("\n~\n")
        val tokenizer = Tokenizer(templateSource, testName.toPath())
        val parser = TemplateParser(tokenizer, declarations)
        val parsedTemplate = parser.parseTemplate()
        println(parsedTemplate.transpileToJs())
        val compiled = rt.compile(parsedTemplate)
        val target = StringBuilder()
        rt.startCapture { target.append(it.toString()) }
        rt.exec(compiled)
        assertEquals(expect, target.toString())
    }

    private fun runTests(
        fileName: String,
        declarations: List<Declarations> = defaultDeclarations,
        customizeRuntime: (Runtime) -> Unit = {}
    ) {
        val testsMd = readTests(fileName)
        val lines = testsMd.split("\n").iterator()
        while (lines.hasNext()) {
            val line = lines.next()
            if (!line.startsWith("##")) continue
            val testName = line.removePrefix("##").trim()
            val testSource = StringBuilder()
            while (lines.hasNext()) {
                val line2 = lines.next()
                if (!line2.startsWith("```")) continue
                while (lines.hasNext()) {
                    val line3 = lines.next()
                    if (line3.startsWith("```")) break
                    testSource.appendLine(line3)
                }
                break
            }
            runTest(
                testName,
                testSource.toString().trim(),
                declarations,
                customizeRuntime
            )
        }
    }

    private fun JsonElement.toAny(): Any? = when (this) {
        is JsonNull -> null
        is JsonPrimitive -> booleanOrNull
            ?: longOrNull ?: intOrNull
            ?: doubleOrNull ?: floatOrNull
            ?: contentOrNull

        is JsonArray -> map { it.toAny() }
        is JsonObject -> map { (k, v) -> k to v.toAny() }.toMap()
    }

    @Test
    fun testBasic() = runTests("basic.md")

    @Test
    fun testTrim() = runTests("trim.md")

    @Test
    fun testCustomControl() = runTests(
        fileName = "custom_tags.md",
        declarations = defaultDeclarations + listOf(
            Declarations.Command(
                "load_json",
                endAliases = setOf("endload"),
                parser = {
                    if (name == "load_json") {
                        val kwAs = parsePrimary()
                        require(kwAs is Node.Variable && kwAs.name == "as") {
                            "expected 'as', found: $kwAs"
                        }
                        val varName = parsePrimary()
                        require(varName is Node.Variable) {
                            "expected variable name, found: $varName"
                        }
                        args["varName"] = Node.StringLiteral(
                            varName.first,
                            varName.first,
                            varName.name
                        )
                    }
                }
            ),
            Declarations.Command(
                "debug",
                parser = { args["value"] = parseExpression() }
            )
        ),
        customizeRuntime = { rt ->
            rt.setFunction("control_load_json") { args ->
                val (branches) = rt.readArgs(args, "branches")
                // expect single branch
                val branch = (branches as List<*>).single()
                branch as Runtime.Command

                val target = mutableListOf<Any?>()
                rt.startCapture { v -> target += v }
                try {
                    branch.body.invoke()
                } finally {
                    rt.endCapture()
                }

                val jsonSource = target.joinToString("") { it.toString() }
                val result = Json.decodeFromString<JsonElement>(jsonSource)
                val (varName) = rt.readArgs(branch.args(), "varName")
                rt.setVar(varName as String, result.toAny())
            }
            rt.setFunction("cmd_debug") { args ->
                val (value) = rt.readArgs(args, "value")
                println("debug: $value")
            }
        }
    )

    @Test
    fun testFail() {
        val template = parseTemplate("{% fail %}")
        val rt = createRuntime()
        rt.setCoreExtensions()
        val compiled = rt.compile(template)
        println(compiled.textRepresentation)
        assertFails {
            rt.exec(compiled)
        }
    }
}
