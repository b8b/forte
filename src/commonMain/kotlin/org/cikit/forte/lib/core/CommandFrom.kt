package org.cikit.forte.lib.core

import org.cikit.forte.core.CommandTag
import org.cikit.forte.core.Context
import org.cikit.forte.core.DecodeUrlPath
import org.cikit.forte.core.UPath
import org.cikit.forte.core.concatToString
import org.cikit.forte.parser.Expression
import org.cikit.forte.parser.ParsedTemplate

class CommandFrom : CommandTag {
    override suspend fun invoke(
        ctx: Context.Builder<*>,
        template: ParsedTemplate,
        args: Map<String, Expression>
    ) {
        val file = ctx.evalExpression(args.getValue("file")) as CharSequence
        val functionName = ctx
            .evalExpression(args.getValue("functionName")) as String
        val path = UPath(file.concatToString(), DecodeUrlPath)
        ctx.importTemplate(listOf(path), template.path) { _, importedTemplate ->
            val exportedFunction = importedTemplate.getVar(functionName)
            require(exportedFunction != null) {
                "function $functionName not defined in $file"
            }
            ctx.setVar(functionName, exportedFunction)
        }
    }
}
