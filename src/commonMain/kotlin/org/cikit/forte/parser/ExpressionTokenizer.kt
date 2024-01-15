package org.cikit.forte.parser

interface ExpressionTokenizer {
    val input: String
    fun tokenizeSingleString(): Pair<Token, Token>
    fun tokenizeDoubleString(): Pair<Token, Token>
    fun tokenize(skipSpace: Boolean = false): Token
    fun peek(skipSpace: Boolean = false): Token
    fun peekAfter(token: Token, skipSpace: Boolean = false): Token
    fun consume(t: Token)
}