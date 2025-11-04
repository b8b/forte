package org.cikit.forte.parser

import org.cikit.forte.core.LocationInfo

class ParseException private constructor(
    locationInfo: LocationInfo,
    val tokenizer: ExpressionTokenizer,
    val node: Node?,
    val expression: Expression?,
    val errorMessage: String,
) : RuntimeException(
    locationInfo.buildMessage(node, expression, errorMessage)
) {
    val token: Token = locationInfo.token
    val tokenStart = locationInfo.tokenStart
    val tokenEnd = locationInfo.tokenEnd

    constructor(
        tokenizer: ExpressionTokenizer,
        token: Token,
        errorMessage: String,
    ) : this(
        locationInfo = LocationInfo(tokenizer, token),
        tokenizer = tokenizer,
        node = null,
        expression = null,
        errorMessage = errorMessage
    )

    constructor(
        tokenizer: ExpressionTokenizer,
        node: Node,
        errorMessage: String,
    ) : this(
        LocationInfo(tokenizer, node.sourceTokenRange().first),
        tokenizer,
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
        tokenizer,
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
    is Expression.ByteStringLiteral -> node.first to node.last
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
