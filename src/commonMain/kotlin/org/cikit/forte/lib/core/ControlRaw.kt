package org.cikit.forte.lib.core

import org.cikit.forte.core.Branch
import org.cikit.forte.core.Context
import org.cikit.forte.core.ControlTag
import org.cikit.forte.parser.ParsedTemplate

class ControlRaw : ControlTag {
    override suspend fun invoke(
        ctx: Context.Builder<*>,
        template: ParsedTemplate,
        branches: List<Branch>
    ) {
        val cmd = branches.single()
        ctx.scope().evalNodes(template, cmd.body)
    }
}
