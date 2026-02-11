package org.cikit.forte.lib.funit

import org.cikit.forte.core.*
import org.cikit.forte.parser.Expression
import org.cikit.forte.parser.ParsedTemplate

class CommandAssertThat : CommandTag {
    override suspend fun invoke(
        ctx: Context.Builder<*>,
        template: ParsedTemplate,
        args: Map<String, Expression>
    ) {
        val subject = ctx.evalExpression(args.getValue("subject"))
        val testNameExpr = args.getValue("test")
        val argNamesExpr = args.getValue("argNames")
        val argValuesExpr = args.getValue("argValues")
        val testName = ctx.evalExpression(testNameExpr) as CharSequence
        val argNames = buildList {
            val items = ctx.evalExpression(argNamesExpr) as List<*>
            for (item in items) {
                this += (item as CharSequence).concatToString()
            }
        }
        val argValues = ctx.evalExpression(argValuesExpr) as List<*>
        val testFunction = ctx.getMethod(
            Context.Key.Apply.create(
                testName.concatToString(),
                TestMethod.OPERATOR
            )
        ) ?: throw EvalException(
            testNameExpr,
            "test '$testName' is not defined"
        )
        var result = testFunction(subject, NamedArgs(argValues, argNames))
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
