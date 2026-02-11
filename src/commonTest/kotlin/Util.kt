import kotlinx.coroutines.test.runTest
import org.cikit.forte.Forte
import org.cikit.forte.core.UPath
import org.cikit.forte.parser.Node
import org.cikit.forte.parser.sourceTokenRange

expect fun readTests(fileName: String): String

fun Forte.runTests(fileName: String) = runTest {
    val source = readTests(fileName)
    val template = parseTemplate(source, UPath(fileName))
    val ctx = scope().renderToString()
    var failed = 0
    for (node in template.nodes) {
        if (node is Node.Text) {
            node.eval(ctx, template)
        } else {
            val tokenRange = node.sourceTokenRange()
            val nodeSource = source.substring(
                tokenRange.first.first .. tokenRange.second.last
            )
            ctx.emitValue(nodeSource)
            try {
                ctx.emitValue(" ")
                ctx.evalNodes(template, listOf(node))
                ctx.emitValue(" \u2705")
            } catch (ex: Exception) {
                ctx.emitValue(" \u274C $ex")
                failed++
            }
        }
    }
    val result = ctx.result.split("\n")
    val numTests = result.count { it.startsWith("##") }
    if (failed > 0) {
        println(result.joinToString("\n"))
        error("$fileName: $failed/$numTests tests failed")
    } else {
        println("$fileName: $numTests tests OK")
    }
}
