package org.cikit.forte.parser

sealed class Node {

    class Comment(val first: Token, val content: Token, val last: Token) : Node() {
        override fun toString(): String {
            return "Comment(${first.first}..${last.last}: $content)"
        }
    }

    class Command(val first: Token, val name: String, val content: Map<String, Expression>, val last: Token) : Node() {
        override fun toString(): String {
            return "Command(${first.first}..${last.last}: %$name $content)"
        }
    }

    class Branch(val first: Command, val body: List<Node>, val last: Command) {
        override fun toString(): String {
            return "Branch(${first.first}..${last.last}: ${body.joinToString(", ")})"
        }
    }

    class Control(val first: Branch, val branches: List<Branch> = emptyList()) : Node() {
        override fun toString(): String {
            val firstToken = first.first
            val lastToken = (branches.lastOrNull() ?: first).last.last
            return "Control($firstToken..$lastToken: ${branches.joinToString(", ")})"
        }
    }

    class Text(val content: Token, val trimLeft: Boolean = false, val trimRight: Boolean = false) : Node() {
        override fun toString(): String {
            return "Text(${content.first}..${content.last})"
        }
    }

    class Emit(val first: Token, val content: Expression, val last: Token) : Node() {
        override fun toString(): String {
            return "Emit(${first.first}..${last.last}: $content)"
        }
    }

}