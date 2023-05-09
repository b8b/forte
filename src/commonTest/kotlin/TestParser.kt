import org.cikit.forte.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestParser {
    @Test
    fun testInterpolation() {
        val tokenBuffer = mutableListOf<Token>()
        val tokenizer = Tokenizer(
            """{{ "test #{1} huhu" }}""",
            tokenInspector = tokenBuffer::plusAssign
        )
        val result = parseTemplate(tokenizer)
        for (t in tokenBuffer) {
            println(t)
        }
        val emit = result.nodes.drop(1).first() as Node.Emit
        println(emit.content)
    }

    @Test
    fun testCommand() {
        val result = parseTemplate(
            "{% if 'x' in user.name or 'y' in user.name %}\n{% endif %}\n"
        )
        println(result)
    }

    @Test
    fun testControl() {
        val result = parseTemplate("""{% if true %}hello{% endif %}""")
        println(result)
    }

    @Test
    fun testCustomControl() {
        val result = parseTemplate(
            """{% load_yaml 'blub' %}hello{% endload %}""",
            declarations = defaultDeclarations + listOf(
                Declarations.Command("load_yaml",
                    endAliases = setOf("endload")
                ),
            )
        )
        println(result)
    }

    @Test
    fun testArrayLiteral() {
        val resultIt = parseTemplate("{{ [1, 2, 3].first() }}").nodes.iterator()
        assertTrue(resultIt.next() is Node.Text)
        val nodeEmit = resultIt.next()
        assertTrue(nodeEmit is Node.Emit)
        val extensionCall = nodeEmit.content
        assertTrue(extensionCall is Node.ExtensionCall)
        val arrayLit =  extensionCall.left
        assertTrue(arrayLit is Node.ArrayLiteral)
        val result = arrayLit.children.map { child ->
            (child as Node.NumericLiteral).value as Int
        }
        assertEquals(listOf(1, 2, 3), result)
    }

    @Test
    fun testPrecedence1() {
        val resultIt = parseTemplate("{{ 1 + 2 * 3 }}").nodes.iterator()
        assertTrue(resultIt.next() is Node.Text)
        val nodeEmit = resultIt.next()
        assertTrue(nodeEmit is Node.Emit)
        println(nodeEmit.content)
        (nodeEmit.content as Node.BinOp).let { plusOperation ->
            assertEquals("plus", plusOperation.decl.name)
            assertEquals(1, (plusOperation.left as Node.NumericLiteral).value)
            plusOperation.right as Node.BinOp
        }.let { mulOperation ->
            assertEquals("mul", mulOperation.decl.name)
            assertEquals(2, (mulOperation.left as Node.NumericLiteral).value)
            assertEquals(3, (mulOperation.right as Node.NumericLiteral).value)
        }
    }

    @Test
    fun testPrecedence2() {
        val resultIt = parseTemplate("{{ 2 * 3 + 1 }}").nodes.iterator()
        assertTrue(resultIt.next() is Node.Text)
        val nodeEmit = resultIt.next()
        assertTrue(nodeEmit is Node.Emit)
        println(nodeEmit.content)
        (nodeEmit.content as Node.BinOp).let { plusOperation ->
            assertEquals("plus", plusOperation.decl.name)
            assertEquals(1, (plusOperation.right as Node.NumericLiteral).value)
            plusOperation.left as Node.BinOp
        }.let { mulOperation ->
            assertEquals("mul", mulOperation.decl.name)
            assertEquals(2, (mulOperation.left as Node.NumericLiteral).value)
            assertEquals(3, (mulOperation.right as Node.NumericLiteral).value)
        }
    }

    @Test
    fun testPipe() {
        val resultIt = parseTemplate("{{ {k: 1}|dictsort()|first }}").nodes.iterator()
        assertTrue(resultIt.next() is Node.Text)
        val nodeEmit = resultIt.next()
        assertTrue(nodeEmit is Node.Emit)
        println(nodeEmit.content)
        (nodeEmit.content as Node.BinOp).let { op ->
            assertEquals("pipe", op.decl.name)
            assertEquals("first", (op.right as Node.Variable).name)
            op.left as Node.BinOp
        }.let { op ->
            assertEquals("pipe", op.decl.name)
            assertEquals("dictsort", (op.right as Node.FunctionCall).name)
            op.left as Node.ObjectLiteral
        }.let { o ->
            assertEquals("k", (o.pairs.single().first as Node.Variable).name)
        }
    }

    @Test
    fun testNamedArg() {
        val resultIt = parseTemplate("{{ {k: 1}|dictsort(reverse=true, numeric=true) }}").nodes.iterator()
        assertTrue(resultIt.next() is Node.Text)
        val nodeEmit = resultIt.next()
        assertTrue(nodeEmit is Node.Emit)
        println(nodeEmit.content)
        (nodeEmit.content as Node.BinOp).let { op ->
            assertEquals("pipe", op.decl.name)
            assertEquals("dictsort", (op.right as Node.FunctionCall).name)
            assertTrue((op.right as Node.FunctionCall).children.all {
                it is Node.BinOp && it.decl.name == "assign"
            })
            op.left as Node.ObjectLiteral
        }
    }
}