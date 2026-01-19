package org.cikit.forte.lib.core

import org.cikit.forte.core.*
import org.cikit.forte.parser.Node
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

        val blocks = mutableMapOf<String, List<Node>>()
        for (i in 1 until branches.size) {
            val block = branches[i]
            val blockName = ctx.evalExpression(
                block.args.getValue("blockName")
            ) as String
            blocks[blockName] = block.body
        }

        ctx.discard().evalNodes(template, branch.body)

        ctx.importTemplate(listOf(file), template.path) { _, importedCtx ->
            for (superBlock in importedCtx.result) {
                when (superBlock) {
                    is ControlBlock.RenderedBlock -> {
                        val derivedBlock = blocks[superBlock.blockName]
                        if (derivedBlock == null) {
                            ctx.emitValue(superBlock)
                        } else {
                            val renderedValue = ctx.scope()
                                .renderToString()
                                .defineFunction("super") { args ->
                                    args.requireEmpty()
                                    superBlock
                                }
                                .evalNodes(template, derivedBlock)
                                .result
                            val renderedBlock = ControlBlock.RenderedBlock(
                                superBlock.blockName,
                                renderedValue
                            )
                            ctx.emitValue(renderedBlock)
                        }
                    }

                    is CharSequence -> {
                        ctx.emitValue(superBlock)
                    }

                    else -> {
                        val renderedValue = importedCtx
                            .filterString(superBlock, NamedArgs.Empty)
                        ctx.emitValue(renderedValue)
                    }
                }
            }
        }
    }
}
