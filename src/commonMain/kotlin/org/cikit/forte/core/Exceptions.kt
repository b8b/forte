package org.cikit.forte.core

import org.cikit.forte.parser.Expression
import org.cikit.forte.parser.Node
import org.cikit.forte.parser.Token
import org.cikit.forte.parser.sourceTokenRange

class EvalException private constructor(
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
    )  : this(startToken, startToken, null, null, message, cause)

    constructor(
        startToken: Token,
        endToken: Token,
        message: String? = null,
        cause: Throwable? = null
    )  : this(startToken, endToken, null, null, message, cause)

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

    val detailedMessage: String
        get() = "$startToken .. $endToken: $message"
}
