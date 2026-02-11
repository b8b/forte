package org.cikit.forte.lib.funit

import org.cikit.forte.core.*
import org.cikit.forte.parser.ParsedTemplate

class ControlAssertFails : ControlTag {
    override suspend fun invoke(
        ctx: Context.Builder<*>,
        template: ParsedTemplate,
        branches: List<Branch>
    ) {
        val branch = branches.single()
        val varNameExpr = branch.args["varName"]
        val varName = if (varNameExpr != null) {
            (ctx.evalExpression(varNameExpr) as CharSequence).concatToString()
        } else {
            null
        }
        try {
            ctx.scope().evalNodes(template, branch.body)
        } catch (ex: Exception) {
            if (varName != null) {
                ctx.setVar(varName, ex.toString())
            }
            return
        }
        throw EvalException(
            branch.body.first(),
            "expected failure"
        )
    }
}
