package org.cikit.forte.core

import org.cikit.forte.parser.Expression
import org.cikit.forte.parser.ExpressionTokenizer
import org.cikit.forte.parser.Location
import org.cikit.forte.parser.Node
import org.cikit.forte.parser.Token

internal class LocationInfo(
    val path: UPath?,
    val input: String,
    val token: Token,
    val nl: String = "\n",
    val lineRanges: List<IntRange> = buildList {
        if (input.isEmpty()) {
            add(0 .. 0)
        } else {
            var index = 0
            while (true) {
                val next = input.indexOf(nl, startIndex = index)
                if (next < 0) {
                    if (index < input.length - 1) {
                        add(index..<input.length)
                    }
                    break
                }
                add(index..next)
                index = next + nl.length
            }
        }
    },
) {
    constructor(
        tokenizer: ExpressionTokenizer,
        token: Token
    ) : this(tokenizer.path, tokenizer.input, token)

    val tokenStart: Location
    val tokenEnd: Location

    init {
        var line1 = -1
        var line2 = -1
        var col1 = -1
        var col2 = -1
        var lineRange1 = -1 .. -1
        var lineRange2 = -1 .. -1
        if (token.first >= input.length) {
            val lastRange = lineRanges.last()
            line1 = lineRanges.size
            line2 = line1
            col1 = lastRange.last - lastRange.first + 2
            col2 = col1
        } else {
            for ((l, range) in lineRanges.withIndex()) {
                if (line1 < 0) {
                    if (token.first in range) {
                        line1 = l + 1
                        col1 = token.first - range.first + 1
                        lineRange1 = range
                    }
                }
                if (token.last in range) {
                    line2 = l + 1
                    col2 = token.last - range.first + 1
                    lineRange2 = range
                    break
                }
            }
        }
        tokenStart = Location(line1, col1, lineRange1)
        tokenEnd = Location(line2, col2, lineRange2)
    }

    fun buildMessage(
        node: Node?,
        expression: Expression?,
        errorMessage: String?
    ) = buildString {
        append(path ?: "<anonymous>")
        append("[")
        append(tokenStart.lineNumber)
        append(":")
        append(tokenStart.columnNumber)
        append("]: ")
        if (node != null) {
            append(node)
            append(": ")
        }
        if (expression != null) {
            append(expression)
            append(": ")
        }
        append(errorMessage)
    }
}
