package org.cikit.forte.lib.funit

import org.cikit.forte.core.*
import org.cikit.forte.lib.core.IsInTest
import org.cikit.forte.parser.ParsedTemplate

class ControlAssert : ControlTag {
    override suspend fun invoke(
        ctx: Context.Builder<*>,
        template: ParsedTemplate,
        branches: List<Branch>
    ) {
        val branch = branches.single()
        val testNameExpr = branch.args.getValue("test")
        val argNamesExpr = branch.args.getValue("argNames")
        val argValuesExpr = branch.args.getValue("argValues")

        val testName = ctx.evalExpression(testNameExpr) as String

        var callInAsContains = false
        val key = if (testName == "contains") {
            callInAsContains = true
            IsInTest.KEY
        } else {
            Context.Key.Apply.create(testName, TestMethod.OPERATOR)
        }
        val testFunction = ctx.getMethod(key)
            ?: throw EvalException(
                testNameExpr,
                "test '$testName' not defined"
            )
        val argNames = buildList {
            val items = ctx.evalExpression(argNamesExpr) as List<*>
            for (item in items) {
                this += (item as CharSequence).concatToString()
            }
        }
        val argValues = ctx.evalExpression(argValuesExpr) as List<*>
        val subject = ctx.scope()
            .renderToString()
            .evalNodes(template, branch.body)
            .result
        var result = if (callInAsContains) {
            testFunction(
                argValues.single(),
                NamedArgs(listOf(subject), emptyList())
            )
        } else {
            testFunction(subject, NamedArgs(argValues, argNames))
        }
        if (result is Suspended) {
            result = result.eval(ctx)
        }
        if (result != true) {
            throw EvalException(
                testNameExpr,
                "assertion failed on '$subject'"
            )
        }
    }
}
