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
            val outerScope = ctx.scope()
                .defineMethod(ApplyCycle.KEY, ApplyCycle())
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
                outerScope
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

    private class ApplyCycle : Method {
        companion object {
            val KEY = Context.Key.Apply.create("cycle", Method.OPERATOR)
        }

        override fun invoke(subject: Any?, args: NamedArgs): Any? {
            require(subject is Loop) {
                "cannot call cycle on " + typeName(subject)
            }
            return args.values[subject.index % args.values.size]
        }
    }

    private class Loop(
        val length: Int,
        val index: Int,
        val nextItem: Any?,
        val prevItem: Any?,
    ) : Map<String, Any?> {
        companion object {
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
                "previtem" to Loop::prevItem,
            )
        }

        private val index1: Int
            get() = index + 1

        private val revIndex: Int
            get() = length - index

        private val revIndex0: Int
            get() = length - index - 1

        private val isFirst: Boolean
            get() = index == 0

        private val isLast: Boolean
            get() = index == length - 1

        private val depth: Int
            get() = depth0 + 1

        private val depth0: Int
            get() = 0

        override val size: Int
            get() = attributes.size

        override val keys: Set<String>
            get() = attributes.keys

        override val values: Collection<Any?>
            get() = attributes.map { (_, v) -> v(this) }

        override val entries: Set<Map.Entry<String, Any?>>
            get() = buildSet {
                for ((k, v) in attributes) {
                    add(LoopMapEntry(k, v.invoke(this@Loop)))
                }
            }

        override fun isEmpty(): Boolean = false

        override fun containsKey(key: String): Boolean = key in attributes

        override fun containsValue(value: Any?): Boolean = value in values

        override fun get(key: String): Any? = attributes[key]?.invoke(this)
    }

    private class LoopMapEntry(
        override val key: String,
        override val value: Any?,
    ): Map.Entry<String, Any?> {

        override fun toString(): String {
            return "$key=$value"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || other !is Map.Entry<*, *>) return false

            if (key != other.key) return false
            if (value != other.value) return false

            return true
        }

        override fun hashCode(): Int {
            var result = key.hashCode()
            result = 31 * result + (value?.hashCode() ?: 0)
            return result
        }
    }
}
