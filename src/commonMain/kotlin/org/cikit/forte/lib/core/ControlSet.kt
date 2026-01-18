package org.cikit.forte.lib.core

import org.cikit.forte.core.*
import org.cikit.forte.parser.ParsedTemplate

class ControlSet : ControlTag {
    override suspend fun invoke(
        ctx: Context.Builder<*>,
        template: ParsedTemplate,
        branches: List<Branch>
    ) {
        val cmd = branches.single()
        val varName = cmd.args.getValue("varName")
        val value = ctx.scope()
            .renderToString()
            .evalNodes(template, cmd.body)
            .result
        ctx.setVar(
            ctx.evalExpression(varName) as String,
            value
        )
    }
}
