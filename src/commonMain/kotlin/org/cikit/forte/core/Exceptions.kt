package org.cikit.forte.core

import org.cikit.forte.parser.*

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
    ) : this(
        startToken = tokens.first,
        endToken = tokens.second,
        node = node,
        expression = expression,
        message = message,
        cause = cause
    )

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

    var template: ParsedTemplate? = null
        private set

    var location: Location? = null
        private set

    val detailedMessage: String
        get() = buildString {
            append(template?.path ?: "<anonymous>")
            location?.let {
                append("[")
                append(it.lineNumber)
                append(":")
                append(it.columnNumber)
                append("]")
            }
            append(": ")
            append(message)
        }

    internal fun setTemplate(template: ParsedTemplate) {
        if (this.template == null) {
            this.template = template
            this.location = LocationInfo(
                template.path,
                template.input,
                startToken
            ).tokenStart
        }
    }
}
