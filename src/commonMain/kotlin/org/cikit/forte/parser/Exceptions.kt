package org.cikit.forte.parser

import org.cikit.forte.internal.LocationInfo

class ParseException private constructor(
    locationInfo: LocationInfo,
    val tokenizer: ExpressionTokenizer,
    @Deprecated("parser should come up with more specific error location")
    val node: Node?,
    val expression: Expression?,
    errorMessage: String,
    cause: Throwable?
) : RuntimeException(errorMessage, cause) {
    val location: Location = locationInfo.startLocation
    val startToken: Token = locationInfo.startToken
    val endToken: Token = locationInfo.endToken

    @Deprecated("replace with startToken")
    val token: Token
        get() = startToken

    @Deprecated("replace with location")
    val tokenStart: Location
        get() = location

    @Deprecated("use location + endToken.last - startToken.first")
    val tokenEnd = locationInfo.endLocation

    constructor(
        tokenizer: ExpressionTokenizer,
        token: Token,
        errorMessage: String,
    ) : this(
        locationInfo = LocationInfo(tokenizer, token, token),
        tokenizer = tokenizer,
        node = null,
        expression = null,
        errorMessage = errorMessage,
        cause = null
    )

    constructor(
        tokenizer: ExpressionTokenizer,
        startToken: Token,
        endToken: Token,
        errorMessage: String,
    ) : this(
        locationInfo = LocationInfo(tokenizer, startToken, endToken),
        tokenizer = tokenizer,
        node = null,
        expression = null,
        errorMessage = errorMessage,
        cause = null
    )

    @Deprecated("parser should come up with more specific error location")
    constructor(
        tokenizer: ExpressionTokenizer,
        node: Node,
        errorMessage: String,
    ) : this(
        locationInfo = node.sourceTokenRange().let { (t1, t2) ->
            LocationInfo(tokenizer, t1, t2)
        },
        tokenizer = tokenizer,
        node = node,
        expression = null,
        errorMessage = errorMessage,
        cause = null
    )

    constructor(
        tokenizer: ExpressionTokenizer,
        expression: Expression,
        errorMessage: String,
    ) : this(
        locationInfo = expression.sourceTokenRange().let { (t1, t2) ->
            LocationInfo(tokenizer, t1, t2)
        },
        tokenizer = tokenizer,
        node = null,
        expression = expression,
        errorMessage = errorMessage,
        cause = null
    )

    constructor(
        tokenizer: ExpressionTokenizer,
        expression: Expression,
        cause: Throwable
    ) : this(
        locationInfo = expression.sourceTokenRange().let { (t1, t2) ->
            LocationInfo(tokenizer, t1, t2)
        },
        tokenizer = tokenizer,
        node = null,
        expression = expression,
        errorMessage = cause.message ?: cause.toString(),
        cause = null
    )

    override val message: String
        get() = buildString {
            append(tokenizer.path ?: "<anonymous>")
            append("[")
            append(location.lineNumber)
            append(":")
            append(location.columnNumber)
            append("]")
            append(": ")
            append(super.message)
        }
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
    is Expression.SliceAccess -> node.first to node.first
    is Expression.Malformed -> node.tokens.first() to node.tokens.last()
}
