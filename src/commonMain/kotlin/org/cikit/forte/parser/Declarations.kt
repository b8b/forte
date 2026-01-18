package org.cikit.forte.parser

import kotlin.collections.set

val defaultDeclaredCommands = listOf(
    Declarations.Command(
        name = "raw",
        endAliases = setOf("endraw"),
        branchParser = { parser, cmd ->
            var start: Token? = null
            var end: Node.Command? = null
            while (true) {
                val (txt, t) = parser.tokenizer.tokenizeInitial()
                if (start == null) {
                    start = txt
                }
                if (t == null) {
                    break
                }
                if (t is Token.BeginCommand) {
                    val t1 = parser.tokenizer.peek(true)
                    val name = parser.input.substring(t1.first .. t1.last)
                    if (t1 is Token.Identifier && name in cmd.endAliases) {
                        parser.tokenizer.consume(t1)
                        val t2 = parser.tokenizer.tokenize(true)
                        require(t2 is Token.EndCommand) {
                            "expected end command: actual: ${2::class}"
                        }
                        end = Node.Command(
                            first = t,
                            name = name,
                            args = emptyMap(),
                            branchAliases = cmd.branchAliases,
                            endAliases = cmd.endAliases,
                            last = t2
                        )
                        break
                    }
                }
            }
            if (end == null) {
                error("expected endraw command")
            }
            val trimLeft = parser.input[cmd.last.first] == '-'
            val trimRight = parser.input[end.first.last] == '-'
            val txtNode = Node.Text(
                parser.input,
                Token.Text(start.first..< end.first.first),
                trimLeft = trimLeft,
                trimRight = trimRight,
            )
            val branch = Node.Branch(
                first = cmd,
                body = listOfNotNull(txtNode),
                last = end
            )
            Node.Control(branch, emptyList())
        }
    ),
    Declarations.Command("set") {
        if (name == "set") {
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
            val t = tokenizer.peek(skipSpace = true)
            if (t is Token.Assign) {
                args["varNames"] = Expression.ArrayLiteral(
                    vars.first().first,
                    vars.last().last,
                    vars.toList()
                )
                tokenizer.consume(t)
                args["value"] = parseExpression()
            } else {
                args["varName"] = vars.singleOrNull()
                    ?: error("cannot set multiple variables with template body")
                endAliases += "endset"
            }
        }
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
            val listValueExpr = parseExpression()
            if (listValueExpr is Expression.BinOp &&
                listValueExpr.decl.name == "if")
            {
                args["listValue"] = listValueExpr.left
                args["condition"] = listValueExpr.right
            } else {
                args["listValue"] = listValueExpr
            }
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
                        require(!args.containsKey("condition")) {
                            "duplicate condition in for loop"
                        }
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
            val lPar = tokenizer.peek(skipSpace = true) as? Token.LPar
            if (lPar == null) {
                args["argNames"] = Expression.ArrayLiteral(
                    name, name, emptyList()
                )
                args["argDefaults"] = Expression.ObjectLiteral(
                    name, name, emptyList()
                )
                return@Command
            }
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
    },
    Declarations.Command(
        name = "filter",
        endAliases = setOf("endfilter")
    ) {
        if (name == "filter") {
            val name = expect<Token.Identifier>()
            args["name"] = Expression.StringLiteral(
                name,
                name,
                input.substring(name.first..name.last)
            )
            val lPar = tokenizer.peek(skipSpace = true) as? Token.LPar
            if (lPar == null) {
                args["argNames"] = Expression.ArrayLiteral(
                    name, name, emptyList()
                )
                args["argValues"] = Expression.ArrayLiteral(
                    name, name, emptyList()
                )
                return@Command
            }
            tokenizer.consume(lPar)
            val argNames = mutableListOf<Expression.StringLiteral>()
            val argValues = mutableListOf<Expression>()

            var t = tokenizer.peek(skipSpace = true)

            while (t !is Token.RPar) {
                var requireArgName = argNames.isNotEmpty()
                if (t is Token.Identifier) {
                    val assign = tokenizer.peekAfter(t, skipSpace = true)
                    if (assign is Token.Assign) {
                        val argNameExpression = Expression.StringLiteral(
                            t,
                            t,
                            input.substring(t.first..t.last)
                        )
                        argNames += argNameExpression
                        tokenizer.consume(assign)
                        requireArgName = false
                    }
                }
                if (requireArgName) {
                    error("expected arg name, found: $t")
                }
                argValues += parseExpression()
                t = tokenizer.tokenize(skipSpace = true)
                if (t is Token.Comma) {
                    t = tokenizer.peek(skipSpace = true)
                } else if (t !is Token.RPar) {
                    error("expected ')', found: $t")
                }
            }

            args["argNames"] = Expression.ArrayLiteral(
                lPar, t, argNames.toList()
            )
            args["argValues"] = Expression.ArrayLiteral(
                lPar, t, argValues.toList()
            )
        }
    },
    Declarations.Command(
        name = "include"
    ) {
        val file = parsePrimary()
        args["file"] = file
        //TODO optional "ignore" "missing"
        //TODO optional ( "with" | "without" ) "context"
    },
    Declarations.Command(
        name = "import"
    ) {
        val file = parsePrimary()
        expect<Token.Identifier>("as")
        val variable = expect<Token.Identifier>()
        args["file"] = file
        args["varName"] = Expression.StringLiteral(
            variable,
            variable,
            input.substring(variable.first..variable.last)
        )
    },
    Declarations.Command("from") {
        val file = parsePrimary()
        args["file"] = file
        expect<Token.Identifier>("import")
        val functionName1 = expect<Token.Identifier>()
        //TODO optional "as" varName
        //TODO optional "," functionName [ "as" varName ]
        args["functionName"] = Expression.StringLiteral(
            functionName1,
            functionName1,
            input.substring(functionName1.first..functionName1.last)
        )
    },
    Declarations.Command("extends", postProcessor = ExtendsPostProcessor()) {
        val file = parsePrimary()
        args["file"] = file
    },
    Declarations.Command("block", setOf("endblock")) {
        if (name == "block") {
            val variable = expect<Token.Identifier>()
            args["blockName"] = Expression.StringLiteral(
                variable,
                variable,
                input.substring(variable.first..variable.last)
            )
        }
    },
)

val defaultDeclaredUnaryOperations = listOf(
    Declarations.UnOp(99, "not"),
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

    Declarations.BinOp(50, "+", name = "plus", left = true),
    Declarations.BinOp(50, "-", name = "minus", left = true),

    *Declarations.BinOp(40, "in").let {
        arrayOf(
            it,
            Declarations.BinOp(40, "not in", name = "not_in", negate = it)
        )
    },

    *Declarations.TransformOp(40, "is").let {
        arrayOf(
            it,
            Declarations.TransformOp(40, "is not", name = "is_not", negate = it)
        )
    },

    Declarations.BinOp(40, "<", name = "lt"),
    Declarations.BinOp(40, "<=", name = "le"),
    Declarations.BinOp(40, ">", name = "gt"),
    Declarations.BinOp(40, ">=", name = "ge"),

    Declarations.BinOp(30, "==", name = "eq"),
    Declarations.BinOp(30, "!=", name = "ne"),

    Declarations.BinOp(20, "and", left = true),
    Declarations.BinOp(20, "or", left = true),

    Declarations.BinOp(15, "if", reverse = true, left = true),
    Declarations.BinOp(15, "else", left = true) { expression ->
        val left = (expression as? Expression.BinOp)?.left
        require((left as? Expression.BinOp)?.decl?.name == "if") {
            "operator 'else' without an if expression"
        }
        expression
    }
)

val defaultDeclarations = listOf(
    *defaultDeclaredCommands.toTypedArray(),
    *defaultDeclaredOperations.toTypedArray(),
    *defaultDeclaredUnaryOperations.toTypedArray()
)

class ExtendsPostProcessor : PostProcessor {

    val declaration = Declarations.Command("extends", postProcessor = this) {
        val file = parsePrimary()
        args["file"] = file
    }

    override fun transform(template: ParsedTemplate): ParsedTemplate {
        var i = 0
        while (i < template.nodes.size) {
            val node = template.nodes[i++]
            val result = buildExtends(template, node, i - 1)
            if (result != null) {
                return result
            }
            when (node) {
                is Node.Comment -> continue
                is Node.Text -> continue

                else -> break
            }
        }
        while (i < template.nodes.size) {
            val node = template.nodes[i++]
            if (isExtends(node)) {
                fail()
            }
        }
        return template
    }

    override fun transform(control: Node.Control): Node.Control {
        if (control.first.body.any(::isExtends) ||
                    control.branches.any(::haveExtends))
        {
            fail()
        }
        return control
    }

    private fun buildExtends(
        template: ParsedTemplate,
        node: Node,
        i: Int
    ): ParsedTemplate? {
        if (!isExtends(node)) {
            return null
        }
        node as Node.Command

        val newNodes = mutableListOf<Node>()
        val extendsNodes = mutableListOf<Node>()
        val branches = mutableListOf<Node.Branch>()

        for (k in 0 until template.nodes.size) {
            val node = template.nodes[k]
            if (k == i) {
                continue
            }
            if (k < i) {
                newNodes.add(node)
            } else if (node is Node.Control &&
                node.first.first.name == "block")
            {
                require(node.branches.isEmpty()) {
                    "unexpected branches in 'block' control"
                }
                branches.add(node.first)
            } else {
                if (isExtends(node)) {
                    fail()
                }
                extendsNodes.add(node)
            }
        }

        newNodes.add(
            Node.Control(
                first = Node.Branch(
                    first = node,
                    body = extendsNodes.toList(),
                    last = node
                ),
                branches = branches.toList()
            )
        )

        return ParsedTemplate(
            input = template.input,
            path = template.path,
            nodes = newNodes.toList()
        )
    }

    private fun haveExtends(branch: Node.Branch) =
        branch.body.any(::isExtends)

    private fun isExtends(node: Node) =
        node is Node.Command && node.name == declaration.name

    private fun fail(): Nothing = error(
        "command '${declaration.name}' is only valid at start of template"
    )
}

sealed class Declarations {
    class Command(
        val name: String,
        val endAliases: Set<String> = emptySet(),
        val branchAliases: Set<String> = emptySet(),
        val postProcessor: PostProcessor? = null,
        val branchParser: ((TemplateParser, Node.Command) -> Node.Control)? =
            null,
        val parser: (CommandArgBuilder.() -> Unit)? = null,
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
        override val negate: TransformOp? = null,
        left: Boolean = false,
        right: Boolean = false,
        transform: ((Expression) -> Expression)? = null
    ) : BinOp(
        precedence = precedence,
        aliases = aliases,
        name = name,
        negate = negate,
        reverse = false,
        left = left,
        right = right,
        transform = transform
    ) {
        constructor(
            precedence: Int,
            vararg aliases: String,
            name: String = aliases.first(),
            negate: TransformOp? = null,
            left: Boolean = false,
            right: Boolean = false,
            transform: ((Expression) -> Expression)? = null
        ) : this(
            precedence = precedence,
            aliases = aliases.toSet(),
            name = name,
            negate = negate,
            left = left,
            right = right,
            transform = transform
        )

        override fun toString(): String {
            return negate?.let { "Not($it)" } ?: "XOp($precedence, `$name`)"
        }
    }

    open class BinOp(
        val precedence: Int,
        val aliases: Set<String>,
        val name: String = aliases.first(),
        open val negate: BinOp? = null,
        val reverse: Boolean = false,
        val left: Boolean = false,
        val right: Boolean = false,
        val transform: ((Expression) -> Expression)? = null
    ) : Declarations() {
        constructor(
            precedence: Int,
            vararg aliases: String,
            name: String = aliases.first(),
            negate: BinOp? = null,
            reverse: Boolean = false,
            left: Boolean = false,
            right: Boolean = false,
            transform: ((Expression) -> Expression)? = null
        ) : this(
            precedence = precedence,
            aliases = aliases.toSet(),
            name = name,
            negate = negate,
            reverse = reverse,
            left = left,
            right = right,
            transform = transform,
        )

        override fun toString(): String {
            return negate?.let { "Not($it)" } ?: "BinOp($precedence, `$name`)"
        }
    }

    class UnOp(
        val precedence: Int,
        val aliases: Set<String>,
        val name: String = aliases.first(),
        val transform: ((Expression) -> Expression)? = null
    ) : Declarations() {
        constructor(
            precedence: Int,
            vararg aliases: String,
            name: String = aliases.first(),
            transform: ((Expression) -> Expression)? = null
        ) : this(
            precedence = precedence,
            aliases = aliases.toSet(),
            name = name,
            transform = transform
        )

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
        throw ParseException(
            tokenizer,
            t,
            "expected ${T::class}, actual ${t::class}"
        )
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
        throw ParseException(
            tokenizer,
            n,
            "expected ${N::class}, actual ${n::class}"
        )
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
        "expected boolean literal $value, actual: $n"
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
        "expected numeric literal $value, actual: $n"
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
