package org.cikit.forte.parser

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

    fun parseExpression(): Expression {
        val exprParser = ExpressionParserImpl(
            tokenizer,
            unaryOpDeclarations,
            binaryOpDeclarations
        )
        return exprParser.parseExpression()
    }

    fun parseExpressionOrNull(): Expression? {
        val exprParser = ExpressionParserImpl(
            tokenizer,
            unaryOpDeclarations,
            binaryOpDeclarations
        )
        return exprParser.parseExpressionOrNull()
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

                else -> throw ParseException(
                    tokenizer,
                    t,
                    "unexpected token $t"
                )
            }
        }
        return nodes.toList()
    }

    private fun parseComment(startToken: Token): Node.Comment {
        val (txt, t) = tokenizer.tokenizeEndComment()
        if (t !is Token.EndComment) {
            throw ParseException(
                tokenizer,
                t,
                "expected 'end comment' token"
            )
        }
        return Node.Comment(startToken, txt, t)
    }

    private fun parseCommand(startToken: Token): Node.Command {
        val nameToken = tokenizer.tokenize(skipSpace = true)
        if (nameToken !is Token.Identifier) {
            throw ParseException(tokenizer, nameToken, "expected command name")
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
                        tokenizer,
                        t,
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
            val args = mutableMapOf<String, Expression>()
            val argBuilder = object :
                CommandArgBuilder,
                ExpressionParser by exprParser
            {
                override val name: String
                    get() = name
                override val args: MutableMap<String, Expression>
                    get() = args
            }
            argsParser(argBuilder)
            val endToken = tokenizer.tokenize(skipSpace = true)
            if (endToken !is Token.EndCommand) {
                throw ParseException(
                    tokenizer,
                    endToken,
                    "expected 'end command' token"
                )
            }
            return Node.Command(
                startToken,
                name,
                args.toMap(),
                endToken
            )
        }
        // generic
        val args = mutableListOf<Expression>()
        while (true) {
            val arg = exprParser.parsePrimaryOrNull()
            if (arg != null) {
                args += arg
            }
            return when (val t = tokenizer.tokenize()) {
                is Token.Space -> continue
                is Token.EndCommand -> Node.Command(
                    startToken,
                    name,
                    args.mapIndexed { i, v -> i.toString() to v }.toMap(),
                    t
                )

                else -> throw ParseException(
                    tokenizer,
                    t,
                    "unexpected token $t"
                )
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
                tokenizer,
                content.last(),
                "expected end command ${decl.endAliases}"
            )
            val firstNode = if (input[branchStart.last.first] == '-') {
                (content.first() as? Node.Text)?.let { txt ->
                    Node.Text(txt.content, trimLeft = true, txt.trimRight)
                } ?: content.first()
            } else {
                content.first()
            }
            branches += Node.Branch(
                branchStart,
                listOf(firstNode) + content.subList(1, content.size - 1),
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
            throw ParseException(
                tokenizer,
                t,
                "expected 'end emit' token"
            )
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

    override fun parseExpressionOrNull(): Expression? {
        return when (val lhs = parsePrimaryOrNull()) {
            null -> null
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
                is Token.Dot, is Token.Comma, is Token.Colon, is Token.Assign,
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
        lhs: Expression,
        minPrecedence: Int
    ) : Expression {
        var mutLhs = lhs
        while (true) {
            val (op1, tokens1) = peekOp(binaryOpDeclarations) ?: break
            if (op1.precedence < minPrecedence) {
                // detected operator with lower precedence -> break here
                break
            }
            tokenizer.consume(tokens1.last())

            var rhs: Expression = parsePrimary()

            if (op1 is Declarations.TransformOp) {
                mutLhs = when (rhs) {
                    is Expression.Variable -> Expression.TransformOp(
                        tokens1, op1, mutLhs, rhs.name, Expression.NamedArgs()
                    )
                    is Expression.FunctionCall -> Expression.TransformOp(
                        tokens1, op1, mutLhs, rhs.name, rhs.args
                    )
                    else -> throw ParseException(
                        tokenizer,
                        tokens1.first(),
                        "expected extension function call"
                    )
                }
                continue
            }

            while (true) {
                val (op2, tokens2) = peekOp(binaryOpDeclarations) ?: break
                when {
                    op2.precedence < op1.precedence -> break
                    op2.precedence == op1.precedence -> when {
                        op1.left && op2.left -> break
                        !op1.right || !op2.right -> {
                            throw ParseException(
                                tokenizer,
                                tokens2.first(),
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

            mutLhs = Expression.BinOp(tokens1, op1, mutLhs, rhs)
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

    private fun parseSingleQuotedString(t: Token): Expression.StringLiteral {
        val content = StringBuilder()
        while (true) {
            val (txt, t2) = tokenizer.tokenizeSingleString()
            content.append(input, txt.first, txt.last + 1)
            when (t2) {
                is Token.SingleQuote -> {
                    return Expression.StringLiteral(t, t2, content.toString())
                }
                is Token.Escape -> {
                    val s = evalEscape(input[t2.last]) ?: throw ParseException(
                        tokenizer,
                        t2,
                        "invalid escape"
                    )
                    content.append(s)
                }
                is Token.UnicodeEscape -> {
                    val hex = input.substring(
                        (t2.last - 4) .. t2.last
                    )
                    content.append(hex.toInt(16).toChar())
                }
                else -> throw ParseException(
                    tokenizer,
                    t,
                    "unexpected token $t in string"
                )
            }
        }
    }

    private fun parseDoubleQuotedString(t: Token): Expression {
        val constContent = StringBuilder()
        var isConstString = true
        val content = mutableListOf<Expression>()
        while (true) {
            val (txt, t2) = tokenizer.tokenizeDoubleString()
            if (isConstString) {
                constContent.append(input, txt.first, txt.last + 1)
            }
            content += Expression.StringLiteral(
                txt,
                txt,
                input.substring(txt.first..txt.last)
            )
            when (t2) {
                is Token.DoubleQuote -> {
                    if (isConstString) {
                        val s = constContent.toString()
                        return Expression.StringLiteral(t, t2, s)
                    }
                    return Expression.StringInterpolation(content)
                }
                is Token.Escape -> {
                    val s = evalEscape (input[t2.last]) ?: throw ParseException(
                        tokenizer,
                        t2,
                        "invalid escape"
                    )
                    if (isConstString) {
                        constContent.append(s)
                    }
                    content += Expression.StringLiteral(t2, t2, s)
                }
                is Token.UnicodeEscape -> {
                    val hex = input.substring(
                        (t2.last - 4) .. t2.last
                    )
                    val c = hex.toInt(16).toChar()
                    if (isConstString) {
                        constContent.append(c)
                    }
                    content += Expression.StringLiteral(t2, t2, "$c")
                }
                is Token.BeginInterpolation -> {
                    isConstString = false
                    val subExpr = parseExpression()
                    val t3 = tokenizer.tokenize(skipSpace = true)
                    if (t3 !is Token.RBrace) {
                        throw ParseException(
                            tokenizer,
                            t3,
                            "expected closing brace"
                        )
                    }
                    content += subExpr
                }
                else -> throw ParseException(
                    tokenizer,
                    t,
                    "unexpected token $t in string"
                )
            }
        }
    }

    override fun parsePrimaryOrNull(): Expression? {
        peekOp(unaryOpDeclarations)?.let { (unOpDecl, unOpTokens) ->
            tokenizer.consume(unOpTokens.last())
            val p = parsePrimary()
            val rhs = parseExpression(p, minPrecedence = unOpDecl.precedence)
            return Expression.UnOp(unOpTokens, unOpDecl, rhs)
        }
        var primary: Expression
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
                        throw ParseException(
                            tokenizer,
                            t2,
                            "expected closing parenthesis"
                        )
                    }
                    primary = Expression.SubExpression(content)
                    break
                }

                is Token.LBracket -> {
                    tokenizer.consume(t)
                    val args = parseList()
                    val t2 = tokenizer.tokenize(skipSpace = true)
                    if (t2 !is Token.RBracket) {
                        throw ParseException(
                            tokenizer,
                            t2,
                            "expected closing bracket"
                        )
                    }
                    primary = Expression.ArrayLiteral(t, t2, args)
                    break
                }

                is Token.LBrace -> {
                    tokenizer.consume(t)
                    val args = parsePairs()
                    val t2 = tokenizer.tokenize(skipSpace = true)
                    if (t2 !is Token.RBrace) {
                        throw ParseException(
                            tokenizer,
                            t2,
                            "expected closing brace"
                        )
                    }
                    primary = Expression.ObjectLiteral(t, t2, args)
                    break
                }

                is Token.Identifier -> {
                    tokenizer.consume(t)
                    val name = input.substring(t.first .. t.last)
                    primary = Expression.Variable(t, name)
                    break
                }

                is Token.Const -> {
                    tokenizer.consume(t)
                    val s = input.substring(t.first..t.last)
                    primary = when (s.lowercase()) {
                        "null" -> Expression.NullLiteral(t)
                        "true" -> Expression.BooleanLiteral(t, true)
                        "false" -> Expression.BooleanLiteral(t, false)
                        "nan" -> Expression.NumericLiteral(
                            t, t, Double.NaN
                        )
                        "-infinity" -> Expression.NumericLiteral(
                            t, t, Double.NEGATIVE_INFINITY
                        )
                        "+infinity" -> Expression.NumericLiteral(
                            t, t, Double.POSITIVE_INFINITY
                        )
                        else -> Expression.Malformed(listOf(t))
                    }
                    break
                }

                is Token.Number -> {
                    tokenizer.consume(t)
                    val s = input.substring(t.first .. t.last)
                    val v: Number = when {
                        '.' in s -> s.toDouble()
                        else -> s.toInt()
                    }
                    primary = Expression.NumericLiteral(t, t, v)
                    break
                }

                // end token
                is Token.EndComment,
                is Token.EndCommand,
                is Token.EndEmit,
                is Token.RPar, is Token.RBrace, is Token.RBracket -> {
                    // empty expression
                    return null
                }

                is Token.Comma,
                is Token.Colon,
                is Token.Assign,
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
                is Token.End -> {
                    // unexpected token
                    return null
                }
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
                    primary = Expression.Access(t, t2, primary, name)
                }
                is Token.LBracket -> {
                    tokenizer.consume(t)
                    val args = parseExpressionOrNull()
                    val t2 = tokenizer.tokenize(skipSpace = true)
                    if (t2 !is Token.RBracket) {
                        throw ParseException(
                            tokenizer,
                            t2,
                            "expected closing bracket"
                        )
                    }
                    val arg = when (args) {
                        null -> throw ParseException(
                            tokenizer,
                            t2,
                            "unexpected token $t2"
                        )
                        else -> args
                    }
                    primary = Expression.CompAccess(t, primary, arg)
                }
                is Token.LPar -> {
                    tokenizer.consume(t)
                    val args = parseArgList()
                    val t2 = tokenizer.tokenize(skipSpace = true)
                    if (t2 !is Token.RPar) {
                        throw ParseException(
                            tokenizer,
                            t2,
                            "expected closing parenthesis"
                        )
                    }
                    primary = when (primary) {
                        is Expression.Variable -> {
                            Expression.FunctionCall(
                                primary.first,
                                primary.name,
                                args
                            )
                        }
                        else -> {
                            Expression.InvokeOp(
                                t, t2,
                                primary,
                                args
                            )
                        }
                    }
                }
                else -> break
            }
        }
        return primary
    }

    private fun parseList(): List<Expression> {
        val result = mutableListOf<Expression>()
        while (true) {
            val expression = parseExpressionOrNull() ?: break
            result += expression
            val t = tokenizer.peek(true)
            if (t !is Token.Comma) {
                break
            }
            tokenizer.consume(t)
        }
        return result.toList()
    }

    private fun parsePairs(): List<Pair<Expression, Expression>> {
        val result = mutableListOf<Pair<Expression, Expression>>()
        while (true) {
            val key = parseExpressionOrNull() ?: break
            val finalKey = if (key is Expression.Variable) {
                Expression.StringLiteral(key.first, key.first, key.name)
            } else {
                key
            }
            val t = tokenizer.peek(true)
            if (t !is Token.Colon) {
                throw ParseException(tokenizer, t, "expected colon")
            }
            tokenizer.consume(t)
            val value = parseExpression()
            result += finalKey to value
            val t2 = tokenizer.peek(true)
            if (t2 !is Token.Comma) {
                break
            }
            tokenizer.consume(t2)
        }
        return result.toList()
    }

    private fun parseArgList(): Expression.NamedArgs {
        var argIndex = 0
        var haveNamedArg = false
        val names = mutableListOf<String>()
        val values = mutableListOf<Expression>()
        while (true) {
            val expression = parseExpressionOrNull() ?: break
            val t = tokenizer.peek(true)
            if (t is Token.Assign) {
                haveNamedArg = true
                if (expression !is Expression.Variable) {
                    throw ParseException(
                        tokenizer,
                        expression,
                        "expected arg name"
                    )
                }
                tokenizer.consume(t)
                names += expression.name
                values += parseExpression()
                val t2 = tokenizer.peek(true)
                if (t2 !is Token.Comma) {
                    break
                }
                tokenizer.consume(t2)
            } else {
                if (haveNamedArg) {
                    throw ParseException(
                        tokenizer,
                        expression,
                        "expected named arg"
                    )
                }
                values += expression
                argIndex++
                if (t !is Token.Comma) {
                    break
                }
                tokenizer.consume(t)
            }
        }
        return Expression.NamedArgs(names, values)
    }
}
