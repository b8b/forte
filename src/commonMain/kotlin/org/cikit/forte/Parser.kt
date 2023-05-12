package org.cikit.forte

import okio.Path

interface ExpressionParser {
    val tokenizer: ExpressionTokenizer
    val input: String get() = tokenizer.input

    fun copy(declarations: List<Declarations> = emptyList()): ExpressionParser

    fun parsePrimary(): Node.Expression
    fun parseExpression(): Node.Expression
    fun parseExpression(
        lhs: Node.Expression,
        minPrecedence: Int
    ): Node.Expression
}

inline fun <reified T: Token> ExpressionParser.expect(
    skipSpace: Boolean = true
): T {
    val t = tokenizer.tokenize(skipSpace = skipSpace)
    if (t !is T) {
        throw ParseException(t, "expected ${T::class}")
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
        throw ParseException(t, "expected '$value', actual '$actual'")
    }
}

inline fun <reified N: Node.Expression> ExpressionParser.expect(): N {
    val n = parsePrimary()
    if (n !is N) {
        throw ParseException(n, "expected ${N::class}")
    }
    return n
}

fun ExpressionParser.expect(value: Boolean): Node.BooleanLiteral {
    val n = this.expect<Node.BooleanLiteral>()
    if (n.value == value) {
        return n
    }
    throw ParseException(
        n,
        "expected boolean literal $value, actual: ${n.value}"
    )
}

fun ExpressionParser.expect(value: Number): Node.NumericLiteral {
    val n = this.expect<Node.NumericLiteral>()
    if (n.value == value) {
        return n
    }
    throw ParseException(
        n,
        "expected numeric literal $value, actual: ${n.value}"
    )
}

fun ExpressionParser.expect(value: String): Node.StringLiteral {
    val n = this.expect<Node.StringLiteral>()
    if (n.value == value) {
        return n
    }
    throw ParseException(n, "expected string literal '$value', actual: $n")
}

interface CommandArgBuilder : ExpressionParser {
    val name: String
    val args: MutableMap<String, Node.Expression>
}

private val emptyTokenizer = Tokenizer("")
private val defaultParser = TemplateParser(emptyTokenizer)

fun parseTemplate(input: String, path: Path? = null) =
    defaultParser.copy(Tokenizer(input, path)).parseTemplate()

fun parseTemplate(
    input: String,
    path: Path? = null,
    declarations: List<Declarations> = defaultDeclarations
) = TemplateParser(Tokenizer(input, path), declarations).parseTemplate()

fun parseTemplate(tokenizer: TemplateTokenizer) =
    defaultParser.copy(tokenizer).parseTemplate()

fun parseTemplate(
    tokenizer: TemplateTokenizer,
    declarations: List<Declarations>
) = TemplateParser(tokenizer, declarations).parseTemplate()

class ParsedTemplate(
    val input: String,
    val path: Path?,
    val nodes: List<Node>
)

class TemplateParser private constructor(
    val tokenizer: TemplateTokenizer,
    val commandDeclarations: Map<String, Declarations.Command>,
    val unaryOpDeclarations: Map<String, Declarations.UnOp>,
    val binaryOpDeclarations: Map<String, Declarations.BinOp>,
    private val context: Declarations.Command?
) {
    constructor(
        tokenizer: TemplateTokenizer
    ) : this(tokenizer, defaultDeclarations)

    constructor(
        tokenizer: TemplateTokenizer,
        declarations: List<Declarations> = defaultDeclarations,
    ) : this(
        tokenizer = tokenizer,
        commandDeclarations = Declarations.mapOf(
            declarations.filterIsInstance<Declarations.Command>()
        ),
        unaryOpDeclarations = Declarations.mapOf(
            declarations.filterIsInstance<Declarations.UnOp>()
        ),
        binaryOpDeclarations = Declarations.mapOf(
            declarations.filterIsInstance<Declarations.BinOp>()
        ),
        context = null
    )

    val input: String get() = tokenizer.input

    fun parseTemplate(): ParsedTemplate {
        val nodes = parse()
        return ParsedTemplate(tokenizer.input, tokenizer.path, nodes)
    }

    fun copy(
        tokenizer: TemplateTokenizer = this@TemplateParser.tokenizer,
        declarations: List<Declarations> = emptyList(),
    ) = copy(
        tokenizer = tokenizer,
        declarations = declarations,
        context = null
    )

    private fun copy(
        tokenizer: TemplateTokenizer = this@TemplateParser.tokenizer,
        declarations: List<Declarations> = emptyList(),
        context: Declarations.Command? = null
    ): TemplateParser {
        // override declarations by primary name
        val newCommands = commandDeclarations.values.associateBy { it.name } +
                declarations.filterIsInstance<Declarations.Command>()
                    .associateBy { it.name }
        val newOps = binaryOpDeclarations.values.associateBy { it.name } +
                declarations.filterIsInstance<Declarations.BinOp>()
                    .associateBy { it.name }
        val newUnOps = unaryOpDeclarations.values.associateBy { it.name } +
                declarations.filterIsInstance<Declarations.UnOp>()
                    .associateBy { it.name }
        // rebuild declaration lookup tables with all aliases
        return TemplateParser(
            tokenizer = tokenizer,
            commandDeclarations = Declarations.mapOf(newCommands.values),
            unaryOpDeclarations = Declarations.mapOf(newUnOps.values),
            binaryOpDeclarations = Declarations.mapOf(newOps.values),
            context = context
        )
    }

    private fun parse(): List<Node> {
        val nodes = mutableListOf<Node>()

        while (true) {
            val (txt, t) = tokenizer.tokenizeInitial()
            val lastToken = when (val n = nodes.lastOrNull()) {
                is Node.Comment -> n.last
                is Node.Command -> n.last
                is Node.Emit -> n.last
                else -> null
            }
            val trimLeft = lastToken != null && input[lastToken.first] == '-'
            val trimRight = t != null && input[t.last] == '-'
            nodes += Node.Text(txt, trimLeft = trimLeft, trimRight = trimRight)
            if (t == null) {
                break
            }
            when (t) {
                is Token.BeginComment -> {
                    nodes += parseComment(t)
                }

                is Token.BeginCommand -> {
                    val cmd = parseCommand(t)
                    if (context != null) {
                        if (cmd.name in context.branchAliases ||
                            cmd.name in context.endAliases
                        ) {
                            nodes += cmd
                            break
                        }
                    }
                    nodes += commandDeclarations[cmd.name]
                        ?.takeIf { it.endAliases.isNotEmpty() }
                        ?.let { parseControl(cmd, it) }
                        ?: cmd
                }

                is Token.BeginEmit -> {
                    nodes += parseEmit(t)
                }

                else -> throw ParseException(t, "unexpected token")
            }
        }
        return nodes.toList()
    }

    private fun parseComment(startToken: Token): Node.Comment {
        val (txt, t) = tokenizer.tokenizeEndComment()
        if (t !is Token.EndComment) {
            throw ParseException(startToken, t, "unclosed comment")
        }
        return Node.Comment(startToken, txt, t)
    }

    private fun parseCommand(startToken: Token): Node.Command {
        val nameToken = tokenizer.tokenize(skipSpace = true)
        if (nameToken !is Token.Identifier) {
            throw ParseException(nameToken, "expected command name")
        }
        val name = input.substring(nameToken.first..nameToken.last)
        val argsParser = when {
            context != null && (name in context.branchAliases ||
                    name in context.endAliases) -> context.parser

            else -> commandDeclarations[name]?.parser
        }
        // build an expression parser that cannot consume beyond
        // the next command end token
        val subTokenizer = object : TemplateTokenizer by tokenizer {
            private var endToken: Token? = null

            private fun checkEndToken(t: Token) {
                val endToken = endToken
                if (endToken == null) {
                    if (t is Token.EndCommand) {
                        this.endToken = t
                    }
                    return
                }
                if (endToken.first < t.first) {
                    throw ParseException(
                        startToken, t,
                        "parsing command exceeded command end token"
                    )
                }
            }

            override fun tokenize(skipSpace: Boolean): Token {
                val t = tokenizer.tokenize(skipSpace)
                checkEndToken(t)
                return t
            }

            override fun peek(skipSpace: Boolean): Token {
                val t = tokenizer.peek(skipSpace)
                checkEndToken(t)
                return t
            }

            override fun peekAfter(
                token: Token,
                skipSpace: Boolean
            ): Token {
                val t = tokenizer.peekAfter(token, skipSpace)
                checkEndToken(t)
                return t
            }
        }
        val exprParser = ExpressionParserImpl(
            subTokenizer,
            unaryOpDeclarations,
            binaryOpDeclarations
        )
        if (argsParser != null) {
            val args = mutableMapOf<String, Node.Expression>()
            val argBuilder = object :
                CommandArgBuilder,
                ExpressionParser by exprParser
            {
                override val name: String
                    get() = name
                override val args: MutableMap<String, Node.Expression>
                    get() = args
            }
            try {
                argsParser(argBuilder)
                val endToken = tokenizer.tokenize(skipSpace = true)
                require(endToken is Token.EndCommand) {
                    "expected end command, found: $endToken"
                }
                return Node.Command(
                    startToken,
                    name,
                    args.toList(),
                    endToken
                )
            } catch (ex: Throwable) {
                throw ParseException(
                    startToken,
                    "error parsing args for command '$name'",
                    ex
                )
            }
        }
        // generic
        val args = mutableListOf<Node.Expression>()
        while (true) {
            val arg = exprParser.parsePrimary()
            if (arg !is Node.Empty) {
                args += arg
            }
            return when (val t = tokenizer.tokenize()) {
                is Token.Space -> continue
                is Token.EndCommand -> Node.Command(
                    startToken,
                    name,
                    args.mapIndexed { index, expression ->
                        index.toString() to expression
                    },
                    t
                )

                else -> throw ParseException(t, "unexpected token")
            }
        }
    }

    private fun parseControl(
        cmd: Node.Command,
        decl: Declarations.Command
    ): Node.Control {
        val branches = mutableListOf<Node.Branch>()
        var branchStart = cmd
        while (true) {
            val content = copy(context = decl).parse()
            val last = content.last() as? Node.Command ?: throw ParseException(
                content.last(),
                "expected end command ${decl.endAliases}"
            )
            branches += Node.Branch(
                branchStart,
                content.subList(0, content.size - 1),
                last
            )
            branchStart = last
            if (last.name in decl.endAliases) {
                break
            }
        }
        return Node.Control(
            branches.first(),
            branches.subList(1, branches.size)
        )
    }

    private fun parseEmit(startToken: Token): Node.Emit {
        val exprParser = ExpressionParserImpl(
            tokenizer,
            unaryOpDeclarations,
            binaryOpDeclarations
        )
        val content = exprParser.parseExpression()
        val t = tokenizer.tokenize(skipSpace = true)
        if (t !is Token.EndEmit) {
            throw ParseException(startToken, t, "unclosed emit")
        }
        return Node.Emit(startToken, content, t)
    }
}

private class ExpressionParserImpl(
    override val tokenizer: ExpressionTokenizer,
    val unaryOpDeclarations: Map<String, Declarations.UnOp>,
    val binaryOpDeclarations: Map<String, Declarations.BinOp>,
) : ExpressionParser {

    override fun copy(declarations: List<Declarations>): ExpressionParser {
        // override declarations by primary name
        val newOps = binaryOpDeclarations.values.associateBy { it.name } +
                declarations.filterIsInstance<Declarations.BinOp>()
                    .associateBy { it.name }
        val newUnOps = unaryOpDeclarations.values.associateBy { it.name } +
                declarations.filterIsInstance<Declarations.UnOp>()
                    .associateBy { it.name }
        // rebuild declaration lookup tables with all aliases
        return ExpressionParserImpl(
            tokenizer = tokenizer,
            unaryOpDeclarations = Declarations.mapOf(newUnOps.values),
            binaryOpDeclarations = Declarations.mapOf(newOps.values),
        )
    }

    override fun parseExpression(): Node.Expression {
        return when (val lhs = parsePrimary()) {
            is Node.Empty -> lhs
            else -> parseExpression(lhs, minPrecedence = 0)
        }
    }

    private fun <DT: Declarations> peekOp(
        declarations: Map<String, DT>
    ): Pair<DT, List<Token>>? {
        val sb = StringBuilder()
        val buffer = ArrayDeque<Token>()
        var lastDecl: DT? = null
        var lastWidth = 0

        var t = tokenizer.peek(skipSpace = true)
        while(true) {
            when (t) {
                is Token.Identifier,
                is Token.Dot, is Token.Comma, is Token.Colon,
                is Token.LPar, is Token.LBracket,
                is Token.Operator,
                is Token.Space -> {
                    buffer += t
                    sb.append(input.substring(t.first .. t.last))
                    declarations[sb.toString()]?.let { decl ->
                        lastWidth = buffer.size
                        lastDecl = decl
                    }
                    t = tokenizer.peekAfter(t)
                }
                else -> break
            }
        }
        return lastDecl?.let { decl ->
            return decl to buffer.subList(0, lastWidth)
        }
    }

    override fun parseExpression(
        lhs: Node.Expression,
        minPrecedence: Int
    ) : Node.Expression {
        var mutLhs = lhs
        while (true) {
            val (op1, tokens1) = peekOp(binaryOpDeclarations) ?: break
            if (op1.precedence < minPrecedence) {
                // detected operator with lower precedence -> break here
                break
            }
            tokenizer.consume(tokens1.last())

            var rhs = parsePrimary()
            while (true) {
                val (op2, tokens2) = peekOp(binaryOpDeclarations) ?: break
                when {
                    op2.precedence < op1.precedence -> break
                    op2.precedence == op1.precedence -> when {
                        op1.left && op2.left -> break
                        !op1.right || !op2.right -> {
                            throw ParseException(
                                tokens2.first(),
                                tokens2.last(),
                                "unexpected operator"
                            )
                        }
                    }
                }
                rhs = parseExpression(rhs, op1.precedence + when {
                    op2.precedence > op1.precedence -> 1
                    else -> 0
                })
            }

            mutLhs = Node.BinOp(tokens1, op1, mutLhs, rhs)
        }

        return mutLhs
    }

    private fun evalEscape(ch: Char): String? = when (ch) {
        'b' -> "\u0008"
        't' -> "\u0009"
        'f' -> "\u000c"
        'n' -> "\u000a"
        'r' -> "\u000d"
        '\\' -> "\\"
        '"' -> "\""
        '\'' -> "'"
        else -> null
    }

    private fun parseSingleQuotedString(t: Token): Node.StringLiteral {
        val content = StringBuilder()
        while (true) {
            val (txt, t2) = tokenizer.tokenizeSingleString()
            content.append(input, txt.first, txt.last + 1)
            when (t2) {
                is Token.SingleQuote -> {
                    return Node.StringLiteral(t, t2, content.toString())
                }
                is Token.Escape -> {
                    val s = evalEscape(input[t2.last])
                        ?: throw ParseException(t2, t2, "invalid escape")
                    content.append(s)
                }
                is Token.UnicodeEscape -> {
                    val hex = input.substring(
                        (t2.last - 4) .. t2.last
                    )
                    content.append(hex.toInt(16).toChar())
                }
                else -> throw ParseException(
                    t, t2,
                    "unexpected token in string"
                )
            }
        }
    }

    private fun parseDoubleQuotedString(t: Token): Node.Expression {
        val constContent = StringBuilder()
        var isConstString = true
        val content = mutableListOf<Node.Expression>()
        while (true) {
            val (txt, t2) = tokenizer.tokenizeDoubleString()
            if (isConstString) {
                constContent.append(input, txt.first, txt.last + 1)
            }
            content += Node.StringLiteral(
                txt,
                txt,
                input.substring(txt.first .. txt.last)
            )
            when (t2) {
                is Token.DoubleQuote -> {
                    if (isConstString) {
                        val s = constContent.toString()
                        return Node.StringLiteral(t, t2, s)
                    }
                    return Node.StringInterpolation(content)
                }
                is Token.Escape -> {
                    val s = evalEscape (input[t2.last])
                        ?: throw ParseException(t2, t2, "invalid escape")
                    if (isConstString) {
                        constContent.append(s)
                    }
                    content += Node.StringLiteral(t2, t2, s)
                }
                is Token.UnicodeEscape -> {
                    val hex = input.substring(
                        (t2.last - 4) .. t2.last
                    )
                    val c = hex.toInt(16).toChar()
                    if (isConstString) {
                        constContent.append(c)
                    }
                    content += Node.StringLiteral(t2, t2, "$c")
                }
                is Token.BeginInterpolation -> {
                    isConstString = false
                    val subExpr = parseExpression()
                    val t3 = tokenizer.tokenize(skipSpace = true)
                    if (t3 !is Token.RBrace) {
                        throw ParseException(t3, "expected closing brace")
                    }
                    content += subExpr
                }
                else -> throw ParseException(
                    t, t2,
                    "unexpected token in string"
                )
            }
        }
    }

    override fun parsePrimary(): Node.Expression {
        peekOp(unaryOpDeclarations)?.let { (unOpDecl, unOpTokens) ->
            tokenizer.consume(unOpTokens.last())
            val p = parsePrimary()
            val rhs = parseExpression(p, minPrecedence = unOpDecl.precedence)
            return Node.UnOp(unOpTokens, unOpDecl, rhs)
        }
        var primary: Node.Expression
        while (true) {
            when (val t = tokenizer.peek()) {
                is Token.Space -> {
                    //ignore
                    tokenizer.consume(t)
                }

                is Token.SingleQuote -> {
                    tokenizer.consume(t)
                    primary = parseSingleQuotedString(t)
                    break
                }

                is Token.DoubleQuote -> {
                    tokenizer.consume(t)
                    primary = parseDoubleQuotedString(t)
                    break
                }

                is Token.LPar -> {
                    tokenizer.consume(t)
                    val content = parseExpression()
                    val t2 = tokenizer.tokenize(skipSpace = true)
                    if (t2 !is Token.RPar) {
                        throw ParseException(t2, "expected closing parenthesis")
                    }
                    primary = Node.SubExpression(content)
                    break
                }

                is Token.LBracket -> {
                    tokenizer.consume(t)
                    val args = parseExpression()
                    val t2 = tokenizer.tokenize(skipSpace = true)
                    if (t2 !is Token.RBracket) {
                        throw ParseException(t2, "expected closing bracket")
                    }
                    val itemList = when (args) {
                        is Node.Empty -> emptyList()
                        else -> flattenList(args)
                    }
                    primary = Node.ArrayLiteral(t, t2, itemList)
                    break
                }

                is Token.LBrace -> {
                    tokenizer.consume(t)
                    val args = parseExpression()
                    val t2 = tokenizer.tokenize(skipSpace = true)
                    if (t2 !is Token.RBrace) {
                        throw ParseException(t2, "expected closing brace")
                    }
                    val itemList = when (args) {
                        is Node.Empty -> emptyList()
                        else -> flattenPairs(args)
                    }
                    primary = Node.ObjectLiteral(t, t2, itemList)
                    break
                }

                is Token.Identifier -> {
                    tokenizer.consume(t)
                    val name = input.substring(t.first .. t.last)
                    primary = Node.Variable(t, name)
                    break
                }

                is Token.Const -> {
                    tokenizer.consume(t)
                    val s = input.substring(t.first..t.last)
                    primary = when (s.lowercase()) {
                        "null" -> Node.NullLiteral(t)
                        "true" -> Node.BooleanLiteral(t, true)
                        "false" -> Node.BooleanLiteral(t, false)
                        "nan" -> Node.NumericLiteral(
                            t, t, Double.NaN
                        )
                        "-infinity" -> Node.NumericLiteral(
                            t, t, Double.NEGATIVE_INFINITY
                        )
                        "+infinity" -> Node.NumericLiteral(
                            t, t, Double.POSITIVE_INFINITY
                        )
                        else -> Node.Malformed(listOf(t))
                    }
                    break
                }

                is Token.Number -> {
                    tokenizer.consume(t)
                    val s = input.substring(t.first .. t.last)
                    val v = when {
                        '.' in s -> s.toDouble()
                        else -> s.toInt()
                    }
                    primary = Node.NumericLiteral(t, t, v)
                    break
                }

                // end token
                is Token.EndComment,
                is Token.EndCommand,
                is Token.EndEmit,
                is Token.RPar, is Token.RBrace, is Token.RBracket -> {
                    // empty expression
                    return Node.Empty
                }

                is Token.Comma,
                is Token.Colon,
                is Token.Dot,
                is Token.Operator,
                is Token.Text,
                is Token.BeginInterpolation,
                is Token.Escape,
                is Token.UnicodeEscape,
                is Token.InvalidEscape,
                is Token.BeginComment,
                is Token.BeginCommand,
                is Token.BeginEmit,
                is Token.Word,
                is Token.End -> throw ParseException(t, "unexpected token")
            }
        }
        while (true) {
            when (val t = tokenizer.peek(skipSpace = true)) {
                is Token.Dot -> {
                    val t2 = tokenizer.peekAfter(t, skipSpace = true)
                    if (t2 !is Token.Identifier) {
                        // maybe operator
                        break
                    }
                    tokenizer.consume(t2)
                    val name = input.substring(t2.first .. t2.last)
                    primary = Node.Access(t, t2, primary, name)
                }
                is Token.LBracket -> {
                    tokenizer.consume(t)
                    val args = parseExpression()
                    val t2 = tokenizer.tokenize(skipSpace = true)
                    if (t2 !is Token.RBracket) {
                        throw ParseException(t2, "expected closing bracket")
                    }
                    val arg = when (args) {
                        is Node.Empty -> throw ParseException(
                            t2,
                            "unexpected token"
                        )
                        else -> args
                    }
                    primary = Node.CompAccess(t, primary, arg)
                }
                is Token.LPar -> {
                    tokenizer.consume(t)
                    val args = parseExpression()
                    val t2 = tokenizer.tokenize(skipSpace = true)
                    if (t2 !is Token.RPar) {
                        throw ParseException(t2, "expected closing parenthesis")
                    }
                    primary = createCall(primary, args)
                }
                else -> break
            }
        }
        return primary
    }

    private fun createCall(
        left: Node.Expression,
        args: Node.Expression
    ): Node.Expression {
        var argIndex = 0
        val argList: List<Pair<String, Node.Expression>> = when (args) {
            is Node.Empty -> emptyList()
            else -> flattenList(args).map { item ->
                if ((item as? Node.BinOp)?.decl?.name == "pair") {
                    val name = item.left
                    if (name !is Node.Variable) {
                        throw ParseException(name, "expected argument name")
                    }
                    name.name to item
                } else {
                    require(argIndex >= 0) {
                        throw ParseException(item, "expected named argument")
                    }
                    val name = argIndex.toString()
                    argIndex++
                    name to item
                }
            }
        }
        return when (left) {
            is Node.Variable -> {
                Node.FunctionCall(left.first, left.name, argList)
            }
            is Node.Access -> {
                Node.ExtensionCall(left.first, left.left, left.name, argList)
            }
            else -> Node.MethodCall(left, argList)
        }
    }

    private fun flattenList(node: Node.Expression): List<Node.Expression> {
        return when (node) {
            is Node.BinOp -> {
                if (node.decl.name == "tuple") {
                    flattenList(node.left) + flattenList(node.right)
                } else {
                    listOf(node)
                }
            }

            else -> listOf(node)
        }
    }

    private fun flattenPairs(
        node: Node.Expression
    ): List<Pair<Node.Expression, Node.Expression>> {
        return flattenList(node).map { item ->
            when (item) {
                is Node.BinOp -> {
                    when (item.decl.name) {
                        "pair" -> item.left to item.right
                        else -> throw ParseException(item, "expected pair")
                    }
                }

                else -> throw ParseException(item, "expected pair")
            }
        }
    }
}
