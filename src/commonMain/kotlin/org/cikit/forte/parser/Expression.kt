package org.cikit.forte.parser

sealed class Expression {
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

        fun read(vararg args: String): List<Expression?> {
            val result = mutableListOf<Expression?>()
            val nameOff = values.size - names.size
            require(nameOff >= 0)
            val valueByName = mutableMapOf<String, Expression>()
            for (i in names.indices) {
                valueByName[names[i]] = values[i + nameOff]
            }
            for (i in args.indices) {
                val name = args[i]
                val (realName, optional) = when (val suffix = name.indexOf('?')) {
                    name.length - 1 -> name.substring(0, suffix) to true
                    else -> name to false
                }
                if (i < nameOff) {
                    if (i >= this.values.size) {
                        if (!optional) {
                            error("missing required argument $realName")
                        }
                        result += null
                        continue
                    }
                    if (valueByName.containsKey(realName)) {
                        error("argument $realName is already passed")
                    }
                    result += this.values[i]
                } else {
                    val v = valueByName.remove(realName)
                    if (v == null) {
                        if (!optional) {
                            error("missing required argument $realName")
                        }
                        result += null
                        continue
                    }
                    result += v
                }
            }
            if (valueByName.isNotEmpty()) {
                error("unknown argument(s): ${valueByName.keys}")
            }
            return result.toList()
        }
    }

    class SubExpression(val content: Expression) : Expression() {
        override val children: Iterable<Expression> get() = listOf(content)
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

    class FunctionCall(val first: Token, val name: String, val args: NamedArgs) : Expression() {
        override val children: Iterable<Expression> get() = args.values
        override fun toString(): String {
            return "Call($name ...)"
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

    class TransformOp(
        val tokens: List<Token>,
        val decl: Declarations.TransformOp,
        val left: Expression,
        val name: String,
        val args: NamedArgs
    ) : Expression() {
        override val children: Iterable<Expression> get() = listOf(left) + args.values
        override fun toString(): String {
            return "Transform($left `${decl.name}` $name(...))"
        }
    }

    class InvokeOp(
        val first: Token,
        val last: Token,
        val left: Expression,
        val args: NamedArgs
    ) : Expression() {
        override val children: Iterable<Expression> get() = listOf(left) + args.values

        override fun toString(): String {
            return "Invoke($left)"
        }
    }
}
