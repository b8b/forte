package org.cikit.forte.parser

interface TemplateTokenizer : ExpressionTokenizer {
    fun tokenizeInitial(): Pair<Token, Token?>
    fun tokenizeEndComment(): Pair<Token, Token>
}
