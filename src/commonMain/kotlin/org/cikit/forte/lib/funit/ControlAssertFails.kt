package org.cikit.forte.lib.funit

import org.cikit.forte.core.Branch
import org.cikit.forte.core.Context
import org.cikit.forte.core.ControlTag
import org.cikit.forte.core.EvalException
import org.cikit.forte.parser.ParsedTemplate

class ControlAssertFails : ControlTag {
    override suspend fun invoke(
        ctx: Context.Builder<*>,
        template: ParsedTemplate,
        branches: List<Branch>
    ) {
        val branch = branches.single()
        try {
            ctx.scope().evalNodes(template, branch.body)
        } catch (ex: Exception) {
            ctx.emitValue(ex.toString())
            return
        }
        throw EvalException(
            branch.body.first(),
            "expected failure"
        )
    }
}
