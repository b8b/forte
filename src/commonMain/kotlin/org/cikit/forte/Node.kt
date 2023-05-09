package org.cikit.forte

sealed class Node {

    class Comment(val first: Token, val content: Token, val last: Token) : Node() {
        override fun toString(): String {
            return "Comment(${first.first}..${last.last}: $content)"
        }
    }

    class Command(val first: Token, val name: String, val content: List<Pair<String, Expression>>, val last: Token) : Node() {
        override fun toString(): String {
            return "Command(${first.first}..${last.last}: %$name ${content.joinToString(", ")})"
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


    sealed class Expression : Node() {
        abstract val children: Iterable<Expression>
    }

    class SubExpression(val content: Expression) : Expression() {
        override val children: Iterable<Expression> get() = listOf(content)
    }

    object Empty : Expression() {
        override val children: Iterable<Expression> get() = emptyList()
    }

    class Malformed(val tokens: List<Token>) : Expression() {
        override val children: Iterable<Expression> get() = emptyList()
    }

    class Variable(val first: Token, val name: String) : Expression() {
        override val children: Iterable<Expression> get() = emptyList()
        override fun toString(): String {
            return "Var($name)"
        }
    }

    class NullLiteral(val token: Token) : Expression() {
        override val children: Iterable<Expression> get() = emptyList()
        override fun toString(): String {
            return "Literal(null)"
        }
    }

    class BooleanLiteral(val token: Token, val value: Boolean) : Expression() {
        override val children: Iterable<Expression> get() = emptyList()
        override fun toString(): String {
            return "Literal($value)"
        }
    }

    class NumericLiteral(val first: Token, val last: Token, val value: Number) : Expression() {
        override val children: Iterable<Expression> get() = emptyList()
        override fun toString(): String {
            return "Literal($value)"
        }
    }

    class StringLiteral(val first: Token, val last: Token, val value: String) : Expression() {
        override val children: Iterable<Expression> get() = emptyList()
        override fun toString(): String {
            return "Literal('$value')"
        }
    }

    class StringInterpolation(override val children: List<Expression>) : Expression() {
        override fun toString(): String {
            return "String(...)"
        }
    }

    class ArrayLiteral(val first: Token, val last: Token, override val children: List<Expression>) : Expression() {
        override fun toString(): String {
            return "Literal([...])"
        }
    }

    class ObjectLiteral(val first: Token, val last: Token, val pairs: List<Pair<Expression, Expression>>) : Expression() {
        override val children: Iterable<Expression> get() = pairs.flatMap { (k, v) -> listOf(k, v) }
        override fun toString(): String {
            return "Literal({...})"
        }
    }

    class Access(val first: Token, val last: Token, val left: Expression, val name: String) : Expression() {
        override val children: Iterable<Expression> get() = listOf(left)
        override fun toString(): String {
            return "Access($left . $name)"
        }
    }

    class CompAccess(val first: Token, val left: Expression, val right: Expression) : Expression() {
        override val children: Iterable<Expression> get() = listOf(left, right)
        override fun toString(): String {
            return "CompAccess($left [ $right ])"
        }
    }

    class FunctionCall(val first: Token, val name: String, val args: List<Pair<String, Expression>>) : Expression() {
        override val children: Iterable<Expression> get() = args.map { (_, n) -> n }
        override fun toString(): String {
            return "Call($name(...))"
        }
    }

    class MethodCall(val left: Expression, val args: List<Pair<String, Expression>>) : Expression() {
        override val children: Iterable<Expression> get() = listOf(left) + args.map { (_, n) -> n }
        override fun toString(): String {
            return "Call(invoke(...) on $left)"
        }
    }

    class ExtensionCall(val first: Token, val left: Expression, val name: String, val args: List<Pair<String, Expression>>) : Expression() {
        override val children: Iterable<Expression> get() = listOf(left) + args.map { (_, n) -> n }
        override fun toString(): String {
            return "Call($name(...) on $left)"
        }
    }

    class UnOp(val tokens: List<Token>, val decl: Declarations.UnOp, val right: Expression) : Expression() {
        override val children: Iterable<Expression> get() = listOf(right)
        override fun toString(): String {
            return "Op(`${decl.name}` $right)"
        }
    }

    class BinOp(val tokens: List<Token>, val decl: Declarations.BinOp, val left: Expression, val right: Expression) : Expression() {
        override val children: Iterable<Expression> get() = listOf(left, right)
        override fun toString(): String {
            return "Op($left `${decl.name}` $right)"
        }
    }
}