package org.cikit.forte.parser

import okio.Path

interface TemplateTokenizer : ExpressionTokenizer {
    val path: Path?
    fun tokenizeInitial(): Pair<Token, Token?>
    fun tokenizeEndComment(): Pair<Token, Token>
}