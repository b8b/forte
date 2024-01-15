package org.cikit.forte.parser

interface ExpressionParser {
    val tokenizer: ExpressionTokenizer
    val input: String get() = tokenizer.input

    fun copy(declarations: List<Declarations> = emptyList()): ExpressionParser

    fun parsePrimary(): Expression = parsePrimaryOrNull()
        ?: throw ParseException(tokenizer.peek(), "expected expression")
    fun parsePrimaryOrNull(): Expression?
    fun parseExpression(): Expression = parseExpressionOrNull()
        ?: throw ParseException(tokenizer.peek(), "expected expression")
    fun parseExpressionOrNull(): Expression?
    fun parseExpression(
        lhs: Expression,
        minPrecedence: Int
    ): Expression
}
