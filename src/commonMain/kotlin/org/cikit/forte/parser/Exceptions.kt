package org.cikit.forte.parser

class ParseException private constructor(
    val startToken: Token,
    val endToken: Token,
    val node: Node?,
    val expression: Expression?,
    message: String?,
    cause: Throwable?
) : RuntimeException(message, cause) {
    private constructor(
        tokens: Pair<Token, Token>,
        node: Node?,
        expression: Expression?,
        message: String?,
        cause: Throwable?
    ) : this(tokens.first, tokens.second, node, expression, message, cause)

    constructor(
        startToken: Token,
        message: String? = null,
        cause: Throwable? = null
    ) : this(startToken, startToken, null, null, message, cause)

    constructor(
        startToken: Token,
        endToken: Token,
        message: String? = null,
        cause: Throwable? = null
    ) : this(startToken, endToken, null, null, message, cause)

    constructor(
        node: Node,
        message: String? = null,
        cause: Throwable? = null
    ) : this(node.sourceTokenRange(), node, null, message, cause)

    constructor(
        expression: Expression,
        message: String? = null,
        cause: Throwable? = null
    ) : this(expression.sourceTokenRange(), null, expression, message, cause)
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
