import org.cikit.forte.Forte
import org.cikit.forte.core.toUPath
import org.cikit.forte.parser.Expression
import org.cikit.forte.parser.Node
import kotlin.io.path.Path
import kotlin.io.path.readText

class GenerateSourcesTest {

    //@Test
    fun testGenerateMapSources() {
        val source = Path("src/forte/map.twig")
        val template = Forte.parseTemplate(source.readText(), source.toUPath())
        val target = StringBuilder()
        target.appendLine("import org.cikit.forte.parser.*")
        target.append("val x = listOf(")
        target.append(
            template.nodes.mapNotNull { node ->
                val sb = StringBuilder()
                node.generateSource(sb)
                sb.toString().takeIf { it.isNotBlank() }
            }.joinToString(", ")
        )
        target.appendLine(")")
        println(target.toString())
    }

    fun Node.generateSource(target: StringBuilder) {
        when (val node = this) {
            is Node.Command -> {
                node.generateSource(target)
            }
            is Node.Control -> {
                node.generateSource(target)
            }
            is Node.Emit -> {
                node.generateSource(target)
            }
            is Node.Text,
            is Node.Comment -> { /* ignored */ }
        }
    }

    fun Node.Command.generateSource(target: StringBuilder) {
        target.append("Node.Command(first = Token.Text(0..0), name = \"")
        target.append(name)
        target.append("\", content = mapOf(")
        for ((k, v) in args) {
            target.append("\"")
            target.append(k)
            target.append("\" to ")
            v.generateSource(target)
            target.append(", ")
        }
        target.append("), branchAliases = setOf(")
        for (s in branchAliases) {
            target.append("\"")
            target.append(s)
            target.append("\", ")
        }
        target.append("), endAliases = setOf(")
        for (s in endAliases) {
            target.append("\"")
            target.append(s)
            target.append("\", ")
        }
        target.append("), last = Token.Text(0..0), allowHidden = ")
        target.append(allowHidden)
        target.append(")")
    }

    fun Node.Control.generateSource(target: StringBuilder) {
        target.append("Node.Control(first = ")
        first.generateSource(target)
        target.append(", branches = listOf(")
        //TODO
        target.append("), allowHidden = ")
        target.append(allowHidden)
        target.append(")")
    }

    fun Node.Branch.generateSource(target: StringBuilder) {
        target.append("Node.Branch(first = ")
        first.generateSource(target)
        target.append(", body = listOf(")
        target.append(
            body.mapNotNull { node ->
                val sb = StringBuilder()
                node.generateSource(sb)
                sb.toString().takeIf { it.isNotBlank() }
            }.joinToString(", ")
        )
        target.append("), last = ")
        last.generateSource(target)
        target.append(")")
    }

    fun Node.Emit.generateSource(target: StringBuilder) {
        target.append("Node.Emit(first = Token.Text(0..0), content = ")
        content.generateSource(target)
        target.append(", last = Token.Text(0..0)")
        target.append(")")
    }

    fun Expression.generateSource(target: StringBuilder) {
        when (val expression = this) {
            is Expression.SubExpression -> TODO()
            is Expression.Malformed -> TODO()
            is Expression.Variable -> TODO()
            is Expression.NullLiteral -> TODO()
            is Expression.BooleanLiteral -> TODO()
            is Expression.IntegerLiteral -> TODO()
            is Expression.FloatLiteral -> TODO()
            is Expression.StringLiteral -> TODO()
            is Expression.ByteStringLiteral -> TODO()
            is Expression.StringInterpolation -> TODO()
            is Expression.ArrayLiteral -> TODO()
            is Expression.ObjectLiteral -> TODO()
            is Expression.Access -> TODO()
            is Expression.CompAccess -> TODO()
            is Expression.SliceAccess -> TODO()
            is Expression.FunctionCall -> TODO()
            is Expression.UnOp -> TODO()
            is Expression.BinOp -> TODO()
            is Expression.TransformOp -> TODO()
            is Expression.InvokeOp -> TODO()
        }
    }

    fun Expression.Variable.generateSource(target: StringBuilder) {
        TODO()
    }

    fun Expression.BooleanLiteral.generateSource(target: StringBuilder) {
        TODO()
    }
}
