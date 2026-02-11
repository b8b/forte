package org.cikit.forte.lib.funit

import org.cikit.forte.core.CommandTag
import org.cikit.forte.core.Context
import org.cikit.forte.core.EvalException
import org.cikit.forte.parser.Expression
import org.cikit.forte.parser.ParsedTemplate

class CommandAssertBoolean(
    val cond: Boolean
) : CommandTag {
    override suspend fun invoke(
        ctx: Context.Builder<*>,
        template: ParsedTemplate,
        args: Map<String, Expression>
    ) {
        val testExpression = args.getValue("test")
        val result = ctx.evalExpression(testExpression)
        if (result != cond) {
            throw EvalException(
                testExpression,
                "assertion failed"
            )
        }
    }
}
