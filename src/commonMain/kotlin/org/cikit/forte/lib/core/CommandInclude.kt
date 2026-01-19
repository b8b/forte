package org.cikit.forte.lib.core

import org.cikit.forte.core.*
import org.cikit.forte.parser.Expression
import org.cikit.forte.parser.ParsedTemplate

class CommandInclude : CommandTag {
    override suspend fun invoke(
        ctx: Context.Builder<*>,
        template: ParsedTemplate,
        args: Map<String, Expression>
    ) {
        val fileExpr = args.getValue("file")
        val files = when (val file = ctx.evalExpression(fileExpr)) {
            is CharSequence -> listOf(
                UPath(file.concatToString(), DecodeUrlPath)
            )
            is Iterable<*> -> file.map {
                UPath((it as CharSequence).concatToString(), DecodeUrlPath)
            }
            else -> error("invalid type '${typeName(file)}' for arg 'file'")
        }
        ctx.loadTemplate(files, template.path) { parsedTemplate ->
            ctx.withRootScope().evalTemplate(parsedTemplate)
        }
    }
}
