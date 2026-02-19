import org.cikit.forte.*
import org.cikit.forte.parser.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class TestParser {
    @Test
    fun testInterpolation() {
        val tokenBuffer = mutableListOf<Token>()
        val tokenizer = Tokenizer(
            """{{ "test #{1} huhu" }}""",
            tokenInspector = tokenBuffer::plusAssign
        )
        val result = Forte.parseTemplate(tokenizer)
        for (t in tokenBuffer) {
            println(t)
        }
        val emit = result.nodes.drop(1).first() as Node.Emit
        println(emit.content)
    }

    @Test
    fun testCommand() {
        val result = Forte.parseTemplate(
            "{% if 'x' in user.name or 'y' in user.name %}\n{% endif %}\n"
        )
        println(result)
    }

    @Test
    fun testControl() {
        val result = Forte.parseTemplate("""{% if true %}hello{% endif %}""")
        println(result)
    }

    @Test
    fun testCustomControl() {
        val forte = Forte {
            declarations += Declarations.Command("load_yaml",
                endAliases = setOf("endload")
            )
        }
        val result = forte.parseTemplate(
            """{% load_yaml 'blub' %}hello{% endload %}"""
        )
        println(result)
    }

    @Test
    fun testArrayLiteral() {
        val resultIt = Forte.parseTemplate("{{ [1, 2, 3].first() }}").nodes.iterator()
        assertTrue(resultIt.next() is Node.Text)
        val nodeEmit = resultIt.next()
        assertTrue(nodeEmit is Node.Emit)
        val methodCall = nodeEmit.content
        assertTrue(methodCall is Expression.InvokeOp)
        val methodAccess = methodCall.left
        assertTrue(methodAccess is Expression.Access)
        assertEquals("first", methodAccess.name)
        val arrayList =  methodAccess.left
        assertTrue(arrayList is Expression.ArrayLiteral)
        val result = arrayList.children.map { child ->
            (child as Expression.NumericLiteral).value as Int
        }
        assertEquals(listOf(1, 2, 3), result)
    }

    @Test
    fun testPrecedence1() {
        val resultIt = Forte.parseTemplate("{{ 1 + 2 * 3 }}").nodes.iterator()
        assertTrue(resultIt.next() is Node.Text)
        val nodeEmit = resultIt.next()
        assertTrue(nodeEmit is Node.Emit)
        println(nodeEmit.content)
        (nodeEmit.content as Expression.BinOp).let { plusOperation ->
            assertEquals("plus", plusOperation.decl.name)
            assertEquals(1, (plusOperation.left as Expression.NumericLiteral).value)
            plusOperation.right as Expression.BinOp
        }.let { mulOperation ->
            assertEquals("mul", mulOperation.decl.name)
            assertEquals(2, (mulOperation.left as Expression.NumericLiteral).value)
            assertEquals(3, (mulOperation.right as Expression.NumericLiteral).value)
        }
    }

    @Test
    fun testPrecedence2() {
        val resultIt = Forte.parseTemplate("{{ 2 * 3 + 1 }}").nodes.iterator()
        assertTrue(resultIt.next() is Node.Text)
        val nodeEmit = resultIt.next()
        assertTrue(nodeEmit is Node.Emit)
        println(nodeEmit.content)
        (nodeEmit.content as Expression.BinOp).let { plusOperation ->
            assertEquals("plus", plusOperation.decl.name)
            assertEquals(1, (plusOperation.right as Expression.NumericLiteral).value)
            plusOperation.left as Expression.BinOp
        }.let { mulOperation ->
            assertEquals("mul", mulOperation.decl.name)
            assertEquals(2, (mulOperation.left as Expression.NumericLiteral).value)
            assertEquals(3, (mulOperation.right as Expression.NumericLiteral).value)
        }
    }

    @Test
    fun testPipe() {
        val resultIt = Forte.parseTemplate("{{ {k: 1}|dictsort()|first }}").nodes.iterator()
        assertTrue(resultIt.next() is Node.Text)
        val nodeEmit = resultIt.next()
        assertTrue(nodeEmit is Node.Emit)
        println(nodeEmit.content)
        (nodeEmit.content as Expression.TransformOp).let { op ->
            assertEquals("pipe", op.decl.name)
            assertEquals("first", (op.name))
            op.left as Expression.TransformOp
        }.let { op ->
            assertEquals("pipe", op.decl.name)
            assertEquals("dictsort", op.name)
            op.left as Expression.ObjectLiteral
        }.let { o ->
            assertEquals("k", (o.pairs.single().first as Expression.Variable).name)
        }
    }

    @Test
    fun testIs() {
        val resultIt = Forte.parseTemplate("{{ 1 + x is not y + 1 }}").nodes.iterator()
        assertTrue(resultIt.next() is Node.Text)
        val nodeEmit = resultIt.next()
        assertTrue(nodeEmit is Node.Emit)
        println(nodeEmit.content)
        (nodeEmit.content as Expression.BinOp).let { op ->
            assertEquals("plus", op.decl.name)
            op.left as Expression.TransformOp
        }.let { op ->
            assertEquals("is_not", op.decl.name)
            assertEquals("y", op.name)
            assertEquals(0, op.args.values.size)
        }
    }

    @Test
    fun testNamedArg() {
        val resultIt = Forte.parseTemplate("{{ {k: 1}|dictsort(reverse=true, numeric=true) }}").nodes.iterator()
        assertTrue(resultIt.next() is Node.Text)
        val nodeEmit = resultIt.next()
        assertTrue(nodeEmit is Node.Emit)
        println(nodeEmit.content)
        (nodeEmit.content as Expression.TransformOp).let { op ->
            assertEquals("pipe", op.decl.name)
            assertEquals("dictsort", op.name)
            assertTrue((op.args).values.all { v ->
                v is Expression.BooleanLiteral && v.value
            })
            op.left as Expression.ObjectLiteral
        }
    }

    @Test
    fun testError() {
        assertFails {
            Forte.parseTemplate("xxx\n{% fu ")
        }
    }

    @Test
    fun testCommandWithTrailingGarbage() {
        assertFails {
            Forte.parseTemplate("{% macro f(x) for ever %}fail{% endmacro %}")
        }
    }

    @Test
    fun testParseInt() {
        assertEquals(-2147483648, "-2147483648".toIntOrNull())
        assertEquals(null, "-2147483649".toIntOrNull())
        assertEquals(2147483647, "2147483647".toIntOrNull())
        assertEquals(null, "2147483648".toIntOrNull())
    }

    @Test
    fun testParseBigInt() {
        println(Forte.parseExpression("2147483648"))
    }

    @Test
    fun testChainedIfExpression() {
        val expr = Forte.parseExpression(
            "'Excellent' if cat_revenue > 50000 else 'Good' if cat_revenue > 20000 else 'Needs Work'"
        )
        println(expr)
    }
}
