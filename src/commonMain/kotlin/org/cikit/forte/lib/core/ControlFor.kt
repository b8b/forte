package org.cikit.forte.lib.core

import org.cikit.forte.core.*
import org.cikit.forte.core.Context.Key
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
        val varNames = ctx.evalExpression(varNamesExpr) as List<*>
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
            val loop = Loop(
                length = size,
                index = 0,
                nextItem = null,
                prevItem = null
            )
            val outerScope = ctx.scope()
                .setVar("loop", loop)
                .defineMethod(Loop.KEY_CYCLE, loop)
            val listIt = finalList.iterator()
            var lastItem = listIt.next()
            while (true) {
                val isLast = !listIt.hasNext()
                if (isLast) {
                    loop.nextItem = null
                } else {
                    loop.nextItem = listIt.next()
                }
                outerScope
                    .scope()
                    .setVars(*unpackList(varNames, lastItem))
                    .evalNodes(template, cmd.body)
                if (isLast) {
                    break
                }
                loop.prevItem = lastItem
                lastItem = loop.nextItem
                loop.index++
            }
        } else {
            val cmdElse = branches.getOrNull(1) ?: return
            require(cmdElse.name == "else") {
                "unexpected command: $cmdElse"
            }
            ctx.evalNodes(template, cmdElse.body)
        }
    }

    private class Loop(
        val length: Int,
        var index: Int,
        var nextItem: Any?,
        var prevItem: Any?,
    ) : Method, Map<String, Any?> {
        companion object {
            val KEY_CYCLE = Key.Apply.create("cycle", Method.OPERATOR)
            val attributes = setOf(
                "length", "index", "index0", "revindex", "revindex0",
                "first", "last", "depth", "depth0", "nextitem", "previtem"
            )
        }

        override fun invoke(subject: Any?, args: NamedArgs): Any? {
            require(subject === this) {
                "cannot call cycle on " +
                        typeName(subject)
            }
            return args.values[index % args.values.size]
        }

        override val size: Int
            get() = attributes.size

        override val keys: Set<String>
            get() = attributes

        override val values: Collection<Any?>
            get() = listOf(
                length, index + 1, index, length - index, length - index - 1,
                index == 0, index == length - 1, 1, 0, nextItem, prevItem
            )

        override val entries: Set<Map.Entry<String, Any?>>
            get() = attributes.associateWith { this[it] }.entries

        override fun isEmpty(): Boolean = false

        override fun containsKey(key: String): Boolean = key in attributes

        override fun containsValue(value: Any?): Boolean = value in values

        override fun get(key: String): Any? = when (key) {
            "index" -> index + 1
            "index0" -> index
            "revindex" -> length - index
            "revindex0" -> length - index - 1
            "first" -> index == 0
            "last" -> index == length - 1
            "depth" -> 1
            "depth0" -> 0
            "nextitem" -> if (index == length - 1) {
                Undefined("key 'nextitem' is not defined")
            } else {
                nextItem
            }
            "previtem" -> if (index == 0) {
                Undefined("key 'previtem' is not defined")
            } else {
                prevItem
            }

            else -> null
        }
    }
}
