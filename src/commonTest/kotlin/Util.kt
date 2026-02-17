import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.test.runTest
import org.cikit.forte.Forte
import org.cikit.forte.core.*
import org.cikit.forte.parser.ParsedTemplate
import org.cikit.forte.parser.sourceTokenRange
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

expect fun readTests(fileName: String): String

fun Forte.runTests(fileName: String) = runTest {
    val source = wrapCodeBlocks(readTests(fileName))
    val output = StringBuilder()
    val testResults = mutableListOf<Pair<Int, BlockResult>>()
    val template = parseTemplate(source, UPath(fileName))
    val ctx = scope()
        .defineControlTag("block", ControlBlock())
    ctx
        .captureTo(
            FlowCollector { value ->
                if (value is BlockResult) {
                    val offset = output.length
                    testResults += offset to value
                }
                val result = ctx.filterString(value, NamedArgs.Empty)
                if (result is Suspended) {
                    output.append(result.eval(ctx) as CharSequence)
                } else {
                    output.append((result as CharSequence))
                }
            }
        )
        .evalTemplate(template)

    var failed = 0
    var total = 0

    var currentTestOffset = -1
    var currentSrcTestOffset = -1

    val markdown = output.toString()
    output.clear()
    val flavor = CommonMarkFlavourDescriptor()
    val parsedTree = MarkdownParser(flavor)
        .buildMarkdownTreeFromString(markdown)
    for (child in parsedTree.children) {
        when (child.type) {
            // each second level heading is mapped to a test case
            MarkdownElementTypes.ATX_2 -> {
                if (currentSrcTestOffset >= 0) {
                    val isFailed = processTestOutput(
                        output,
                        currentSrcTestOffset .. child.startOffset,
                        currentTestOffset,
                        testResults
                    )
                    if (isFailed) {
                        failed++
                    }
                }
                output.append(child.getTextInNode(markdown))
                currentTestOffset = output.length
                currentSrcTestOffset = child.endOffset
                total++
            }

            else -> {
                output.append(child.getTextInNode(markdown))
            }
        }
    }
    if (currentSrcTestOffset >= 0) {
        val isFailed = processTestOutput(
            output,
            currentSrcTestOffset .. Int.MAX_VALUE,
            currentTestOffset,
            testResults
        )
        if (isFailed) {
            failed++
        }
    }

    println()
    println(output)

    if (failed > 0) {
        error("$fileName: $failed/$total tests failed")
    } else {
        println("$fileName: $total tests OK")
    }
}

private fun wrapCodeBlocks(source: String): String {
    val flavor = CommonMarkFlavourDescriptor()
    val parsedTree = MarkdownParser(flavor)
        .buildMarkdownTreeFromString(source)
    val processedSource = StringBuilder()
    for (child in parsedTree.children) {
        when (child.type) {
            MarkdownElementTypes.CODE_FENCE,
            MarkdownElementTypes.CODE_BLOCK -> {
                if (processedSource.endsWith("\n")) {
                    processedSource.setLength(
                        processedSource.length - "\n".length
                    )
                    processedSource.append("{% block ")
                    processedSource.append(
                        child.type.name.lowercase()
                    )
                    processedSource.append(" %}")
                    processedSource.append("\n")
                    processedSource.append(child.getTextInNode(source))
                    processedSource.append("{% endblock %}")
                } else {
                    // code block at beginning of file -> column number shift
                    processedSource.append("{% block code_block %}")
                    processedSource.append(child.getTextInNode(source))
                    processedSource.append("{% endblock %}")
                }
            }
            else -> {
                processedSource.append(child.getTextInNode(source))
            }
        }
    }
    return processedSource.toString()
}

private fun processTestOutput(
    output: StringBuilder,
    srcOffsetRange: IntRange,
    currentTestOffset: Int,
    testResults: List<Pair<Int, BlockResult>>,
): Boolean {
    val results = testResults.filter { tr ->
        tr.first in srcOffsetRange
    }
    val isFailed = results.any { it.second.exception != null }
    val tmp = output.substring(currentTestOffset)
    output.setLength(currentTestOffset)
    if (isFailed) {
        output.append(" \u274C")
    } else {
        output.append(" \u2705")
    }
    if (tmp.isBlank()) {
        output.append("\n")
    } else {
        output.append(tmp.replace(Regex("""\n\n\n+"""), "\n\n"))
    }
    return isFailed
}

private class ControlBlock : ControlTag {
    override suspend fun invoke(
        ctx: Context.Builder<*>,
        template: ParsedTemplate,
        branches: List<Branch>
    ) {
        val branch = branches.single()

        val sourceStart = branch.body.first().sourceTokenRange().first.first
        val sourceEnd = branch.body.last().sourceTokenRange().second.last
        val innerCode = template.input.substring(sourceStart .. sourceEnd)

        try {
            val rendered = ctx.scope().renderToString()
                .evalNodes(template, branch.body)
                .result
            if (rendered.trimStart()
                .removePrefix("```")
                .removeSuffix("```")
                .isBlank())
            {
                ctx.emitValue(BlockResult(innerCode, ""))
            } else {
                ctx.emitValue(BlockResult(innerCode, rendered))
            }
        } catch (ex: Exception) {
            //TODO show source extract with location marker
            val rendered = if (innerCode.startsWith("\n")) {
                "\n$ex"
            } else {
                ex.toString()
            }
            ctx.emitValue(BlockResult(innerCode, rendered, ex))
        }
    }
}

private class BlockResult(
    val source: String,
    val value: String,
    val exception: Exception? = null
) : InlineString, CharSequence by value {
    override fun toString(): String {
        return value
    }
}
