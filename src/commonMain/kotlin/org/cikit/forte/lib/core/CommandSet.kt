package org.cikit.forte.lib.core

import org.cikit.forte.core.CommandTag
import org.cikit.forte.core.Context
import org.cikit.forte.parser.Expression
import org.cikit.forte.parser.ParsedTemplate

class CommandSet : CommandTag {
    override suspend fun invoke(
        ctx: Context.Builder<*>,
        template: ParsedTemplate,
        args: Map<String, Expression>
    ) {
        val varNamesExpr = args.getValue("varNames")
        val valueExpr = args.getValue("value")
        val varNames = ctx.evalExpression(varNamesExpr)
        val value = ctx.evalExpression(valueExpr)
        varNames as List<*>
        for ((k, v) in unpackList(varNames, value)) {
            ctx.setVar(k, v)
        }
    }
}

