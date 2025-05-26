package org.cikit.forte.parser

import org.cikit.forte.core.Context
import org.cikit.forte.eval.UNCOMPILED_NODE

private typealias Execute = suspend (Context.Builder<*>) -> Unit

sealed class Node(
    val execute: Execute? = UNCOMPILED_NODE
) {

    class Comment(
        val first: Token,
        val content: Token,
        val last: Token,
        execute: Execute? = null
    ) : Node(execute) {
        override fun toString(): String {
            return "Comment(${first.first}..${last.last}: $content)"
        }
    }

    class Command(
        val first: Token,
        val name: String,
        val content: Map<String, Expression>,
        val last: Token,
        execute: Execute? = UNCOMPILED_NODE
    ) : Node(execute) {
        override fun toString(): String {
            return "Command(${first.first}..${last.last}: %$name $content)"
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
        execute: Execute? = UNCOMPILED_NODE
    ) : Node(execute) {
        override fun toString(): String = buildString {
            append("Control(")
            append(first.first)
            append("..")
            append((branches.lastOrNull() ?: first).last.last)
            append(": ")
            append(branches.joinToString(", "))
            append(")")
        }
    }

    class Text(
        val content: Token,
        val trimLeft: Boolean = false,
        val trimRight: Boolean = false,
        execute: Execute? = UNCOMPILED_NODE
    ) : Node(execute) {
        override fun toString(): String {
            return "Text(${content.first}..${content.last})"
        }
    }

    class Emit(
        val first: Token,
        val content: Expression,
        val last: Token,
        execute: Execute? = UNCOMPILED_NODE
    ) : Node(execute) {
        override fun toString(): String {
            return "Emit(${first.first}..${last.last}: $content)"
        }
    }

}
