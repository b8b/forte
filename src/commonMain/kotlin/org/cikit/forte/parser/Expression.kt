package org.cikit.forte.parser

import org.cikit.forte.eval.Operation
import org.cikit.forte.eval.UNCOMPILED_EXPRESSION

sealed class Expression(
    val operations: List<Operation> = UNCOMPILED_EXPRESSION
) {
    abstract val children: Iterable<Expression>

    class NamedArgs(
        val names: List<String> = emptyList(),
        val values: List<Expression> = emptyList()
    ) {
        override fun toString(): String {
            val nameOff = values.size - names.size
            require(nameOff >= 0)
            return buildString {
                append("NamedArgs(")
                for (i in values.indices) {
                    if (i > 0) {
                        append(", ")
                    }
                    if (i - nameOff >= 0) {
                        append(names[i - nameOff])
                        append(" = ")
                    }
                    append(values[i])
                }
                append(")")
            }
        }
    }

    class SubExpression(
        val content: Expression,
        operations: List<Operation> = UNCOMPILED_EXPRESSION
    ) : Expression(operations) {
        override val children: Iterable<Expression>
            get() = listOf(content)
    }

    class Malformed(val tokens: List<Token>) : Expression() {
        override val children: Iterable<Expression> get() = emptyList()
    }

    class Variable(
        val first: Token,
        val name: String,
        operations: List<Operation> = UNCOMPILED_EXPRESSION
    ) : Expression(operations) {
        override val children: Iterable<Expression>
            get() = emptyList()

        override fun toString(): String {
            return "Var($name)"
        }
    }

    class NullLiteral(
        val token: Token,
        operations: List<Operation> = UNCOMPILED_EXPRESSION
    ) : Expression(operations) {
        override val children: Iterable<Expression>
            get() = emptyList()

        override fun toString(): String {
            return "Literal(null)"
        }
    }

    class BooleanLiteral(
        val token: Token,
        val value: Boolean,
        operations: List<Operation> = UNCOMPILED_EXPRESSION
    ) : Expression(operations) {
        override val children: Iterable<Expression>
            get() = emptyList()

        override fun toString(): String {
            return "Literal($value)"
        }
    }

    class NumericLiteral(
        val first: Token,
        val last: Token,
        val value: Number,
        operations: List<Operation> = UNCOMPILED_EXPRESSION
    ) : Expression(operations) {
        override val children: Iterable<Expression>
            get() = emptyList()

        override fun toString(): String {
            return "Literal($value)"
        }
    }

    class StringLiteral(
        val first: Token,
        val last: Token,
        val value: String,
        operations: List<Operation> = UNCOMPILED_EXPRESSION
    ) : Expression(operations) {
        override val children: Iterable<Expression>
            get() = emptyList()

        override fun toString(): String {
            return "Literal('$value')"
        }
    }

    class StringInterpolation(
        override val children: List<Expression>,
        operations: List<Operation> = UNCOMPILED_EXPRESSION
    ) : Expression(operations) {
        override fun toString(): String {
            return "String(...)"
        }
    }

    class ArrayLiteral(
        val first: Token,
        val last: Token,
        override val children: List<Expression>,
        operations: List<Operation> = UNCOMPILED_EXPRESSION
    ) : Expression(operations) {
        override fun toString(): String {
            return "Literal([...])"
        }
    }

    class ObjectLiteral(
        val first: Token,
        val last: Token,
        val pairs: List<Pair<Expression, Expression>>,
        operations: List<Operation> = UNCOMPILED_EXPRESSION
    ) : Expression(operations) {
        override val children: Iterable<Expression>
            get() = pairs.flatMap { (k, v) -> listOf(k, v) }

        override fun toString(): String {
            return "Literal({...})"
        }
    }

    class Access(
        val first: Token,
        val last: Token,
        val left: Expression,
        val name: String,
        operations: List<Operation> = UNCOMPILED_EXPRESSION
    ) : Expression(operations) {
        override val children: Iterable<Expression>
            get() = listOf(left)

        override fun toString(): String {
            return "Access($left . $name)"
        }
    }

    class CompAccess(
        val first: Token,
        val left: Expression,
        val right: Expression,
        operations: List<Operation> = UNCOMPILED_EXPRESSION
    ) : Expression(operations) {
        override val children: Iterable<Expression>
            get() = listOf(left, right)

        override fun toString(): String {
            return "CompAccess($left [ $right ])"
        }
    }

    class FunctionCall(
        val first: Token,
        val name: String,
        val args: NamedArgs,
        operations: List<Operation> = UNCOMPILED_EXPRESSION
    ) : Expression(operations) {
        override val children: Iterable<Expression>
            get() = args.values

        override fun toString(): String {
            return "Call($name ...)"
        }
    }

    class UnOp(
        val tokens: List<Token>,
        val decl: Declarations.UnOp,
        val right: Expression,
        operations: List<Operation> = UNCOMPILED_EXPRESSION
    ) : Expression(operations) {
        override val children: Iterable<Expression>
            get() = listOf(right)

        override fun toString(): String {
            return "Op(`${decl.name}` $right)"
        }
    }

    class BinOp(
        val tokens: List<Token>,
        val decl: Declarations.BinOp,
        val left: Expression,
        val right: Expression,
        operations: List<Operation> = UNCOMPILED_EXPRESSION
    ) : Expression(operations) {
        override val children: Iterable<Expression>
            get() = listOf(left, right)

        override fun toString(): String {
            return "Op($left `${decl.name}` $right)"
        }
    }

    class TransformOp(
        val tokens: List<Token>,
        val decl: Declarations.TransformOp,
        val left: Expression,
        val name: String,
        val args: NamedArgs,
        operations: List<Operation> = UNCOMPILED_EXPRESSION
    ) : Expression(operations) {
        override val children: Iterable<Expression>
            get() = listOf(left) + args.values

        override fun toString(): String {
            return "Transform($left `${decl.name}` $name(...))"
        }
    }

    class InvokeOp(
        val first: Token,
        val last: Token,
        val left: Expression,
        val args: NamedArgs,
        operations: List<Operation> = UNCOMPILED_EXPRESSION
    ) : Expression(operations) {
        override val children: Iterable<Expression>
            get() = listOf(left) + args.values

        override fun toString(): String {
            return "Invoke($left)"
        }
    }
}
