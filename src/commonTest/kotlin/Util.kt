import kotlinx.coroutines.test.runTest
import org.cikit.forte.Forte
import org.cikit.forte.core.UPath
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

expect fun readTests(fileName: String): String

fun Forte.runTests(fileName: String) = runTests(fileName) { testCase ->
    val template = parseTemplate(testCase, UPath(fileName))
    renderToString()
        .evalTemplate(template)
        .result
}

fun runTests(
    fileName: String,
    render: suspend (String) -> String
) = runTest {
    val source = readTests(fileName)
    val testCases = extractTestCases(source)
    var failed = 0
    var includeHeader = true
    val output = StringBuilder()

    for (testCase in testCases) {
        try {
            val result = render(testCase)
            appendTestOutput(
                output,
                result,
                failed = false,
                includeHeader = includeHeader
            )
            includeHeader = false
        } catch (ex: Throwable) {
            failed++
            val markdown = buildString {
                append(extractTestSource(testCase))
                appendLine()
                appendLine()
                for (exLine in ex.toString().split("\n")) {
                    append("    ")
                    appendLine(exLine)
                }
            }
            appendTestOutput(
                output,
                markdown,
                failed = true,
                includeHeader = includeHeader
            )
        }
    }

    println()
    println(output)

    if (failed > 0) {
        error("$fileName: $failed/${testCases.size} tests failed")
    } else {
        println("$fileName: ${testCases.size} tests OK")
    }
}

fun extractTestCases(source: String): List<String> {
    val testCases = mutableListOf<String>()
    val flavor = CommonMarkFlavourDescriptor()
    val parsedTree = MarkdownParser(flavor)
        .buildMarkdownTreeFromString(source)
    val processedSource = StringBuilder()
    var currentTestOffset = -1
    for (child in parsedTree.children) {
        when (child.type) {
            MarkdownElementTypes.ATX_2 -> {
                if (currentTestOffset >= 0) {
                    testCases.add(processedSource.toString())
                    val currentLength = processedSource.length
                    for (i in currentTestOffset until currentLength) {
                        if (processedSource[i] != '\n') {
                            processedSource[i] = ' '
                        }
                    }
                    currentTestOffset = currentLength
                } else {
                    currentTestOffset = processedSource.length
                }
                processedSource.append(child.getTextInNode(source))
            }
            else -> {
                processedSource.append(child.getTextInNode(source))
            }
        }
    }
    if (currentTestOffset >= 0) {
        testCases.add(processedSource.toString())
    }
    return testCases.toList()
}

private fun extractTestSource(testCase: String): String {
    val flavor = CommonMarkFlavourDescriptor()
    val parsedTree = MarkdownParser(flavor)
        .buildMarkdownTreeFromString(testCase)
    val output = StringBuilder()
    var doOutput = false
    for (child in parsedTree.children) {
        when (child.type) {
            MarkdownElementTypes.ATX_2 -> {
                output.append(child.getTextInNode(testCase))
                doOutput = true
            }
            MarkdownElementTypes.CODE_FENCE -> {
                if (doOutput) {
                    var firstLine = (0 until child.startOffset)
                        .count { testCase[it] == '\n' } + 1
                    val lines = child.getTextInNode(testCase).split("\n")
                    val lastLine = firstLine + lines.size - 1
                    val padding = "$lastLine".length
                    for (line in lines) {
                        val lineStr = "${firstLine++}"
                        if (!line.startsWith("```")) {
                            repeat(padding - lineStr.length) {
                                output.append(" ")
                            }
                            output.append(lineStr)
                            output.append(": ")
                        }
                        output.appendLine(line)
                    }
                }
            }
            MarkdownElementTypes.CODE_BLOCK -> {
                if (doOutput) {
                    var firstLine = (0 until child.startOffset)
                        .count { testCase[it] == '\n' } + 1
                    val lines = child.getTextInNode(testCase).split("\n")
                    val lastLine = firstLine + lines.size - 1
                    val padding = "$lastLine".length
                    var indent = lines
                        .first()
                        .indexOfFirst { !it.isWhitespace() }
                    if (indent < 0) {
                        indent = 0
                    }
                    for (line in lines) {
                        val lineStr = "${firstLine++}"
                        repeat(indent + padding - lineStr.length) {
                            output.append(" ")
                        }
                        output.append(lineStr)
                        output.append(": ")
                        output.appendLine(line.substring(indent))
                    }
                }
            }

            else -> {
                if (doOutput) {
                    output.append(child.getTextInNode(testCase))
                }
            }
        }
    }
    while (output.lastOrNull()?.isWhitespace() == true) {
        output.setLength(output.length - 1)
    }
    return output.toString()
}

private fun appendTestOutput(
    output: StringBuilder,
    markdown: String,
    failed: Boolean,
    includeHeader: Boolean = true
) {
    val flavor = CommonMarkFlavourDescriptor()
    val parsedTree = MarkdownParser(flavor)
        .buildMarkdownTreeFromString(markdown)
    var doOutput = includeHeader
    var textOffset = -1
    for (child in parsedTree.children) {
        when (child.type) {
            // each second level heading is mapped to a test case
            MarkdownElementTypes.ATX_2 -> {
                if (includeHeader) {
                    while (output.lastOrNull()?.isWhitespace() == true) {
                        output.setLength(output.length - 1)
                    }
                    output.appendLine()
                    output.appendLine()
                }
                output.append(child.getTextInNode(markdown))
                if (failed) {
                    output.append(" \u274C")
                } else {
                    output.append(" \u2705")
                }
                textOffset = output.length
                doOutput = true
            }

            else -> {
                if (doOutput) {
                    output.append(child.getTextInNode(markdown))
                }
            }
        }
    }
    if (textOffset >= 0) {
        val textOutput = output.substring(textOffset, output.length)
        if (textOutput.isBlank()) {
            output.setLength(textOffset)
            output.appendLine()
        } else {
            while (output.lastOrNull()?.isWhitespace() == true) {
                output.setLength(output.length - 1)
            }
            output.appendLine()
            output.appendLine()
        }
    }
}
