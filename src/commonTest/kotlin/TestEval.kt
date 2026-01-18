import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import org.cikit.forte.Forte
import org.cikit.forte.core.EvalException
import org.cikit.forte.lib.jinja.strictJinjaCompat
import org.cikit.forte.parser.Declarations
import org.cikit.forte.parser.Expression
import kotlin.test.*

class TestEval {
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
    fun testBasic() = Forte.runTests("basic.md")

    @Test
    fun testTrim() = Forte.runTests("trim.md")

    @Test
    fun testPython() = Forte.runTests("python.md") { source ->
        val newSource = buildString {
            val linesIt = source.splitToSequence("\n").iterator()
            while (linesIt.hasNext()) {
                val line = linesIt.next()
                appendLine(line)
                if (line.startsWith(">>> ")) {
                    appendLine("'{{ ${line.removePrefix(">>> ")} }}'")
                    linesIt.next()
                }
            }
        }
        println("---")
        println(newSource)
        println("---")
        "$source\n~\n$newSource"
    }

    @Test
    fun testJinjaCompat() = runTest {
        val forte = Forte
        val jinja = Forte {
            strictJinjaCompat()
        }

        val template1 = "{{ \"hello #{world}\" }}"
        assertEquals(
            "hello #{world}",
            jinja.renderToString()
                .setVar("world", "moon")
                .evalTemplate(jinja.parseTemplate(template1))
                .result
        )
        assertEquals(
            "hello moon",
            forte.renderToString()
                .setVar("world", "moon")
                .evalTemplate(forte.parseTemplate(template1))
                .result
        )

        val template2 = "{{ 2 ** 3 ** 4 }}"
        assertEquals(
            "4096",
            jinja.renderToString()
                .evalTemplate(jinja.parseTemplate(template2))
                .result
        )
        assertEquals(
            "2417851639229258349412352",
            forte.renderToString()
                .evalTemplate(forte.parseTemplate(template2))
                .result
        )
    }

    @Test
    fun testSimple() = runTest {
        val result = Forte.scope().evalExpression(
            Forte.parseExpression("1 + 1")
        )
        assertEquals(2, result)
    }

    @Test
    fun testVar() = runTest {
        val result = Forte.scope()
            .setVar("x", 2)
            .evalExpression(Forte.parseExpression("x + 1 == 3"))
        assertEquals(result, true)
    }

    @Test
    fun testCondBinOp() = runTest {
        val result = Forte.scope()
            .evalExpression(Forte.parseExpression("false && true"))
        assertFalse(result as Boolean)
    }

    @Test
    fun testTemplate() = runTest {
        val template = Forte.parseTemplate("{% set y = x + 1 %}{{ y }}")
        val result = Forte
            .captureTo { println("--> $it") }
            .setVar("x", 2)
            .evalTemplate(template)
        assertEquals(3, result.getVar("y"))
    }

    @Test
    fun testContext() = runTest {
        with (Forte.captureTo { println("--> $it") }) {
            setVar("x", 2)
            evalTemplate(Forte.parseTemplate("{% if true %}{% set y = x + 1 %}{% endif %}{{ y }}"))
            assertEquals(3, getVar("y"))
            evalTemplate(Forte.parseTemplate("{% for x in [1] %}{% set y = x + 1 %}{% endfor %}{{ y }}"))
            assertEquals(3, getVar("y"))
        }
    }

    @Test
    fun testCustomControl() {
        val forte = Forte {
            declarations += Declarations.Command(
                "load_json",
                endAliases = setOf("endload"),
                parser = {
                    if (name == "load_json") {
                        val kwAs = parsePrimary()
                        require(kwAs is Expression.Variable && kwAs.name == "as") {
                            "expected 'as', found: $kwAs"
                        }
                        val varName = parsePrimary()
                        require(varName is Expression.Variable) {
                            "expected variable name, found: $varName"
                        }
                        args["varName"] = Expression.StringLiteral(
                            varName.first,
                            varName.first,
                            varName.name
                        )
                    }
                }
            )
            context.defineControlTag("load_json") { ctx, template, branches ->
                // expect single branch
                val branch = branches.single()
                val jsonSource = ctx.scope()
                    .renderToString()
                    .evalNodes(template, branch.body)
                    .result
                val result = Json.decodeFromString<JsonElement>(jsonSource)
                val varName = ctx.evalExpression(
                    branch.args.getValue("varName")
                )
                ctx.setVar(varName as String, result.toAny())
            }
            declarations += Declarations.Command(
                "debug",
                parser = { args["value"] = parseExpression() }
            )
            context.defineCommandTag("debug") { ctx, _, args ->
                val value = ctx.evalExpression(args.getValue("value"))
                println("debug: $value")
            }
        }
        forte.runTests("custom_tags.md")
    }

    @Test
    fun testFail() = runTest {
        assertFails {
            Forte.scope().evalTemplate(Forte.parseTemplate("{% fail %}"))
        }
    }

    @Test
    fun testNamedArg() = runTest {
        val result = Forte.renderToString().evalTemplate(
            Forte.parseTemplate("{{ range(start=1, end_inclusive=2)|json }}")
        ).result
        assertEquals("[1,2]", result)
    }

    @Test
    fun testControl() = runTest {
        val result = Forte.renderToString().evalTemplate(
            Forte.parseTemplate("{% if true %}true{% endif %}")
        ).result
        assertEquals("true", result)
    }

    @Test
    fun testFailOnUndefined() = runTest {
        val vars = mapOf("x" to mapOf("a" to 1, "b" to 2))
        assertEquals(
            1,
            Forte.scope()
                .setVars(vars)
                .evalExpression(Forte.parseExpression("x.a"))
        )
        assertFailsWith<EvalException> {
            try {
                Forte.scope()
                    .setVars(vars)
                    .evalExpression(Forte.parseExpression("yes.a.b.c"))
            } catch (ex: EvalException) {
                println(ex)
                assertEquals(0, ex.startToken.first)
                assertContains(ex.errorMessage, "undefined variable: 'yes'")
                throw ex
            }
        }
        assertFailsWith<EvalException> {
            try {
                Forte.scope()
                    .setVars(vars)
                    .evalExpression(Forte.parseExpression("x.z.b.c"))
            } catch (ex: EvalException) {
                println(ex)
                assertEquals(1, ex.startToken.first)
                assertContains(ex.errorMessage, "'z'")
                throw ex
            }
        }
        assertFailsWith<EvalException> {
            try {
                Forte.scope()
                    .setVars(vars)
                    .evalExpression(Forte.parseExpression("x.cat|default(flash)"))
            } catch (ex: EvalException) {
                // the expression "a.cat" is rescued
                // so expect the expression flash to fail
                println(ex)
                assertEquals(14, ex.startToken.first)
                assertContains(ex.errorMessage, "undefined variable: 'flash'")
                throw ex
            }
        }
    }

    @Test
    fun testDefined() = runTest {
        val template = Forte.parseTemplate(
            "{% if (grains[\"zpool\"][\"data\"] is defined and grains[\"zpool\"][\"data\"]|string|matches_glob(\"*\")) %}BAD{% endif %}"
        )
        val result = Forte.renderToString()
            .setVars("grains" to mapOf("zpool" to emptyMap<String, Any?>()))
            .evalTemplate(template)
            .result
        assertEquals("", result)
    }

    @Test
    fun testInvoke() = runTest {
        val result = Forte.scope()
            .setVars("a" to mapOf("x" to 1))
            .evalExpression(Forte.parseExpression("a.keys()"))
        assertEquals(listOf("x"), (result as Iterable<*>).toList())
        val result2 = Forte {
            context.defineMethod("invoke") { subject, args ->
                args.requireEmpty()
                subject
            }
        }.scope().evalExpression(Forte.parseExpression("'test'()"))
        assertEquals("test", result2)
    }

    @Test
    fun testIfExpression() = runTest {
        assertEquals(
            1,
            Forte.scope()
                .evalExpression(Forte.parseExpression("1 if true else 2"))
        )
        assertFailsWith<EvalException> {
            Forte.renderToString()
                .evalTemplate(Forte.parseTemplate("{{ 3 if false }}"))
                .result
        }
        assertEquals(
            4,
            Forte.scope()
                .evalExpression(Forte.parseExpression("3 if false else 4"))
        )
    }
}
