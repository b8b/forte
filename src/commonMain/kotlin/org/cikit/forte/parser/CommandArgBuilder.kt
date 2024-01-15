package org.cikit.forte.parser

interface CommandArgBuilder : ExpressionParser {
    val name: String
    val args: MutableMap<String, Expression>
}
