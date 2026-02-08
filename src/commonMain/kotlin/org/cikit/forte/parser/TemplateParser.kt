package org.cikit.forte.parser

import org.cikit.forte.core.EvalException
import org.cikit.forte.core.compile
import org.cikit.forte.internal.parseInt

class TemplateParser private constructor(
    val tokenizer: TemplateTokenizer,
    val stringInterpolation: Boolean = true,
    val postProcessors: List<PostProcessor>,
    val commandDeclarations: Map<String, Declarations.Command>,
    val unaryOpDeclarations: Map<String, Declarations.UnOp>,
    val binaryOpDeclarations: Map<String, Declarations.BinOp>,
    private val context: Node.Command?
) {
    constructor(
        tokenizer: TemplateTokenizer
    ) : this(tokenizer, defaultDeclarations)

    constructor(
        tokenizer: TemplateTokenizer,
        declarations: List<Declarations> = defaultDeclarations,
    ) : this(
        tokenizer = tokenizer,
        declarations = declarations,
        stringInterpolation = true
    )

    constructor(
        tokenizer: TemplateTokenizer,
        stringInterpolation: Boolean,
        declarations: List<Declarations> = defaultDeclarations,
    ) : this(
        tokenizer = tokenizer,
        stringInterpolation = stringInterpolation,
        postProcessors = emptyList(),
        declarations = declarations
    )

    constructor(
        tokenizer: TemplateTokenizer,
        postProcessors: List<PostProcessor>,
        declarations: List<Declarations> = defaultDeclarations,
    ) : this(
        tokenizer = tokenizer,
        stringInterpolation = true,
        postProcessors = postProcessors,
        declarations = declarations
    )

    constructor(
        tokenizer: TemplateTokenizer,
        stringInterpolation: Boolean,
        postProcessors: List<PostProcessor>,
        declarations: List<Declarations> = defaultDeclarations
    ) : this(
        tokenizer = tokenizer,
        stringInterpolation = stringInterpolation,
        postProcessors = buildList {
            for (v in declarations) {
                if (v is Declarations.Command && v.postProcessor != null) {
                    add(v.postProcessor)
                }
            }
            addAll(postProcessors)
        },
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
        var parsedTemplate = ParsedTemplate(
            tokenizer.input,
            tokenizer.path,
            nodes
        )
        for (postProcessor in postProcessors) {
            parsedTemplate = postProcessor.transform(parsedTemplate)
        }
        return parsedTemplate
    }

    fun parseExpression(): Expression {
        val exprParser = ExpressionParserImpl(
            tokenizer,
            unaryOpDeclarations,
            binaryOpDeclarations,
            stringInterpolation = stringInterpolation
        )
        val parsedExpression = exprParser.parseExpression()
        val compiledExpression = try {
            parsedExpression.compile()
        } catch (ex: ParseException) {
            throw ex
        } catch (ex: EvalException) {
            throw ex
        } catch (ex: Exception) {
            throw ParseException(tokenizer, parsedExpression, ex)
        }
        return compiledExpression
    }

    fun parseExpressionOrNull(): Expression? {
        val exprParser = ExpressionParserImpl(
            tokenizer,
            unaryOpDeclarations,
            binaryOpDeclarations,
            stringInterpolation = stringInterpolation
        )
        val parsedExpression = exprParser.parseExpressionOrNull()
            ?: return null
        val compiledExpression = try {
            parsedExpression.compile()
        } catch (ex: ParseException) {
            throw ex
        } catch (ex: EvalException) {
            throw ex
        } catch (ex: Exception) {
            throw ParseException(tokenizer, parsedExpression, ex)
        }
        return compiledExpression
    }

    fun copy(
        tokenizer: TemplateTokenizer = this@TemplateParser.tokenizer,
        declarations: List<Declarations> = emptyList(),
    ) = copy(
        tokenizer = tokenizer,
        declarations = declarations,
        stringInterpolation = stringInterpolation,
        postProcessors = postProcessors,
        context = context
    )

    private fun copy(
        tokenizer: TemplateTokenizer = this.tokenizer,
        declarations: List<Declarations> = emptyList(),
        stringInterpolation: Boolean = this.stringInterpolation,
        postProcessors: List<PostProcessor> = this.postProcessors,
        context: Node.Command? = null
    ): TemplateParser {
        val newCommands: Map<String, Declarations.Command>
        val newBinOps: Map<String, Declarations.BinOp>
        val newUnOps: Map<String, Declarations.UnOp>
        if (declarations.isEmpty()) {
            newCommands = commandDeclarations
            newBinOps = binaryOpDeclarations
            newUnOps = unaryOpDeclarations
        } else {
            // rebuild declaration lookup tables with all aliases
            val commands = commandDeclarations.values.toMutableList()
            val binOps = binaryOpDeclarations.values.toMutableList()
            val unOps = unaryOpDeclarations.values.toMutableList()
            for (decl in declarations) {
                when (decl) {
                    is Declarations.Command -> commands += decl
                    is Declarations.BinOp -> binOps += decl
                    is Declarations.UnOp -> unOps += decl
                }
            }
            newCommands = Declarations.mapOf(commands)
            newBinOps = Declarations.mapOf(binOps)
            newUnOps = Declarations.mapOf(unOps)
        }
        return TemplateParser(
            tokenizer = tokenizer,
            stringInterpolation = stringInterpolation,
            postProcessors = postProcessors,
            commandDeclarations = newCommands,
            unaryOpDeclarations = newUnOps,
            binaryOpDeclarations = newBinOps,
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
            nodes += Node.Text(
                input,
                txt,
                trimLeft = trimLeft,
                trimRight = trimRight
            )
            if (t == null) {
                break
            }
            when (t) {
                is Token.BeginComment -> {
                    nodes += parseComment(t)
                }

                is Token.BeginCommand -> {
                    val (decl, cmd) = parseCommand(t)
                    if (context != null) {
                        if (cmd.name in context.branchAliases ||
                            cmd.name in context.endAliases
                        ) {
                            nodes += cmd
                            break
                        }
                    }

                    nodes += decl?.branchParser?.let { branchParser ->
                        //TODO error reporting
                        var newControl = branchParser(this, cmd)
                        for (postProcessor in postProcessors) {
                           newControl = postProcessor.transform(newControl)
                        }
                        newControl
                    } ?: if (cmd.endAliases.isNotEmpty()) {
                        var newControl = parseControl(cmd)
                        for (postProcessor in postProcessors) {
                            newControl = postProcessor.transform(newControl)
                        }
                        newControl
                    } else {
                        cmd
                    }
                }

                is Token.BeginEmit -> {
                    nodes += parseEmit(t)
                }

                else -> throw ParseException(
                    tokenizer,
                    t,
                    "unexpected token ${t::class}"
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
                "expected ${Token.EndComment::class}"
            )
        }
        return Node.Comment(startToken, txt, t)
    }

    private fun parseCommand(
        startToken: Token
    ): Pair<Declarations.Command?, Node.Command> {
        val nameToken = tokenizer.tokenize(skipSpace = true)
        if (nameToken !is Token.Identifier) {
            throw ParseException(tokenizer, nameToken, "expected command name")
        }
        val name = input.substring(nameToken.first..nameToken.last)
        val declaration: Declarations.Command? = when {
            context != null && (name in context.branchAliases ||
                    name in context.endAliases) ->
                        commandDeclarations[context.name]

            else -> commandDeclarations[name]
        }
        val argsParser = declaration?.parser
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
            binaryOpDeclarations,
            stringInterpolation = stringInterpolation
        )
        if (argsParser != null) {
            val argBuilder = object :
                CommandArgBuilder,
                ExpressionParser by exprParser
            {
                override val name: String
                    get() = name

                override val args:
                        MutableMap<String, Expression> = mutableMapOf()

                override val branchAliases:
                        MutableSet<String> = mutableSetOf()

                override val endAliases:
                        MutableSet<String> = mutableSetOf()

                init {
                    branchAliases.addAll(declaration.branchAliases)
                    endAliases.addAll(declaration.endAliases)
                }
            }
            argsParser(argBuilder)
            val endToken = tokenizer.tokenize(skipSpace = true)
            if (endToken !is Token.EndCommand) {
                throw ParseException(
                    tokenizer,
                    endToken,
                    "expected ${Token.EndCommand::class}"
                )
            }
            return declaration to Node.Command(
                startToken,
                name,
                argBuilder.args.toMap(),
                argBuilder.branchAliases.toSet(),
                argBuilder.endAliases.toSet(),
                endToken
            )
        }
        // generic
        val args = mutableMapOf<String, Expression>()
        while (true) {
            val arg = exprParser.parsePrimaryOrNull()
            if (arg != null) {
                args[(args.size + 1).toString()] = arg.compile()
            }
            when (val t = tokenizer.tokenize()) {
                is Token.Space -> continue
                is Token.EndCommand -> {
                    return declaration to Node.Command(
                        startToken,
                        name,
                        args.toMap(),
                        declaration?.branchAliases ?: emptySet(),
                        declaration?.endAliases ?: emptySet(),
                        t
                    )
                }

                else -> throw ParseException(
                    tokenizer,
                    t,
                    "unexpected token ${t::class}"
                )
            }
        }
    }

    private fun parseControl(cmd: Node.Command): Node.Control {
        val branches = mutableListOf<Node.Branch>()
        var branchStart = cmd
        while (true) {
            val content = copy(context = cmd).parse()
            val last = content.lastOrNull() as? Node.Command
                ?: throw ParseException(
                    tokenizer,
                    tokenizer.peek(), //TBD better to report start token?
                    "expected end command ${cmd.endAliases}"
                )
            val body = buildList {
                val firstNode = content.first()
                if (firstNode is Node.Text &&
                    input[branchStart.last.first] == '-')
                {
                    add(
                        Node.Text(
                            input,
                            firstNode.content,
                            trimLeft = true,
                            trimRight = firstNode.trimRight,
                        )
                    )
                } else {
                    add(firstNode)
                }
                for (i in 1 until content.size - 1) {
                    add(content[i])
                }
            }
            branches += Node.Branch(
                branchStart,
                body,
                last
            )
            branchStart = last
            if (last.name in cmd.endAliases) {
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
            binaryOpDeclarations,
            stringInterpolation = stringInterpolation
        )
        val content = exprParser.parseExpression().compile()
        val t = tokenizer.tokenize(skipSpace = true)
        if (t !is Token.EndEmit) {
            throw ParseException(
                tokenizer,
                t,
                "expected ${Token.EndEmit::class}"
            )
        }
        return Node.Emit(startToken, content, t)
    }
}

private class ExpressionParserImpl(
    override val tokenizer: ExpressionTokenizer,
    val unaryOpDeclarations: Map<String, Declarations.UnOp>,
    val binaryOpDeclarations: Map<String, Declarations.BinOp>,
    val stringInterpolation: Boolean
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
            stringInterpolation = stringInterpolation
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
            val alias = tokens1.joinToString(" ") { token ->
                input.substring(token.first .. token.last)
            }

            var rhs: Expression = parsePrimary()

            if (op1 is Declarations.TransformOp) {
                val expression = when (rhs) {
                    is Expression.Variable -> Expression.TransformOp(
                        tokens1 + rhs.first,
                        op1,
                        alias,
                        mutLhs,
                        rhs.name,
                        Expression.NamedArgs()
                    )
                    is Expression.FunctionCall -> Expression.TransformOp(
                        tokens1 + rhs.first,
                        op1,
                        alias,
                        mutLhs,
                        rhs.name,
                        rhs.args
                    )
                    is Expression.BooleanLiteral -> Expression.TransformOp(
                        tokens1 + rhs.token,
                        op1,
                        alias,
                        mutLhs,
                        input.substring(rhs.token.first, rhs.token.last),
                        Expression.NamedArgs()
                    )
                    is Expression.NullLiteral -> Expression.TransformOp(
                        tokens1 + rhs.token,
                        op1,
                        alias,
                        mutLhs,
                        input.substring(rhs.token.first, rhs.token.last),
                        Expression.NamedArgs()
                    )
                    is Expression.NumericLiteral -> {
                        val source = input.substring(
                            rhs.first.first,
                            rhs.first.last
                        )
                        if (!source.first().isLetter() ||
                            rhs.first != rhs.last)
                        {
                            throw ParseException(
                                tokenizer,
                                rhs,
                                "expected function call"
                            )
                        }
                        Expression.TransformOp(
                            tokens1 + rhs.first,
                            op1,
                            alias,
                            mutLhs,
                            source,
                            Expression.NamedArgs()
                        )
                    }
                    else -> throw ParseException(
                        tokenizer,
                        rhs,
                        "expected function call"
                    )
                }
                mutLhs = try {
                    op1.transform?.invoke(expression) ?: expression
                } catch (ex: Exception) {
                    throw ParseException(tokenizer, expression, ex)
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

            val expression = Expression.BinOp(tokens1, op1, alias, mutLhs, rhs)
            mutLhs = try {
                op1.transform?.invoke(expression) ?: expression
            } catch (ex: Exception) {
                throw ParseException(tokenizer, expression, ex)
            }
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
                    "unexpected token ${t::class} in string"
                )
            }
        }
    }

    private fun parseDoubleQuotedString(t: Token): Expression {
        val constContent = StringBuilder()
        var isConstString = true
        val content = mutableListOf<Expression>()
        while (true) {
            val (txt, t2) = tokenizer.tokenizeDoubleString(
                stringInterpolation = stringInterpolation
            )
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
                    "unexpected token ${t::class} in string"
                )
            }
        }
    }

    override fun parsePrimaryOrNull(): Expression? {
        peekOp(unaryOpDeclarations)?.let { (unOpDecl, unOpTokens) ->
            tokenizer.consume(unOpTokens.last())
            val alias = unOpTokens.joinToString(" ") { token ->
                input.substring(token.first .. token.last)
            }
            val p = parsePrimary()
            val rhs = parseExpression(p, minPrecedence = unOpDecl.precedence)
            val expression = Expression.UnOp(unOpTokens, unOpDecl, alias, rhs)
            return try {
                unOpDecl.transform?.invoke(expression) ?: expression
            } catch (ex: Exception) {
                throw ParseException(tokenizer, expression, ex)
            }
        }
        val unexpectedBinOp = peekOp(binaryOpDeclarations)
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
                        "infinity",
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
                    val v: Number = try {
                        parseInt(s)
                    } catch (_: NumberFormatException) {
                        s.toDouble()
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
                    unexpectedBinOp?.let { (binOpDecl, binOpTokens) ->
                        throw ParseException(
                            tokenizer,
                            binOpTokens.first(),
                            binOpTokens.last(),
                            "unexpected binary operator `${binOpDecl.name}`"
                        )
                    }
                    val t2 = tokenizer.peekAfter(t, skipSpace = true)
                    val name = when (t2) {
                        is Token.Identifier -> {
                            input.substring(t2.first .. t2.last)
                        }
                        is Token.Const if input[t2.first].isLetter() -> {
                            input.substring(t2.first .. t2.last)
                        }

                        else -> {
                            // maybe operator
                            break
                        }
                    }
                    tokenizer.consume(t2)
                    primary = Expression.Access(t, t2, primary, name)
                }
                is Token.LBracket -> {
                    unexpectedBinOp?.let { (binOpDecl, binOpTokens) ->
                        throw ParseException(
                            tokenizer,
                            binOpTokens.first(),
                            binOpTokens.last(),
                            "unexpected binary operator `${binOpDecl.name}`"
                        )
                    }
                    tokenizer.consume(t)
                    val args = parseSliceIndex()
                    val t2 = tokenizer.tokenize(skipSpace = true)
                    if (t2 !is Token.RBracket) {
                        throw ParseException(
                            tokenizer,
                            t2,
                            "expected closing bracket"
                        )
                    }
                    if (args.size > 1) {
                        val argNames = mutableListOf<String>()
                        val argValues = mutableListOf<Expression>()
                        args[0]?.let {
                            argNames += "start"
                            argValues += it
                        }
                        args[1]?.let {
                            argNames += "end"
                            argValues += it
                        }
                        if (args.size > 2) {
                            args[2]?.let {
                                argNames += "step"
                                argValues += it
                            }
                        }
                        primary = Expression.SliceAccess(
                            t,
                            primary,
                            Expression.NamedArgs(
                                argNames.toList(),
                                argValues.toList()
                            )
                        )
                    } else {
                        val arg = if (args.isEmpty()) {
                            null
                        } else {
                            args[0]
                        }
                        if (arg == null) {
                            throw ParseException(
                                tokenizer,
                                t, t2,
                                "no key passed"
                            )
                        }
                        primary = Expression.CompAccess(t, primary, arg)
                    }
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
        if (primary is Expression.Variable) {
            unexpectedBinOp?.let { (binOpDecl, binOpTokens) ->
                throw ParseException(
                    tokenizer,
                    binOpTokens.first(),
                    binOpTokens.last(),
                    "unexpected binary operator `${binOpDecl.name}`"
                )
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

    private fun parseSliceIndex(): List<Expression?> {
        val result = mutableListOf<Expression?>()
        while (true) {
            val expression = parseExpressionOrNull()
            result += expression
            if (result.size == 3) {
                break
            }
            val t = tokenizer.peek(true)
            if (t !is Token.Colon) {
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
                        "expected arg name, actual ${expression::class}"
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
