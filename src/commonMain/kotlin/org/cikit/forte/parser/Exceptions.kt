package org.cikit.forte.parser

class ParseException private constructor(
    locationInfo: LocationInfo,
    val node: Node?,
    val expression: Expression?,
    val errorMessage: String,
) : RuntimeException(
    locationInfo.buildMessage(node, expression, errorMessage)
) {
    val tokenizer: ExpressionTokenizer = locationInfo.tokenizer
    val token: Token = locationInfo.token

    val tokenStart = locationInfo.tokenStart
    val tokenEnd = locationInfo.tokenEnd

    constructor(
        tokenizer: ExpressionTokenizer,
        token: Token,
        errorMessage: String,
    ) : this(LocationInfo(tokenizer, token), null, null, errorMessage)

    constructor(
        tokenizer: ExpressionTokenizer,
        node: Node,
        errorMessage: String,
    ) : this(
        LocationInfo(tokenizer, node.sourceTokenRange().first),
        node,
        null,
        errorMessage
    )

    constructor(
        tokenizer: ExpressionTokenizer,
        expression: Expression,
        errorMessage: String,
    ) : this(
        LocationInfo(tokenizer, expression.sourceTokenRange().first),
        null,
        expression,
        errorMessage
    )
}

class Location(
    val lineNumber: Int,
    val columnNumber: Int,
    val lineRange: IntRange
)

private class LocationInfo(
    val tokenizer: ExpressionTokenizer,
    val token: Token
) {
    val lineRanges: List<IntRange> = tokenizer.lineRanges()
    val tokenStart: Location
    val tokenEnd: Location

    init {
        var line1 = -1
        var line2 = -1
        var col1 = -1
        var col2 = -1
        var lineRange1 = -1 .. -1
        var lineRange2 = -1 .. -1
        if (token.first >= tokenizer.input.length) {
            val lineRanges = tokenizer.lineRanges()
            val lastRange = lineRanges.last()
            line1 = lineRanges.size
            line2 = line1
            col1 = lastRange.last - lastRange.first + 2
            col2 = col1
        } else {
            for ((l, range) in tokenizer.lineRanges().withIndex()) {
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

    fun ExpressionTokenizer.lineRanges(nl: String = "\n") = buildList {
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
    }

    fun buildMessage(
        node: Node?,
        expression: Expression?,
        errorMessage: String?
    ) = buildString {
        append(tokenizer.path ?: "<anonymous>")
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

fun Node.sourceTokenRange(): Pair<Token, Token> = when (val node = this) {
    is Node.Text -> node.content to node.content
    is Node.Emit -> node.first to node.last
    is Node.Comment -> node.first to node.last
    is Node.Command -> node.first to node.last
    is Node.Control -> {
        val lastBr = node.branches.lastOrNull() ?: node.first
        node.first.first.sourceTokenRange().first to
                lastBr.last.sourceTokenRange().second
    }
}

fun Expression.sourceTokenRange(): Pair<Token, Token> = when (val node = this) {
    is Expression.SubExpression -> node.content.sourceTokenRange()
    is Expression.ObjectLiteral -> node.first to node.last
    is Expression.ArrayLiteral -> node.first to node.last
    is Expression.StringLiteral -> node.first to node.last
    is Expression.StringInterpolation -> {
        node.children.first().sourceTokenRange().first to
                node.children.last().sourceTokenRange().second
    }
    is Expression.NumericLiteral -> node.first to node.last
    is Expression.BooleanLiteral -> node.token to node.token
    is Expression.NullLiteral -> node.token to node.token
    is Expression.Variable -> node.first to node.first
    is Expression.FunctionCall -> node.first to node.first
    is Expression.TransformOp -> node.tokens.first() to node.tokens.last()
    is Expression.InvokeOp -> node.first to node.last
    is Expression.BinOp -> node.tokens.first() to node.tokens.last()
    is Expression.UnOp -> node.tokens.first() to node.tokens.last()
    is Expression.Access -> node.first to node.last
    is Expression.CompAccess -> node.first to node.first
    is Expression.Malformed -> node.tokens.first() to node.tokens.last()
}
