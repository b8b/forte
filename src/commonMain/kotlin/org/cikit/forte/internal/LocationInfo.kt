package org.cikit.forte.internal

import org.cikit.forte.core.UPath
import org.cikit.forte.parser.ExpressionTokenizer
import org.cikit.forte.parser.Location
import org.cikit.forte.parser.Token

internal class LocationInfo(
    val path: UPath?,
    val input: String,
    val startToken: Token,
    val endToken: Token,
    val nl: String = "\n",
    lineRanges: List<IntRange> = buildList {
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
        startToken: Token,
        endToken: Token
    ) : this(tokenizer.path, tokenizer.input, startToken, endToken)

    val startLocation: Location
    val endLocation: Location

    init {
        var line1 = -1
        var line2 = -1
        var col1 = -1
        var col2 = -1
        var lineRange1 = -1 .. -1
        var lineRange2 = -1 .. -1
        if (startToken.first >= input.length) {
            val lastRange = lineRanges.last()
            line1 = lineRanges.size
            line2 = line1
            col1 = lastRange.last - lastRange.first + 2
            col2 = col1
        } else {
            for ((l, range) in lineRanges.withIndex()) {
                if (line1 < 0) {
                    if (startToken.first in range) {
                        line1 = l + 1
                        col1 = startToken.first - range.first + 1
                        lineRange1 = range
                    }
                }
                if (endToken.last in range) {
                    line2 = l + 1
                    col2 = endToken.last - range.first + 1
                    lineRange2 = range
                    break
                }
            }
        }
        startLocation = Location(line1, col1, lineRange1)
        endLocation = Location(line2, col2, lineRange2)
    }
}