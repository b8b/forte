package org.cikit.forte.lib.core

import org.cikit.forte.core.*
import org.cikit.forte.parser.ParsedTemplate

class ControlFor : ControlTag {
    override suspend fun invoke(
        ctx: Context.Builder<*>,
        template: ParsedTemplate,
        branches: List<Branch>
    ) {
        val cmd = branches.first()
        val varNamesExpr = cmd.args.getValue("varNames")
        val listValue = cmd.args.getValue("listValue")
        val recursive = cmd.args["recursive"]
        val condition = cmd.args["condition"]
        val varNames = buildList {
            val varNames = ctx.evalExpression(varNamesExpr)
            varNames as List<*>
            for (varName in varNames) {
                add(varName as String)
            }
        }
        val list = ctx.evalExpression(listValue)
        if (recursive != null &&
            ctx.evalExpression(recursive) == true)
        {
            throw IllegalArgumentException(
                "recursive in for loop is not implemented"
            )
        }
        val finalList = when {
            list !is Iterable<*> -> {
                throw IllegalArgumentException(
                    "invalid type '${typeName(list)}' for " +
                            "arg 'listValue': " +
                            "expected 'Iterable<*>'"
                )
            }
            condition != null -> {
                val scope = ctx.scope()
                list.filter { item ->
                    scope
                        .setVars(*unpackList(varNames, item))
                        .evalExpression(condition) == true
                }
            }
            list is Collection<*> -> list
            else -> list.toList()
        }
        val size = finalList.size
        if (size > 0) {
            var thisIndex = 0
            val loop = mutableMapOf<String, Any?>(
                "length" to size,
                "index" to thisIndex + 1,
                "index0" to thisIndex,
                "revindex" to size - thisIndex,
                "revindex0" to size - thisIndex - 1,
                "first" to true,
                "last" to false,
                "depth" to 1,
                "depth0" to 0,
                //"changed" preprocessor macro (unsupported)
            )
            val listIt = finalList.iterator()
            var lastItem = listIt.next()
            while (true) {
                val isLast = !listIt.hasNext()
                if (isLast) {
                    loop["last"] = true
                    loop.remove("nextitem")
                } else {
                    loop["nextitem"] = listIt.next()
                }
                val thisLoop = loop.toMap()
                ctx.scope()
                    .setVar("loop", thisLoop)
                    .setVars(*unpackList(varNames, lastItem))
                    .defineMethod(
                        "cycle",
                        CycleMethod(thisLoop, thisIndex)
                    )
                    .evalNodes(template, cmd.body)
                if (isLast) {
                    break
                }
                loop["previtem"] = lastItem
                lastItem = loop["nextitem"]
                thisIndex++
                loop["index"] = thisIndex + 1
                loop["index0"] = thisIndex
                loop["revindex"] = size - thisIndex
                loop["revindex0"] = size - thisIndex - 1
            }
        } else {
            val cmdElse = branches.getOrNull(1) ?: return
            require(cmdElse.name == "else") {
                "unexpected command: $cmdElse"
            }
            ctx.evalNodes(template, cmdElse.body)
        }
    }

    private class CycleMethod(
        val thisLoop: Map<String, Any?>,
        val thisIndex: Int
    ) : Method {
        override fun invoke(subject: Any?, args: NamedArgs): Any? {
            require(subject === thisLoop) {
                "cannot call cycle on " +
                        typeName(subject)
            }
            return args.values[thisIndex % args.values.size]
        }
    }
}
