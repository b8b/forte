package org.cikit.forte.core

/**
 * Transpile glob like patterns to regex.
 *
 * * `?` (not between brackets) matches any single character.
 * * `*` (not between brackets) matches any string, including the empty string.
 * * `[...]` matches a single character from the defined set (character class).
 * 
 * # Character encoding
 *
 * The implementation is based on 16 bit characters. Named character classes
 * match on ASCII characters only. For individual characters and ranges within
 * a class, surrogate pairs are treated as 2 individual characters and may 
 * lead to unexpected results.
 *
 * `?` matches any single 16 bit character. Surrogate pairs within the input are
 * treated as 2 individual characters.
 * 
 * In general. it is recommended to initialize input and pattern with 
 * ASCII characters only to avoid any confusion. We might add a feature to 
 * enforce ASCII only at least within character classes in the future.
 *
 * # Compatibility
 * 
 * The implementation may reject a pattern (by throwing an exception) in 
 * some cases while other implementations are crafted to always come up with 
 * an interpretation. One such case is an unsupported named character class.
 * 
 * Some implementations provide the named class `[[:word:]]` which is
 * equivalent to `[[:alnum:]_]`.
 *
 */
class Glob(
    val pattern: String,
    val flavor: Flavor = Flavor.Default,
    val ignoreCase: Boolean = false
) {
    val matchingPower: MatchingPower
    val matchingProperties: MatchingProperties
    val regexPattern: Result<String>
    val regex: Result<Regex>

    init {
        val source = Scanner(pattern)
        regexPattern = kotlin.runCatching { convertPattern(source) }
        regex = runCatching {
            if (ignoreCase) {
                regexPattern.getOrThrow().toRegex(RegexOption.IGNORE_CASE)
            } else {
                regexPattern.getOrThrow().toRegex()
            }
        }
        matchingPower = source.matchingPower ?: MatchingPower.ALL
        matchingProperties = MatchingProperties(
            requireAbsolutePath = source.requireAbsolutePath,
            requireRelativePath = source.requireRelativePath,
            requireDirectory = source.requireDirectory,
            requireNormalized = source.requireNormalized
        )
    }

    fun toRegex() = regex.getOrThrow()

    enum class MatchingPower {
        ALL,
        ONE,
        SOME
    }

    data class MatchingProperties(
        val requireAbsolutePath: Boolean = false,
        val requireRelativePath: Boolean = false,
        val requireDirectory: Boolean = false,
        val requireNormalized: Boolean = true
    )

    sealed class Flavor(
        /**
         * allow `**` to match intermediate path segments and
         * stop matching the path separator `/` by wildcards.
         */
        val matchPathName: Boolean,

        /**
         * allow `\` to disable the special meaning of the
         * following character.
         */
        val backslashEscapes: Boolean,

        /**
         * allow `[^...]` as an alternative syntax to `[!...]`
         */
        val regexNegate: Boolean,
    ) {
        data object Default : Flavor(
            matchPathName = false,
            backslashEscapes = true,
            regexNegate = true
        )

        data object Git : Flavor(
            matchPathName = true,
            backslashEscapes = true,
            regexNegate = true
        )

        class Build(
            matchPathName: Boolean = Default.matchPathName,
            backslashEscapes: Boolean = Default.backslashEscapes,
            regexNegate: Boolean = Default.regexNegate,
        ) : Flavor(
            matchPathName = matchPathName,
            backslashEscapes = backslashEscapes,
            regexNegate = regexNegate
        )

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Flavor) return false

            if (matchPathName != other.matchPathName) return false
            if (backslashEscapes != other.backslashEscapes) return false
            if (regexNegate != other.regexNegate) return false

            return true
        }

        override fun hashCode(): Int {
            var result = matchPathName.hashCode()
            result = 31 * result + backslashEscapes.hashCode()
            result = 31 * result + regexNegate.hashCode()
            return result
        }

        override fun toString(): String {
            return buildString {
                append("Flavor(matchPathName=")
                append(matchPathName)
                append(", backslashEscapes=")
                append(backslashEscapes)
                append(", regexNegate=")
                append(regexNegate)
                append(")")
            }
        }
    }

    private companion object {

        val rangeDigit = arrayOf('0'.code .. '9'.code)

        val rangeLower = arrayOf('a'.code .. 'z'.code)

        val rangeUpper = arrayOf('A'.code .. 'Z'.code)

        val rangeAlpha = arrayOf(*rangeUpper, *rangeLower)

        val rangeAlnum = arrayOf(*rangeDigit, *rangeUpper, *rangeLower)

        val rangeBlank = arrayOf(
            '\t'.code .. '\t'.code,
            ' '.code .. ' '.code
        )

        val rangeCntrl = arrayOf(
            0x00 .. 0x1F,
            0x7F .. 0x7F
        )

        val rangeGraph = arrayOf(0x21 .. 0x7E)

        val rangePrint = arrayOf(0x20 .. 0x7E)

        val rangePunct = arrayOf(
            0x21 .. 0x2F,
            0x3A .. 0x40,
            0x5B .. 0x60,
            0x7B .. 0x7E
        )

        val rangeSpace = arrayOf(
            '\t'.code .. '\t'.code,
            '\n'.code .. '\n'.code,
            '\r'.code .. '\r'.code,
            ' '.code .. ' '.code
        )

        val rangeXDigit = arrayOf(
            *rangeDigit,
            'A'.code .. 'F'.code,
            'a'.code .. 'f'.code,
        )

        val rangeByName = mapOf(
            ":alnum:"  to rangeAlnum,
            ":alpha:"  to rangeAlpha,
            ":blank:"  to rangeBlank,
            ":cntrl:"  to rangeCntrl,
            ":digit:"  to rangeDigit,
            ":graph:"  to rangeGraph,
            ":lower:"  to rangeLower,
            ":print:"  to rangePrint,
            ":punct:"  to rangePunct,
            ":space:"  to rangeSpace,
            ":upper:"  to rangeUpper,
            ":xdigit:" to rangeXDigit,
        )

        fun convertNamedSet(name: String, exclude: Char? = null): String {
            val ranges = rangeByName[name]
                ?: error("unsupported named character class: [$name]")
            val output = StringBuilder()
            for (r in ranges) {
                var r1 = r
                var r2 = IntRange.EMPTY
                if (exclude != null) {
                    when (val x = exclude.code) {
                        r.first -> { r1 = x.inc() .. r.last }
                        r.last  -> { r1 = r.first until r.last }
                        in r    -> {
                            r1 = r.first until x
                            r2 = x.inc() .. r.last
                        }
                    }
                }
                if (!r1.isEmpty()) {
                    output.append("\\x")
                    output.append(r1.first.hex2())
                    if (r1.first != r1.last) {
                        output.append("-\\x")
                        output.append(r1.last.hex2())
                    }
                }
                if (!r2.isEmpty()) {
                    output.append("\\x")
                    output.append(r2.first.hex2())
                    if (r2.first != r2.last) {
                        output.append("-\\x")
                        output.append(r2.last.hex2())
                    }
                }
            }
            return output.toString()
        }

        private fun Int.hex2(): String {
            val sb = StringBuilder()
            sb.append("0")
            sb.append(toString(16))
            return sb.substring(sb.length - 2)
        }

        private fun Int.hex4(): String {
            val sb = StringBuilder()
            sb.append("0000")
            sb.append(toString(16))
            return sb.substring(sb.length - 4)
        }
    }

    private inner class Scanner(val input: CharSequence) {
        var index = 0
        var allowDoubleStar = true
        var pathSeparatorCount = 0
        var matchingPower: MatchingPower? = null
        var requireAbsolutePath: Boolean = false
        var requireRelativePath: Boolean = false
        var requireDirectory: Boolean = false
        var requireNormalized: Boolean = true

        fun hasNext() = index < input.length

        fun next(): Char? {
            if (index < input.length) {
                return input[index++]
            }
            return null
        }

        fun peek(): Char? {
            if (index < input.length) {
                return input[index]
            }
            return null
        }

        fun haveLiteral() {
            matchingPower = when (matchingPower) {
                null -> MatchingPower.ONE
                MatchingPower.ALL -> MatchingPower.SOME
                MatchingPower.ONE -> MatchingPower.ONE
                MatchingPower.SOME -> MatchingPower.SOME
            }
        }

        fun haveWildcard() {
            matchingPower = MatchingPower.SOME
        }

        fun haveMatchAll() {
            matchingPower = when (matchingPower) {
                null -> MatchingPower.ALL
                MatchingPower.ALL -> MatchingPower.ALL
                MatchingPower.ONE -> MatchingPower.SOME
                MatchingPower.SOME -> MatchingPower.SOME
            }
        }
    }

    private val wildcard
        get() = if (flavor.matchPathName) "[^/]" else "."

    private fun convertPattern(source: Scanner): String {
        val target = StringBuilder()
        while (source.hasNext()) {
            when (val ch = source.next()) {
                '?' -> {
                    source.allowDoubleStar = false
                    source.haveWildcard()
                    target.append(wildcard)
                }
                '*' -> {
                    if (source.peek() == '*') {
                        source.next()
                        while (source.peek() == '*') {
                            source.next()
                        }
                        if (flavor.matchPathName && source.allowDoubleStar) {
                            when (source.peek()) {
                                null -> {
                                    source.haveMatchAll()
                                    target.append(".*")
                                    continue
                                }
                                '/' -> {
                                    source.haveMatchAll()
                                    target.append("(.*?/)*")
                                    source.next()
                                    source.pathSeparatorCount++
                                    continue
                                }
                            }
                        }
                    }
                    if (flavor.matchPathName) {
                        source.haveWildcard()
                    } else {
                        source.haveMatchAll()
                    }
                    target.append(wildcard)
                    target.append("*")
                    source.allowDoubleStar = false
                }
                '[' -> {
                    source.allowDoubleStar = false
                    convertSet(source, target)
                }
                '\\' -> {
                    require(source.peek() != Separator) {
                        "escaped separator"
                    }
                    source.haveLiteral()
                    source.allowDoubleStar = false
                    if (flavor.backslashEscapes) {
                        val next = source.next()
                        require(next != null) {
                            "invalid escape sequence"
                        }
                        target.append("\\u")
                        target.append(next.code.hex4())
                    } else {
                        target.append("\\\\")
                    }
                }
                ']', '{', '}', '$', '^', '+', '.', '(', ')', '|' -> {
                    source.haveLiteral()
                    source.allowDoubleStar = false
                    target.append("\\")
                    target.append(ch)
                }
                '/' -> {
                    if (target.isEmpty()) {
                        source.requireAbsolutePath = true
                    } else if (target.last() == Separator) {
                        source.requireNormalized = false
                    }
                    source.haveLiteral()
                    target.append(ch)
                    source.allowDoubleStar = true
                    if (source.peek() == null) {
                        source.requireDirectory = true
                    } else {
                        source.pathSeparatorCount++
                    }
                }

                else -> {
                    source.haveLiteral()
                    source.allowDoubleStar = false
                    target.append(ch)
                }
            }
        }
        if (source.pathSeparatorCount > 0 && !source.requireAbsolutePath) {
            source.requireRelativePath = true
        }
        return target.toString()
    }

    private fun convertSet(
        source: Scanner,
        target: StringBuilder,
    ) {
        val tmp = StringBuilder()
        val patterns = mutableSetOf<String>()
        var negate = false
        var isFirst = true
        var haveEnd = false
        val chars = mutableSetOf<Char>()
        while (true) {
            var ch = source.next() ?: break
            if (isFirst) {
                if (ch == '!' || (flavor.regexNegate && ch == '^')) {
                    source.haveWildcard()
                    negate = true
                    ch = source.next() ?: break
                }
                if (ch == ']') {
                    chars.add(ch)
                    tmp.append("\\u")
                    tmp.append(ch.code.hex4())
                    ch = source.next() ?: break
                }
                isFirst = false
            }
            when (ch) {
                ']' -> {
                    haveEnd = true
                    break
                }
                '[' -> when (source.peek()) {
                    ':', '.', '=' -> {
                        source.haveWildcard()
                        val name = StringBuilder()
                        while (source.hasNext()) {
                            val next = source.next()
                            if (next == ']') {
                                break
                            }
                            name.append(ch)
                        }
                        val pattern = convertNamedSet(
                            name.toString(),
                            if (negate || !flavor.matchPathName) null else '/'
                        )
                        patterns.add(pattern)
                    }

                    else -> {
                        chars.add(ch)
                        tmp.append("\\u")
                        tmp.append(ch.code.hex4())
                    }
                }
                '/' -> if (!flavor.matchPathName) {
                    chars.add(ch)
                    tmp.append(ch)
                }
                '^' -> {
                    chars.add(ch)
                    tmp.append("\\u")
                    tmp.append(ch.code.hex4())
                }
                '\\' -> {
                    val next = source.next()
                    require(next != null) {
                        "invalid escape sequence"
                    }
                    chars.add(next)
                    tmp.append("\\u")
                    tmp.append(next.code.hex4())
                }

                else -> {
                    chars.add(ch)
                    tmp.append(ch)
                }
            }
        }
        require(haveEnd) {
            "unclosed character class"
        }
        require(tmp.isNotEmpty() || patterns.isNotEmpty()) {
            "empty character class"
        }
        if (negate) {
            target.append("[^")
            if (flavor.matchPathName) {
                target.append("/")
            }
        } else {
            target.append("[")
        }
        target.append(tmp)
        for (p in patterns) {
            target.append(p)
        }
        target.append("]")
        if (chars.size > 1) {
            source.haveWildcard()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Glob) return false

        if (pattern != other.pattern) return false
        if (flavor != other.flavor) return false
        if (ignoreCase != other.ignoreCase) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pattern.hashCode()
        result = 31 * result + flavor.hashCode()
        result = 31 * result + ignoreCase.hashCode()
        return result
    }

    override fun toString(): String {
        return "Glob(pattern='$pattern')"
    }
}
