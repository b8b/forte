package org.cikit.forte.parser

import kotlinx.coroutines.CancellationException
import org.cikit.forte.core.Context
import org.cikit.forte.core.EvalException
import org.cikit.forte.core.RawString
import org.cikit.forte.core.compile
import org.cikit.forte.internal.UNCOMPILED_EXPRESSION
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

sealed class Node {

    open suspend fun eval(ctx: Context.Builder<*>, template: ParsedTemplate) {
    }

    class Comment(
        val first: Token,
        val content: Token,
        val last: Token
    ) : Node() {
        override fun toString(): String {
            return "Comment(${first.first}..${last.last}: $content)"
        }
    }

    class Command(
        val first: Token,
        val name: String,
        args: Map<String, Expression>,
        val branchAliases: Set<String>,
        val endAliases: Set<String>,
        val last: Token,
        val allowHidden: Boolean = false,
    ) : Node() {
        val args = buildMap(args.size) {
            for ((k, v) in args) {
                if (v.operations === UNCOMPILED_EXPRESSION) {
                    put(k, v.compile())
                } else {
                    put(k, v)
                }
            }
        }

        private val key = Context.Key.Command(name)

        override fun toString(): String {
            return "Command(${first.first}..${last.last}: %$name $args)"
        }

        override suspend fun eval(
            ctx: Context.Builder<*>,
            template: ParsedTemplate
        ) {
            val function = ctx.getCommandTag(key)
                ?: throw EvalException(this, "command '$key' not defined")
            if (!allowHidden && function.isHidden) {
                throw EvalException(this, "command '$key' is hidden")
            }
            try {
                function.invoke(ctx, template, args)
            } catch (ex: EvalException) {
                throw ex
            } catch (ex: Exception) {
                throw EvalException(this, ex.toString(), ex)
            }
        }
    }

    class Branch(
        val first: Command,
        val body: List<Node>,
        val last: Command,
    ) {
        override fun toString(): String = buildString {
            append("Branch(")
            append(first.first)
            append("..")
            append(last.last)
            append(": ")
            append(body.joinToString(", "))
            append(")")
        }
    }

    class Control(
        val first: Branch,
        val branches: List<Branch> = emptyList(),
        val allowHidden: Boolean = false,
    ) : Node() {
        private val key = Context.Key.Control(first.first.name)

        private val compiledBranches = buildList(branches.size + 1) {
            this += org.cikit.forte.core.Branch(
                name = first.first.name,
                args = first.first.args,
                body = first.body.filterNot { node ->
                    node is Comment || (node is Text && node.value.isEmpty())
                }
            )
            for (branch in branches) {
                this += org.cikit.forte.core.Branch(
                    name = branch.first.name,
                    args = branch.first.args,
                    body = branch.body.filterNot { node ->
                        node is Comment ||
                                (node is Text && node.value.isEmpty())
                    }
                )
            }
        }

        fun single(): Branch {
            require(branches.isEmpty()) { "expected single branch" }
            return first
        }

        override fun toString(): String = buildString {
            append("Control(")
            append(first.first)
            append("..")
            append((branches.lastOrNull() ?: first).last.last)
            append(": ")
            append(branches.joinToString(", "))
            append(")")
        }

        override suspend fun eval(
            ctx: Context.Builder<*>,
            template: ParsedTemplate
        ) {
            val function = ctx.getControlTag(key)
                ?: throw EvalException(this, "command '$key' not defined")
            if (!allowHidden && function.isHidden) {
                throw EvalException(this, "command '$key' is hidden")
            }
            try {
                function.invoke(ctx, template, compiledBranches)
            } catch (ex: EvalException) {
                throw ex
            } catch (ex: Exception) {
                throw EvalException(this, ex.toString(), ex)
            }
        }
    }

    class Text(
        input: String,
        val content: Token,
        val trimLeft: Boolean = false,
        val trimRight: Boolean = false,
    ) : Node() {
        val value: RawString = calculateValue(input)

        override fun toString(): String {
            return "Text(${content.first}..${content.last})"
        }

        override suspend fun eval(
            ctx: Context.Builder<*>,
            template: ParsedTemplate
        ) {
            if (value.isNotEmpty()) {
                ctx.emitValue(value)
            }
        }

        private fun calculateValue(input: String): RawString {
            var startIndex = content.first
            var endIndex = content.last
            if (trimLeft) {
                while (startIndex <= endIndex &&
                    input[startIndex].isWhitespace()
                ) {
                    startIndex++
                }
            }
            if (trimRight) {
                while (endIndex >= startIndex &&
                    input[endIndex].isWhitespace()
                ) {
                    endIndex--
                }
            }
            return RawString(input, startIndex, endIndex + 1)
        }
    }

    class Emit(
        val first: Token,
        content: Expression,
        val last: Token,
    ) : Node() {
        val content = if (content.operations === UNCOMPILED_EXPRESSION) {
            content.compile()
        } else {
            content
        }

        override fun toString(): String {
            return "Emit(${first.first}..${last.last}: $content)"
        }

        override suspend fun eval(
            ctx: Context.Builder<*>,
            template: ParsedTemplate
        ) {
            try {
                ctx.emitExpression(content)
            } catch (ex: EvalException) {
                throw ex
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: Exception) {
                throw EvalException(this, ex.toString(), ex)
            }
        }
    }

}
