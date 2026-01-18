package org.cikit.forte.parser

import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.indices
import org.cikit.forte.core.Operation
import org.cikit.forte.internal.UNCOMPILED_EXPRESSION

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
        override fun toString() = "SubExpression($content)"
    }

    class Malformed(val tokens: List<Token>) : Expression() {
        override val children: Iterable<Expression> get() = emptyList()
        override fun toString() = "Malformed($tokens)"
    }

    class Variable(
        val first: Token,
        val name: String,
        operations: List<Operation> = UNCOMPILED_EXPRESSION
    ) : Expression(operations) {
        override val children: Iterable<Expression>
            get() = emptyList()
        override fun toString() = "Var($name)"
    }

    class NullLiteral(
        val token: Token,
        operations: List<Operation> = UNCOMPILED_EXPRESSION
    ) : Expression(operations) {
        override val children: Iterable<Expression>
            get() = emptyList()
        override fun toString() = "Literal(null)"
    }

    class BooleanLiteral(
        val token: Token,
        val value: Boolean,
        operations: List<Operation> = UNCOMPILED_EXPRESSION
    ) : Expression(operations) {
        override val children: Iterable<Expression>
            get() = emptyList()
        override fun toString() = "Literal($value)"
    }

    class NumericLiteral(
        val first: Token,
        val last: Token,
        val value: Number,
        operations: List<Operation> = UNCOMPILED_EXPRESSION
    ) : Expression(operations) {
        override val children: Iterable<Expression>
            get() = emptyList()
        override fun toString() = "Literal($value)"
    }

    class StringLiteral(
        val first: Token,
        val last: Token,
        val value: String,
        operations: List<Operation> = UNCOMPILED_EXPRESSION
    ) : Expression(operations) {
        override val children: Iterable<Expression>
            get() = emptyList()
        override fun toString() = "Literal('$value')"
    }

    class ByteStringLiteral(
        val first: Token,
        val last: Token,
        val value: ByteString,
        operations: List<Operation> = UNCOMPILED_EXPRESSION
    ) : Expression(operations) {
        override val children: Iterable<Expression>
            get() = emptyList()

        override fun toString() = buildString {
            append("ByteStringLiteral('")
            for (i in value.indices) {
                val b = value[i].toInt()
                when (val ch = b.toChar()) {
                    '\\' -> append("\\\\")

                    in '\u0021'..'\u007e' -> append(ch)

                    '\u0007' -> append("\\a")
                    '\u0008' -> append("\\b")
                    '\u0009' -> append("\\t")
                    '\u000A' -> append("\\n")
                    '\u000B' -> append("\\v")
                    '\u000C' -> append("\\f")
                    '\u000D' -> append("\\r")

                    else -> {
                        append("\\")
                        val oct = (b and 0xFF).toString(8)
                        if (oct.length < 3) {
                            append("0".repeat(oct.length - 3))
                        }
                        append(oct)
                    }
                }
            }
            append("')")
        }
    }

    class StringInterpolation(
        override val children: List<Expression>,
        operations: List<Operation> = UNCOMPILED_EXPRESSION
    ) : Expression(operations) {
        override fun toString() = "String(...)"
    }

    class ArrayLiteral(
        val first: Token,
        val last: Token,
        override val children: List<Expression>,
        operations: List<Operation> = UNCOMPILED_EXPRESSION
    ) : Expression(operations) {
        override fun toString() = "Literal([...])"
    }

    class ObjectLiteral(
        val first: Token,
        val last: Token,
        val pairs: List<Pair<Expression, Expression>>,
        operations: List<Operation> = UNCOMPILED_EXPRESSION
    ) : Expression(operations) {
        override val children: Iterable<Expression>
            get() = pairs.flatMap { (k, v) -> listOf(k, v) }
        override fun toString() = "Literal({...})"
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
        override fun toString() = "Access($left . $name)"
    }

    class CompAccess(
        val first: Token,
        val left: Expression,
        val right: Expression,
        operations: List<Operation> = UNCOMPILED_EXPRESSION
    ) : Expression(operations) {
        override val children: Iterable<Expression>
            get() = listOf(left, right)
        override fun toString() = "CompAccess($left [ $right ])"
    }

    class SliceAccess(
        val first: Token,
        val left: Expression,
        val args: NamedArgs,
        operations: List<Operation> = UNCOMPILED_EXPRESSION
    ) : Expression(operations) {
        override val children: Iterable<Expression>
            get() = listOf(left) + args.values

        override fun toString(): String {
            return buildString {
                append("SliceAccess(")
                append(left)
                append(" [ ")
                append(args)
                append(" ])")
            }
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
        override fun toString() = "Call($name ...)"
    }

    class UnOp(
        val tokens: List<Token>,
        val decl: Declarations.UnOp,
        val alias: String,
        val right: Expression,
        operations: List<Operation> = UNCOMPILED_EXPRESSION
    ) : Expression(operations) {
        override val children: Iterable<Expression>
            get() = listOf(right)
        override fun toString() = "Op(`$alias` $right)"
    }

    class BinOp(
        val tokens: List<Token>,
        val decl: Declarations.BinOp,
        val alias: String,
        val left: Expression,
        val right: Expression,
        operations: List<Operation> = UNCOMPILED_EXPRESSION
    ) : Expression(operations) {
        override val children: Iterable<Expression>
            get() = listOf(left, right)
        override fun toString() = "Op($left `$alias` $right)"
    }

    class TransformOp(
        val tokens: List<Token>,
        val decl: Declarations.TransformOp,
        val alias: String,
        val left: Expression,
        val name: String,
        val args: NamedArgs,
        operations: List<Operation> = UNCOMPILED_EXPRESSION
    ) : Expression(operations) {
        override val children: Iterable<Expression>
            get() = listOf(left) + args.values
        override fun toString() = "Transform($left `$alias` $name(...))"
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
        override fun toString() = "Invoke($left)"
    }
}
