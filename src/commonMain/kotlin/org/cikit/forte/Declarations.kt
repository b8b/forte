package org.cikit.forte

val defaultDeclaredCommands = listOf(
    Declarations.Command("set") {
        val variable = expect<Token.Identifier>()
        args["varName"] = Node.StringLiteral(
            variable,
            variable,
            input.substring(variable.first .. variable.last)
        )
        expect<Token.Operator>("=")
        args["value"] = parseExpression()
    },
    Declarations.Command(
        name = "if",
        endAliases = setOf("endif"),
        branchAliases = setOf("else", "elif", "elseif"),
    ) {
        if (name.endsWith("if")) {
            args["condition"] = parseExpression()
        }
    },
    Declarations.Command(
        name = "for",
        endAliases = setOf("endfor"),
        branchAliases = setOf("else")
    ) {
        if (name == "for") {
            val vars = mutableListOf<Node.StringLiteral>()
            while (true) {
                val variable = expect<Token.Identifier>()
                vars += Node.StringLiteral(
                    variable,
                    variable,
                    input.substring(variable.first .. variable.last)
                )
                when (val next = tokenizer.peek(skipSpace = true)) {
                    is Token.Comma -> tokenizer.consume(next)
                    else -> break
                }
            }
            args["varNames"] = Node.ArrayLiteral(
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
                when (input.substring(t.first .. t.last)) {
                    "recursive" -> {
                        tokenizer.consume(t)
                        args["recursive"] = Node.BooleanLiteral(t, true)
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
            args["name"] = Node.StringLiteral(
                name,
                name,
                input.substring(name.first .. name.last)
            )
            val lPar = expect<Token.LPar>()
            val rPar: Token.RPar
            val macroArgs =
                mutableListOf<Pair<Node.Expression, Node.Expression>>()
            while (true) {
                val argName = expect<Token.Identifier>()
                val next = tokenizer.peek(skipSpace = true)
                val argDefault = if (next is Token.Operator &&
                    input.substring(next.first .. next.last) == "=") {
                    tokenizer.consume(next)
                    parsePrimary()
                } else {
                    Node.NullLiteral(next)
                }
                macroArgs.add(
                    Node.StringLiteral(
                        argName,
                        argName,
                        input.substring(argName.first .. argName.last)
                    ) to argDefault
                )
                when (val t = tokenizer.tokenize(skipSpace = true)) {
                    is Token.RPar -> {
                        rPar = t
                        break
                    }
                    is Token.Comma -> continue
                    else -> error("expected ',' or ')', found: $t")
                }
            }
            args["args"] = Node.ObjectLiteral(lPar, rPar, macroArgs.toList())
        }
    }
)

val defaultDeclaredUnaryOperations = listOf(
    Declarations.UnOp(99, "not", "!"),
    Declarations.UnOp(99, "+", name = "plus"),
    Declarations.UnOp(99, "-", name = "minus"),
)

val defaultDeclaredOperations = listOf(
    Declarations.BinOp(90, "|",  name = "pipe", left = true),

    Declarations.BinOp(80, "**", name = "pow", right = true),

    Declarations.BinOp(60, "*",  name = "mul", left = true),
    Declarations.BinOp(60, "/",  name = "div", left = true),
    Declarations.BinOp(60, "%",  name = "rem", left = true),

    // compat with jinja
    Declarations.BinOp(60, "//", name = "tdiv", left = true),
    Declarations.BinOp(60, "~", name = "concat", left = true),

    // compat with pebble
    Declarations.BinOp(55, "..", name = "range", left = true),

    Declarations.BinOp(50, "+",  name = "plus", left = true),
    Declarations.BinOp(50, "-",  name = "minus", left = true),

    Declarations.BinOp(40, "in"),
    Declarations.BinOp(40, "not in", name = "not_in"),

    Declarations.BinOp(40, "is",     name = "is"),
    Declarations.BinOp(40, "is not", name = "is_not"),

    Declarations.BinOp(40, "<",  name = "lt"),
    Declarations.BinOp(40, "<=", name = "le"),
    Declarations.BinOp(40, ">",  name = "gt"),
    Declarations.BinOp(40, ">=", name = "ge"),

    Declarations.BinOp(30, "==", name = "eq"),
    Declarations.BinOp(30, "!=", name = "ne"),

    Declarations.BinOp(20, "and", "&&", left = true),
    Declarations.BinOp(20, "or",  "||", left = true),

    Declarations.BinOp(10, "=",  name = "assign"),
    Declarations.BinOp( 5, ":",  name = "pair"),
    Declarations.BinOp( 4, ",",  name = "tuple", left = true),
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

    class BinOp(
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
