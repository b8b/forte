package org.cikit.forte.lib.core

import org.cikit.forte.core.Branch
import org.cikit.forte.core.Context
import org.cikit.forte.core.ControlTag
import org.cikit.forte.core.Function
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.Suspended
import org.cikit.forte.core.TemplateLoader
import org.cikit.forte.parser.Expression
import org.cikit.forte.parser.Node
import org.cikit.forte.parser.ParsedTemplate

class ControlMacro : ControlTag {
    override suspend fun invoke(
        ctx: Context.Builder<*>,
        template: ParsedTemplate,
        branches: List<Branch>
    ) {
        val cmd = branches.single()
        val functionNameExpr = cmd.args.getValue("name")
        val argNamesExpr = cmd.args.getValue("argNames")
        val argDefaultsExpr = cmd.args.getValue("argDefaults")
        val functionName = ctx.evalExpression(functionNameExpr) as String
        val argNames = ctx.evalExpression(argNamesExpr) as List<*>
        argDefaultsExpr as Expression.ObjectLiteral

        val finalArgDefaults = argDefaultsExpr.pairs.associate { (k, v) ->
            (ctx.evalExpression(k) as String) to v
        }
        val finalArgNames = argNames.map { name -> name as String }

        val defCtx = ctx.build()

        ctx.defineFunction(
            functionName,
            MacroFunction(
                defCtx,
                finalArgNames,
                finalArgDefaults,
                template,
                cmd.body
            )
        )
    }

    private class MacroFunction(
        val defCtx: Context<*>,
        val finalArgNames: List<String>,
        val finalArgDefaults: Map<String, Expression>,
        val template: ParsedTemplate,
        val body: List<Node>
    ) : Function {
        override fun invoke(args: NamedArgs): Any = Suspended { ctx ->
            val macroCtx = ctx.withScope(defCtx)
            val evaluatedArgs = mutableMapOf<String, Any?>()
            args.use {
                for (i in finalArgNames.indices) {
                    val name = finalArgNames[i]
                    val defaultValue = finalArgDefaults[name]
                    val value = if (defaultValue == null) {
                        requireAny(name)
                    } else {
                        optionalNullable(
                            name,
                            { it },
                            { defaultValue }
                        )
                    }
                    evaluatedArgs[name] = value
                }
            }
            for ((name, value) in evaluatedArgs) {
                var value = value
                if (value is Expression) {
                    value = macroCtx.evalExpression(value)
                }
                macroCtx.setVar(name, value)
            }
            macroCtx.renderToString()
                .evalNodes(template, body)
                .result
        }
    }
}
