package org.cikit.forte.parser

val defaultDeclaredCommands = listOf(
    Declarations.Command("set") {
        val variable = expect<Token.Identifier>()
        args["varName"] = Expression.StringLiteral(
            variable,
            variable,
            input.substring(variable.first..variable.last)
        )
        expect<Token.Assign>()
        args["value"] = parseExpression()
    },
    Declarations.Command(
        name = "if",
        endAliases = setOf("endif"),
        branchAliases = setOf("else", "elif", "elseif"),
    ) {
        if (name.endsWith("if") && name != "endif") {
            args["condition"] = parseExpression()
        }
    },
    Declarations.Command(
        name = "for",
        endAliases = setOf("endfor"),
        branchAliases = setOf("else")
    ) {
        if (name == "for") {
            val vars = mutableListOf<Expression.StringLiteral>()
            while (true) {
                val variable = expect<Token.Identifier>()
                vars += Expression.StringLiteral(
                    variable,
                    variable,
                    input.substring(variable.first..variable.last)
                )
                when (val next = tokenizer.peek(skipSpace = true)) {
                    is Token.Comma -> tokenizer.consume(next)
                    else -> break
                }
            }
            args["varNames"] = Expression.ArrayLiteral(
                vars.first().first,
                vars.last().last,
                vars.toList()
            )
            expect<Token.Identifier>("in", skipSpace = true)
            args["listValue"] = parseExpression()
            while (true) {
                val t = tokenizer.peek(skipSpace = true)
                if (t !is Token.Identifier) {
                    break
                }
                when (input.substring(t.first..t.last)) {
                    "recursive" -> {
                        tokenizer.consume(t)
                        args["recursive"] = Expression.BooleanLiteral(t, true)
                    }

                    "if" -> {
                        tokenizer.consume(t)
                        args["condition"] = parseExpression()
                        break
                    }

                    else -> error("unrecognized option in for loop: $t")
                }
            }
        }
    },
    Declarations.Command(
        name = "macro",
        endAliases = setOf("endmacro")
    ) {
        if (name == "macro") {
            val name = expect<Token.Identifier>()
            args["name"] = Expression.StringLiteral(
                name,
                name,
                input.substring(name.first..name.last)
            )
            val lPar = tokenizer.peek(skipSpace = true)
                    as? Token.LPar ?: return@Command
            tokenizer.consume(lPar)
            val argNames = mutableListOf<Expression.StringLiteral>()
            val argDefaults = mutableListOf<Pair<Expression, Expression>>()

            var argName = tokenizer.tokenize(skipSpace = true)

            while (argName is Token.Identifier) {
                val argNameExpression = Expression.StringLiteral(
                    argName,
                    argName,
                    input.substring(argName.first..argName.last)
                )
                argNames += argNameExpression
                val next = tokenizer.peek(skipSpace = true)
                if (next is Token.Assign) {
                    tokenizer.consume(next)
                    argDefaults += argNameExpression to parseExpression()
                }
                argName = tokenizer.tokenize(skipSpace = true)
                if (argName is Token.Comma) {
                    argName = tokenizer.tokenize(skipSpace = true)
                }
            }

            val rPar = argName as? Token.RPar
                ?: error("expected ')', found: $argName")
            args["argNames"] = Expression.ArrayLiteral(
                lPar, rPar, argNames.toList()
            )
            args["argDefaults"] = Expression.ObjectLiteral(
                lPar, rPar, argDefaults.toList()
            )
        }
    }
)

val defaultDeclaredUnaryOperations = listOf(
    Declarations.UnOp(99, "not", "!"),
    Declarations.UnOp(99, "+", name = "plus"),
    Declarations.UnOp(99, "-", name = "minus"),
)

val defaultDeclaredOperations = listOf(
    Declarations.TransformOp(90, "|", name = "pipe", left = true),

    Declarations.BinOp(80, "**", name = "pow", right = true),

    Declarations.BinOp(60, "*", name = "mul", left = true),
    Declarations.BinOp(60, "/", name = "div", left = true),
    Declarations.BinOp(60, "%", name = "rem", left = true),

    // compat with jinja
    Declarations.BinOp(60, "//", name = "tdiv", left = true),
    Declarations.BinOp(60, "~", name = "concat", left = true),

    // compat with pebble
    Declarations.BinOp(55, "..", name = "range", left = true),

    Declarations.BinOp(50, "+", name = "plus", left = true),
    Declarations.BinOp(50, "-", name = "minus", left = true),

    Declarations.BinOp(40, "in"),
    Declarations.BinOp(40, "not in", name = "not_in"),

    Declarations.TransformOp(40, "is", name = "is"),
    Declarations.TransformOp(40, "is not", name = "is_not"),

    Declarations.BinOp(40, "<", name = "lt"),
    Declarations.BinOp(40, "<=", name = "le"),
    Declarations.BinOp(40, ">", name = "gt"),
    Declarations.BinOp(40, ">=", name = "ge"),

    Declarations.BinOp(30, "==", name = "eq"),
    Declarations.BinOp(30, "!=", name = "ne"),

    Declarations.BinOp(20, "and", "&&", left = true),
    Declarations.BinOp(20, "or", "||", left = true),
)

val defaultDeclarations = listOf(
    *defaultDeclaredCommands.toTypedArray(),
    *defaultDeclaredOperations.toTypedArray(),
    *defaultDeclaredUnaryOperations.toTypedArray()
)

sealed class Declarations {
    class Command(
        val name: String,
        val endAliases: Set<String> = emptySet(),
        val branchAliases: Set<String> = emptySet(),
        val parser: (CommandArgBuilder.() -> Unit)? = null
    ) : Declarations() {
        override fun toString(): String = when {
            endAliases.isEmpty() -> "(%$name)"
            branchAliases.isEmpty() -> "(%$name .. $endAliases)"
            else -> "(%$name .. $branchAliases .. $endAliases)"
        }
    }

    class TransformOp(
        precedence: Int,
        aliases: Set<String>,
        name: String = aliases.first(),
        left: Boolean = false,
        right: Boolean = false
    ) : BinOp(precedence, aliases, name, left, right) {
        constructor(
            precedence: Int,
            vararg aliases: String,
            name: String = aliases.first(),
            left: Boolean = false,
            right: Boolean = false
        ) : this(precedence, aliases.toSet(), name, left, right)

        override fun toString(): String {
            return "XOp($precedence, `$name`)"
        }
    }

    open class BinOp(
        val precedence: Int,
        val aliases: Set<String>,
        val name: String = aliases.first(),
        val left: Boolean = false,
        val right: Boolean = false
    ) : Declarations() {
        constructor(
            precedence: Int,
            vararg aliases: String,
            name: String = aliases.first(),
            left: Boolean = false,
            right: Boolean = false
        ) : this(precedence, aliases.toSet(), name, left, right)

        override fun toString(): String {
            return "BinOp($precedence, `$name`)"
        }
    }

    class UnOp(
        val precedence: Int,
        val aliases: Set<String>,
        val name: String = aliases.first()
    ) : Declarations() {
        constructor(
            precedence: Int,
            vararg aliases: String,
            name: String = aliases.first()
        ) : this(precedence, aliases.toSet(), name)

        override fun toString(): String {
            return "UnOp($precedence, `$name`)"
        }
    }

    companion object {
        fun <T: Declarations> mapOf(declaration: Iterable<T>): Map<String, T> {
            val result = mutableMapOf<String, T>()
            for (item in declaration) {
                val aliases = when (item) {
                    is UnOp -> item.aliases
                    is BinOp -> item.aliases
                    is Command -> setOf(item.name)
                    else -> error("invalid declaration")
                }
                for (alias in aliases) {
                    result[alias] = item
                }
            }
            return result.toMap()
        }
    }
}

inline fun <reified T: Token> ExpressionParser.expect(
    skipSpace: Boolean = true
): T {
    val t = tokenizer.tokenize(skipSpace = skipSpace)
    if (t !is T) {
        throw ParseException(tokenizer, t, "expected ${T::class}")
    }
    return t
}

inline fun <reified T: Token> ExpressionParser.expect(
    value: String,
    skipSpace: Boolean = true
) {
    val t = expect<T>(skipSpace = skipSpace)
    val actual = tokenizer.input.substring(t.first .. t.last)
    if (value != actual) {
        throw ParseException(
            tokenizer,
            t,
            "expected '$value', actual '$actual'"
        )
    }
}

inline fun <reified N: Expression> ExpressionParser.expect(): N {
    val n = parsePrimaryOrNull() ?: throw ParseException(
        tokenizer,
        tokenizer.peek(),
        "expected ${N::class}"
    )
    if (n !is N) {
        throw ParseException(tokenizer, n, "expected ${N::class}")
    }
    return n
}

fun ExpressionParser.expect(value: Boolean): Expression.BooleanLiteral {
    val n = this.expect<Expression.BooleanLiteral>()
    if (n.value == value) {
        return n
    }
    throw ParseException(
        tokenizer,
        n,
        "expected boolean literal $value, actual: ${n.value}"
    )
}

fun ExpressionParser.expect(value: Number): Expression.NumericLiteral {
    val n = this.expect<Expression.NumericLiteral>()
    if (n.value == value) {
        return n
    }
    throw ParseException(
        tokenizer,
        n,
        "expected numeric literal $value, actual: ${n.value}"
    )
}

fun ExpressionParser.expect(value: String): Expression.StringLiteral {
    val n = this.expect<Expression.StringLiteral>()
    if (n.value == value) {
        return n
    }
    throw ParseException(
        tokenizer,
        n,
        "expected string literal '$value', actual: $n"
    )
}
