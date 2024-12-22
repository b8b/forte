package org.cikit.forte.parser

import org.cikit.forte.core.UPath

private object Patterns {
    val beginComment =   """\{#-?""".toRegex() to Token::BeginComment
    val endComment =     """-?#\}""".toRegex() to Token::EndComment

    val beginEmit =      """\{\{-?""".toRegex() to Token::BeginEmit
    val endEmit =        """-?\}\}""".toRegex() to Token::EndEmit

    val beginCommand =   """\{%-?""".toRegex() to Token::BeginCommand
    val endCommand =     """-?%\}""".toRegex() to Token::EndCommand

    val operator =       """[-+=*/%|!<>?&~`]+""".toRegex() to Token::Operator

    val space =          """\s+""".toRegex() to Token::Space

    val const =          """null|[Tt]rue|[Ff]alse|NaN|[+-]Infinity"""
        .toRegex() to Token::Const
    val number =         """[+-]?(?:0|[1-9]\d*)(?:\.\d+)?(?:[Ee][+-]\d+)?"""
        .toRegex() to Token::Number
    val identifier =     """[A-Za-z_][A-Za-z0-9_]*"""
        .toRegex() to Token::Identifier

    val word =           """\S+""".toRegex() to Token::Word

    val singleQuote =    """'""".toRegex() to Token::SingleQuote
    val uEscape =        """\\u[0-9A-Fa-f]{4}"""
        .toRegex() to Token::UnicodeEscape
    val escape =         """\\[^u]""".toRegex() to Token::Escape
    val invEscape =      """\\""".toRegex() to Token::InvalidEscape

    val doubleQuote =    """"""".toRegex() to Token::DoubleQuote
    val interpolation =  """#\{""".toRegex() to Token::BeginInterpolation

    val testSeq = arrayOf(
        endCommand, endEmit,
        const, number,
        operator, space,
        identifier
    )
}

private val singleCharTokens = mapOf(
    '.'  to Token::Dot,
    ','  to Token::Comma,
    ':'  to Token::Colon,
    '='  to Token::Assign,
    '\'' to Token::SingleQuote,
    '"'  to Token::DoubleQuote,
    '('  to Token::LPar,
    ')'  to Token::RPar,
    '['  to Token::LBracket,
    ']'  to Token::RBracket,
    '{'  to Token::LBrace,
    '}'  to Token::RBrace,
)

private val textEscapeTokenizer = RegexTokenizer(
    Patterns.beginComment, Patterns.beginEmit, Patterns.beginCommand
)

private val singleEscapeTokenizer = RegexTokenizer(
    Patterns.singleQuote,
    Patterns.uEscape, Patterns.escape, Patterns.invEscape
)

private val doubleEscapeTokenizer = RegexTokenizer(
    Patterns.doubleQuote, Patterns.interpolation,
    Patterns.uEscape, Patterns.escape, Patterns.invEscape
)

class Tokenizer(
    override val input: String,
    override val path: UPath? = null,
    val tokenInspector: ((Token) -> Unit)? = null
) : TemplateTokenizer {
    private var startIndex: Int = 0

    override fun tokenizeInitial(): Pair<Token, Token?> {
        val t2 = textEscapeTokenizer.find(input, startIndex)
        val t1 = if (t2 == null) {
            Token.Text(startIndex until input.length)
        } else {
            Token.Text(startIndex until t2.first)
        }
        tokenInspector?.let {
            it(t1)
            if (t2 != null) {
                it(t2)
            }
        }
        startIndex = (t2 ?: t1).last + 1
        return t1 to t2
    }

    override fun tokenizeEndComment(): Pair<Token, Token> {
        val mr = Patterns.endComment.first.find(input, startIndex)
            ?: return Token.Text(startIndex until input.length) to
                    Token.End(input)
        val t1 = Token.Text(startIndex until mr.range.first)
        val t2 = Token.EndComment(mr.range.first..mr.range.last)
        tokenInspector?.let { it(t1); it(t2) }
        startIndex = t2.last + 1
        return t1 to t2
    }

    override fun tokenizeSingleString() = tokenizeString(singleEscapeTokenizer)

    override fun tokenizeDoubleString() = tokenizeString(doubleEscapeTokenizer)

    private fun tokenizeString(tokenizer: RegexTokenizer): Pair<Token, Token> {
        val t2 = tokenizer.find(input, startIndex)
            ?: return Token.Text(startIndex until input.length) to
                    Token.End(input)
        val t1 = Token.Text(startIndex until t2.first)
        tokenInspector?.let { it(t1); it(t2) }
        startIndex = t2.last + 1
        return t1 to t2
    }

    private fun tokenize(fromIndex: Int): Token {
        if (fromIndex >= input.length) return Token.End(input)
        val t = Patterns.testSeq.firstNotNullOfOrNull { (pattern, c) ->
            pattern.matchAt(input, fromIndex)?.let { mr ->
                val single = if (mr.range.first == mr.range.last) {
                    singleCharTokens[input[mr.range.first]]
                } else {
                    null
                }
                single?.invoke(mr.range)
                    ?: c.invoke(mr.range.first..mr.range.last)
            }
        }
            ?: singleCharTokens[input[fromIndex]]
                ?.invoke(fromIndex .. fromIndex)
            ?: Patterns.word.first.matchAt(input, fromIndex)?.let { mr ->
                Token.Word(mr.range.first..mr.range.last)
            }
        // either space or word will always match
        return t!!
    }

    override fun tokenize(skipSpace: Boolean): Token {
        while (true) {
            val t = tokenize(fromIndex = startIndex)
            tokenInspector?.invoke(t)
            startIndex = t.last + 1
            if (skipSpace && t is Token.Space) {
                continue
            }
            return t
        }
    }

    override fun peek(skipSpace: Boolean): Token {
        var peekIndex = startIndex
        while (true) {
            val t = tokenize(fromIndex = peekIndex)
            peekIndex = t.last + 1
            if (skipSpace && t is Token.Space) {
                continue
            }
            return t
        }
    }

    override fun peekAfter(token: Token, skipSpace: Boolean): Token {
        var peekIndex = token.last + 1
        while (true) {
            val t = tokenize(fromIndex = peekIndex)
            peekIndex = t.last + 1
            if (skipSpace && t is Token.Space) {
                continue
            }
            return t
        }
    }

    override fun consume(t: Token) {
        if (tokenInspector != null) {
            while (startIndex < t.last + 1) {
                tokenize()
            }
            require(startIndex == t.last + 1)
        }
        startIndex = t.last + 1
    }
}

private class RegexTokenizer private constructor(
    private val regex: Regex,
    private val constructors: Array<(IntRange) -> Token>
) {
    constructor(vararg token: Pair<Regex, (IntRange) -> Token>) : this(
        token.joinToString(")|(", prefix = "(", postfix = ")") {
            it.first.pattern
        }.toRegex(),
        token.map { it.second }.toTypedArray()
    )

    fun find(input: CharSequence, startIndex: Int): Token? {
        val mr = regex.find(input, startIndex) ?: return null
        for (i in constructors.indices) {
            if (mr.groupValues[i + 1].isNotBlank()) {
                return constructors[i].invoke(
                    mr.range.first .. mr.range.last
                )
            }
        }
        return null
    }
}
