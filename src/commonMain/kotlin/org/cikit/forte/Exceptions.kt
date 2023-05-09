package org.cikit.forte

class ParseException private constructor(
    val startToken: Token,
    val endToken: Token,
    val node: Node?,
    message: String?,
    cause: Throwable?
) : RuntimeException(message, cause) {

    private constructor(
        tokens: Pair<Token, Token>,
        node: Node,
        message: String?,
        cause: Throwable?
    ) : this(tokens.first, tokens.second, node, message, cause)

    constructor(
        startToken: Token,
        message: String? = null,
        cause: Throwable? = null
    )  : this(startToken, startToken, null, message, cause)

    constructor(
        startToken: Token,
        endToken: Token,
        message: String? = null,
        cause: Throwable? = null
    )  : this(startToken, endToken, null, message, cause)

    constructor(
        node: Node,
        message: String? = null,
        cause: Throwable? = null
    )  : this(reportTokens(node), node, message, cause)

}

private fun reportTokens(node: Node): Pair<Token, Token> {
    val t: Pair<Token, Token> = when (node) {
        is Node.Text -> node.content to node.content
        is Node.Comment -> node.first to node.last
        is Node.Command -> node.first to node.last
        is Node.Control -> {
            val lastBr = node.branches.lastOrNull() ?: node.first
            reportTokens(node.first.first).first to
                    reportTokens(lastBr.last).second
        }
        is Node.Emit -> node.first to node.last

        is Node.SubExpression -> reportTokens(node.content)

        is Node.ObjectLiteral -> node.first to node.last
        is Node.ArrayLiteral -> node.first to node.last
        is Node.StringLiteral -> node.first to node.last
        is Node.StringInterpolation -> {
            reportTokens(node.children.first()).first to
                    reportTokens(node.children.last()).second
        }
        is Node.NumericLiteral -> node.first to node.last
        is Node.BooleanLiteral -> node.token to node.token
        is Node.NullLiteral -> node.token to node.token

        is Node.Variable -> node.first to node.first
        is Node.FunctionCall -> node.first to node.first
        is Node.ExtensionCall -> node.first to node.first
        is Node.MethodCall -> {
            val t = reportTokens(node.left).second
            t to t
        }

        is Node.BinOp -> node.tokens.first() to node.tokens.last()
        is Node.UnOp -> node.tokens.first() to node.tokens.last()
        is Node.Access -> node.first to node.last
        is Node.CompAccess -> node.first to node.first

        is Node.Malformed -> node.tokens.first() to node.tokens.last()
        is Node.Empty -> error("cannot get token for empty expression")
    }
    return t
}
