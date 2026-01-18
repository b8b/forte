package org.cikit.forte.lib.core

import org.cikit.forte.core.Branch
import org.cikit.forte.core.Context
import org.cikit.forte.core.ControlTag
import org.cikit.forte.core.typeName
import org.cikit.forte.parser.ParsedTemplate

class ControlIf : ControlTag {
    override suspend fun invoke(
        ctx: Context.Builder<*>,
        template: ParsedTemplate,
        branches: List<Branch>
    ) {
        val cmd = branches.first()
        val condition = cmd.args.getValue("condition")
        val value = ctx.evalExpression(condition)
        require(value is Boolean) {
            "invalid type '${typeName(value)}' " +
                    "for arg 'condition': expected 'Boolean'"
        }
        if (value) {
            ctx.evalNodes(template, cmd.body)
        } else {
            var haveElse = false
            for (i in 1 until branches.size) {
                val cmd = branches[i]
                when (cmd.name) {
                    "elif" if !haveElse -> {
                        val condition = cmd.args.getValue("condition")
                        val value = ctx.evalExpression(condition)
                        require(value is Boolean) {
                            "invalid type '${typeName(value)}' " +
                                    "for arg 'condition': expected 'Boolean'"
                        }
                        if (value) {
                            ctx.evalNodes(template, cmd.body)
                            break
                        }
                    }
                    "else" -> {
                        ctx.evalNodes(template, cmd.body)
                        haveElse = true
                    }
                    else -> error("unexpected command: ${cmd.name}")
                }
            }
        }
    }
}
