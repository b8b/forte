import kotlinx.serialization.json.*
import org.cikit.forte.Forte
import org.cikit.forte.core.EvalException
import org.cikit.forte.core.evalExpression
import org.cikit.forte.core.evalTemplate
import org.cikit.forte.parser.Declarations
import org.cikit.forte.parser.Expression
import kotlin.test.*

class TestEvaluator {

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
    fun testSimple() {
        val result = Forte.evalExpression("1 + 1")
        assertEquals(2, result)
    }

    @Test
    fun testVar() {
        val result = Forte.evalExpression("x + 1 == 3", "x" to 2)
        assertTrue(result == true)
    }

    @Test
    fun testTemplate() {
        val template = Forte.parseTemplate("{% set y = x + 1 %}{{ y }}")
        val result = Forte
            .capture { println("--> $it") }
            .setVar("x", 2)
            .evalTemplate(template)
        assertEquals(3, result.getVar("y"))
    }

    @Test
    fun testContext() {
        with (Forte.scope { println("--> $it") }) {
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
            context.defineControl("load_json") { ctx, branches ->
                // expect single branch
                val branch = branches.single()
                val jsonSource = ctx.scope()
                    .captureToString()
                    .evalTemplate(branch.body)
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
            context.defineCommand("debug") { ctx, args ->
                val value = ctx.evalExpression(args.getValue("value"))
                println("debug: $value")
            }
        }
        forte.runTests("custom_tags.md")
    }

    @Test
    fun testFail() {
        assertFails {
            Forte.evalTemplate("{% fail %}")
        }
    }

    @Test
    fun testNamedArg() {
        val result = Forte.evalTemplateToString(
            "{{ range(start=1, end_inclusive=2)|json }}"
        )
        assertEquals("[1,2]", result)
    }

    @Test
    fun testControl() {
        val result = Forte.evalTemplateToString(
            "{% if true %}true{% endif %}"
        )
        assertEquals("true", result)
    }

    @Test
    fun testFailOnUndefined() {
        val vars = mapOf("x" to mapOf("a" to 1, "b" to 2))
        assertEquals(
            1,
            Forte.evalExpression("x.a", vars)
        )
        assertFailsWith<EvalException> {
            try {
                Forte.evalExpression("yes.a.b.c", vars)
            } catch (ex: EvalException) {
                println(ex.detailedMessage)
                assertEquals(0, ex.startToken.first)
                assertContains(ex.message!!, "undefined variable: y")
                throw ex
            }
        }
        assertFailsWith<EvalException> {
            try {
                Forte.evalExpression("x.z.b.c", vars)
            } catch (ex: EvalException) {
                println(ex.detailedMessage)
                assertEquals(1, ex.startToken.first)
                assertContains(ex.message!!, "'z'")
                throw ex
            }
        }
    }

    @Test
    fun testDefined() {
        val result = Forte.evalTemplateToString(
            "{% if (grains[\"zpool\"][\"data\"] is defined and grains[\"zpool\"][\"data\"]|string|matches_glob(\"*\")) %}BAD{% endif %}",
            "grains" to mapOf("zpool" to emptyMap<String, Any?>())
        )
        assertEquals("", result)
    }

    @Test
    fun testInvoke() {
        val result = Forte.evalExpression(
            "a.keys()",
            "a" to mapOf("x" to 1)
        )
        assertEquals(listOf("x"), result)
        val result2 = Forte {
            context.defineMethod("invoke") { _, subject, args ->
                args.requireEmpty()
                subject
            }
        }.evalExpression("'test'()")
        assertEquals("test", result2)
    }
}
