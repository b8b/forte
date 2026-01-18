package org.cikit.forte.lib.core

import org.cikit.forte.core.*
import org.cikit.forte.parser.ParsedTemplate

class ControlExtends: ControlTag {
    override suspend fun invoke(
        ctx: Context.Builder<*>,
        template: ParsedTemplate,
        branches: List<Branch>
    ) {
        // {% extends %} has been transformed into a control
        // with all regular commands in the first branch and a
        // separate branch for each block
        val branch = branches.first()
        val fileExpr = branch.args.getValue("file")
        val fileStr = ctx.evalExpression(fileExpr) as CharSequence
        val file = UPath(fileStr.concatToString(), DecodeUrlPath)

        val blocks = mutableMapOf<String, ParsedTemplate>()
        for (i in 1 until branches.size) {
            val block = branches[i]
            val blockName = ctx.evalExpression(
                block.args.getValue("blockName")
            ) as String
            blocks[blockName] = ParsedTemplate(
                input = template.input,
                path = template.path,
                nodes = block.body
            )
        }

        val embedScope = ctx.scope().discard()
        embedScope.evalNodes(template, branch.body)
        embedScope.importTemplate(
            listOf(file),
            template.path
        ) { _, importedCtx ->
            for (superBlock in importedCtx.result) {
                if (superBlock is ControlBlock.RenderedBlock) {
                    val derivedBlock = blocks[superBlock.blockName]
                    if (derivedBlock == null) {
                        ctx.emitValue(superBlock)
                    } else {
                        val renderedValue = embedScope
                            .scope()
                            .renderToString()
                            .defineFunction("super") { args ->
                                args.requireEmpty()
                                superBlock
                            }
                            .evalTemplate(derivedBlock)
                            .result
                        val renderedBlock = ControlBlock.RenderedBlock(
                            superBlock.blockName,
                            renderedValue
                        )
                        ctx.emitValue(renderedBlock)
                    }
                } else {
                    ctx.emitValue(superBlock)
                }
            }
        }
    }
}
