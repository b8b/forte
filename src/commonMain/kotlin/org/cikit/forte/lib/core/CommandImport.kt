package org.cikit.forte.lib.core

import org.cikit.forte.core.*
import org.cikit.forte.parser.Expression
import org.cikit.forte.parser.ParsedTemplate

class CommandImport : CommandTag {
    override suspend fun invoke(
        ctx: Context.Builder<*>,
        template: ParsedTemplate,
        args: Map<String, Expression>
    ) {
        val file = ctx.evalExpression(args.getValue("file")) as CharSequence
        val varName = ctx.evalExpression(args.getValue("varName")) as String
        val path = UPath(file.concatToString(), DecodeUrlPath)
        ctx.importTemplate(listOf(path), template.path) { _, importedTemplate ->
            ctx.setVar(varName, importedTemplate)
        }
    }
}
