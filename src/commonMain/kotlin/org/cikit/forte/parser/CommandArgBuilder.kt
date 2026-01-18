package org.cikit.forte.parser

interface CommandArgBuilder : ExpressionParser {
    val name: String
    val args: MutableMap<String, Expression>
    val branchAliases: MutableSet<String>
    val endAliases: MutableSet<String>
}
