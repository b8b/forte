package org.cikit.forte.lib.core

import org.cikit.forte.core.Branch
import org.cikit.forte.core.Context
import org.cikit.forte.core.ControlTag
import org.cikit.forte.core.Function
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.TemplateObject
import org.cikit.forte.core.typeName
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
            ctx.evalExpression(recursive) == true
        ) {
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
            var index = 0
            val listIt = finalList.iterator()
            var currentItem = listIt.next()
            var prevItem: Any? = null
            while (true) {
                val isLast = !listIt.hasNext()
                val loop = Loop(
                    length = size,
                    index = index,
                    nextItem = if (isLast) {
                        null
                    } else {
                        listIt.next()
                    },
                    prevItem = prevItem
                )
                ctx
                    .scope()
                    .setVar("loop", loop)
                    .setVars(*unpackList(varNames, currentItem))
                    .evalNodes(template, cmd.body)
                if (isLast) {
                    break
                }
                prevItem = currentItem
                currentItem = loop.nextItem
                index++
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
        length: Int,
        index: Int,
        val nextItem: Any?,
        val prevItem: Any?,
    ) : TemplateObject {
        companion object {
            val CYCLE_KEY = Context.Key.Call("cycle")

            val attributes: Map<String, (Loop) -> Any?> = mapOf(
                "length" to Loop::length,
                "index" to Loop::index1,
                "index0" to Loop::index,
                "revindex" to Loop::revIndex,
                "revindex0" to Loop::revIndex0,
                "first" to Loop::isFirst,
                "last" to Loop::isLast,
                "depth" to Loop::depth,
                "depth0" to Loop::depth0,
                "nextitem" to Loop::nextItem,
                "previtem" to Loop::prevItem
            )
        }

        private val _length = length

        private val _index = index

        private val length: Long
            get() = _length.toLong()

        private val index: Long
            get() = _index.toLong()

        private val index1: Long
            get() = index + 1

        private val revIndex: Long
            get() = length - index

        private val revIndex0: Long
            get() = length - index - 1

        private val isFirst: Boolean
            get() = index == 0L

        private val isLast: Boolean
            get() = index == length - 1

        private val depth: Long
            get() = depth0 + 1L

        private val depth0: Long
            get() = 0

        private val cycle by lazy(::CycleFunction)

        override fun getVar(name: String): Any? = attributes[name]?.invoke(this)

        override fun getFunction(key: Context.Key.Call): Function? {
            if (key == CYCLE_KEY) {
                return cycle
            }
            return null
        }

        private inner class CycleFunction : Function {
            override fun invoke(args: NamedArgs): Any? {
                val size = args.values.size
                require(size > 0) {
                    "missing required arg(s)"
                }
                return args.values[this@Loop._index % size]
            }
        }
    }
}
