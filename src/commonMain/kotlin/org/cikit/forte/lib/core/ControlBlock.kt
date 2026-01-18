package org.cikit.forte.lib.core

import org.cikit.forte.core.Branch
import org.cikit.forte.core.Context
import org.cikit.forte.core.ControlTag
import org.cikit.forte.core.InlineString
import org.cikit.forte.parser.ParsedTemplate

class ControlBlock : ControlTag {

    companion object {
        val KEY = Context.Key.Control("block")
    }

    class RenderedBlock(
        val blockName: String,
        val value: String
    ) : InlineString, CharSequence by value {
        override fun toString(): String {
            return value
        }
    }

    override suspend fun invoke(
        ctx: Context.Builder<*>,
        template: ParsedTemplate,
        branches: List<Branch>
    ) {
        val branch = branches.single()
        if (branch.name == "block") {
            val blockName = ctx.evalExpression(
                branch.args.getValue("blockName")
            ) as String
            val result = ctx
                .renderToString()
                .evalNodes(template, branch.body)
                .result
            ctx.emitValue(RenderedBlock(blockName, result))
        }
    }
}
