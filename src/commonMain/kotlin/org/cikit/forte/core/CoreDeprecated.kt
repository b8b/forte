package org.cikit.forte.core

import org.cikit.forte.eval.EvaluationResult
import org.cikit.forte.eval.tryEvalExpression
import org.cikit.forte.parser.Expression

object CoreDeprecated {
    fun <T> Context.Builder<T>.defineDeprecatedFunctions(): Context.Builder<T> {
        defineCommand("set") { ctx, args ->
            val varName = args.getValue("varName")
            val value = args.getValue("value")
            ctx.setVar(
                ctx.evalExpression(varName) as String,
                ctx.evalExpression(value)
            )
        }
        defineControl("if") { ctx, branches ->
            for (cmd in branches) {
                when (cmd.name) {
                    "else" -> {
                        ctx.evalTemplate(cmd.body)
                        break
                    }

                    else -> {
                        val condition = cmd.args.getValue("condition")
                        if (ctx.evalExpression(condition) as Boolean) {
                            ctx.evalTemplate(cmd.body)
                            break
                        }
                    }
                }
            }
        }
        defineControl("for") { ctx, branches ->
            for (cmd in branches) {
                when (cmd.name) {
                    "else" -> {
                        ctx.evalTemplate(cmd.body)
                        break
                    }

                    else -> {
                        val varNames = cmd.args.getValue("varNames")
                        val listValue = cmd.args.getValue("listValue")
                        val varName = (ctx.evalExpression(varNames) as List<*>)
                            .singleOrNull() ?: error(
                            "destructuring in for loop is not implemented"
                        )
                        var done = false
                        val list = ctx.evalExpression(listValue)
                        for (item in list as Iterable<*>) {
                            done = true
                            ctx.scope()
                                .setVar(varName as String, item)
                                .evalTemplate(cmd.body)
                        }
                        if (done) {
                            break
                        }
                    }
                }
            }
        }
        defineControl("macro") { ctx, branches ->
            val cmd = branches.single()
            val functionNameExpr = cmd.args.getValue("name")
            val argNamesExpr = cmd.args.getValue("argNames")
            val argDefaultsExpr = cmd.args.getValue("argDefaults")
            val functionName = ctx.evalExpression(functionNameExpr) as String
            val argNames = ctx.evalExpression(argNamesExpr) as List<*>
            argDefaultsExpr as Expression.ObjectLiteral

            val finalArgDefaults = argDefaultsExpr.pairs.associate { (k, v) ->
                ctx.evalExpression(k).toString() to v
            }
            val finalArgNames = argNames.map { name -> name as String }

            val defCtx = ctx.build()

            ctx.defineFunction(functionName) { _, args ->
                val macroCtx = defCtx.builder()
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
                                { macroCtx.tryEvalExpression(defaultValue) }
                            )
                        }
                        macroCtx.setVar(name, value)
                    }
                }
                for (name in finalArgNames) {
                    when (val value = macroCtx.getVar(name)) {
                        is EvaluationResult -> {
                            macroCtx.setVar(name, value.getOrThrow())
                        }
                    }
                }
                macroCtx.captureToString()
                    .evalTemplate(cmd.body)
                    .result
            }
        }
        return this
    }
}
