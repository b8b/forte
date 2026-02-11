package org.cikit.forte.lib.core

import org.cikit.forte.core.Branch
import org.cikit.forte.core.Context
import org.cikit.forte.core.ControlTag
import org.cikit.forte.core.EvalException
import org.cikit.forte.core.FilterMethod
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.Suspended
import org.cikit.forte.core.Undefined
import org.cikit.forte.core.concatToString
import org.cikit.forte.core.typeName
import org.cikit.forte.parser.ParsedTemplate

class ControlFilter : ControlTag {
    override suspend fun invoke(
        ctx: Context.Builder<*>,
        template: ParsedTemplate,
        branches: List<Branch>
    ) {
        val cmd = branches.single()
        val nameExpr = cmd.args.getValue("name")
        val argNamesExpr = cmd.args.getValue("argNames")
        val argValuesExpr = cmd.args.getValue("argValues")
        val name = ctx.evalExpression(nameExpr)
        val argNames = buildList {
            val items = ctx.evalExpression(argNamesExpr) as List<*>
            for (item in items) {
                this += (item as CharSequence).concatToString()
            }
        }
        val argValues = ctx.evalExpression(argValuesExpr) as List<*>
        require(name is CharSequence) {
            "invalid type '${typeName(name)}' for filter name"
        }
        val filter = ctx.getMethod(
            Context.Key.Apply.create(
                name.concatToString(),
                FilterMethod.OPERATOR
            )
        ) ?: throw EvalException(
            nameExpr,
            "filter '$name' is not defined"
        )
        val filterArgs = NamedArgs(
            values = argValues,
            names = argNames
        )
        val subject = ctx.scope()
            .renderToString()
            .evalNodes(template, cmd.body)
            .result
        var result = filter(subject, filterArgs)
        if (result is Suspended) {
            result = result.eval(ctx)
        }
        if (result is Undefined) {
            error(result.message)
        }
        ctx.emitValue(result)
    }
}
