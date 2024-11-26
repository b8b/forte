package org.cikit.forte.core

import kotlinx.io.bytestring.*

const val Separator = '/'

private const val SeparatorByte = Separator.code.toByte()
private val EmptyByteString = ByteString()

class UPath private constructor (
    val encoded: ByteString,
    utf8: Boolean
) {
    var isUtf8Only: Boolean = utf8
        private set

    constructor() : this(EmptyByteString)

    constructor(encoded: ByteString) : this(encoded, false)

    constructor(utfPath: String) : this(
        encoded = utfPath.encodeToByteString(),
        utf8 = true
    )

    constructor(base: String, vararg parts: String) : this(
        utfPath = buildString {
            append(base)
            while (isNotEmpty() && last() == Separator) {
                setLength(length - 1)
            }
            for (part in parts) {
                append(Separator)
                append(part.trim(Separator))
                while (isNotEmpty() && last() == Separator) {
                    setLength(length - 1)
                }
            }
        }
    )

    constructor(base: UPath, vararg parts: String) : this(
        encoded = base.resolveParts(*parts).encoded,
        utf8 = base.isUtf8Only
    )

    constructor(base: String, decodeEscapes: Boolean) : this(
        base,
        when (decodeEscapes) {
            true -> DecodeEscapes
            else -> UPathDecoder { s -> s.encodeToByteString() }
        }
    )

    constructor(base: String, decoder: UPathDecoder) : this(
        encoded = decoder.decode(base),
        utf8 = false
    )

    init {
        if (encoded !== EmptyByteString) {
            require(encoded.indexOf(0) < 0) {
                "invalid null byte in UPath"
            }
        }
    }

    override fun toString(): String {
        return encoded.decodeToString()
    }

    fun toUrlPath(): String = buildString {
        for (i in encoded.indices) {
            val b = encoded[i].toInt()
            if (b < 0) {
                append('%')
                val hex = (b and 0xFF).toString(16)
                if (hex.length == 1) {
                    append("0")
                }
                append(hex)
            } else {
                append(b.toChar())
            }
        }
    }

    fun toVisualPath(): String = buildString {
        for (i in encoded.indices) {
            val b = encoded[i].toInt()
            when (val ch = b.toChar()) {
                '\\' -> append("\\\\")

                in '\u0021' .. '\u007e' -> append(ch)

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
    }

    operator fun div(other: UPath) = resolve(other)

    operator fun div(utfPath: String) = resolveParts(utfPath)

    fun resolve(utfPath: String): UPath = resolveParts(utfPath)

    fun resolve(other: UPath): UPath {
        var thisLength = encoded.size
        while (thisLength > 0 && encoded[thisLength - 1] == SeparatorByte) {
            thisLength--
        }
        var otherStart = 0
        while (otherStart < other.encoded.size &&
            other.encoded[otherStart] == SeparatorByte)
        {
            otherStart++
        }
        val builder = ByteStringBuilder(
            thisLength + other.encoded.size - otherStart + 1
        )
        builder.append(encoded.substring(0, thisLength))
        builder.append(SeparatorByte)
        builder.append(other.encoded.substring(otherStart))
        return UPath(builder.toByteString())
    }

    private fun resolveParts(vararg parts: String): UPath {
        return resolve(UPath(parts.joinToString("/")))
    }
}

fun interface UPathDecoder {
    fun decode(input: String): ByteString
}

object DecodeUrlPath : UPathDecoder {
    override fun decode(input: String): ByteString {
        val builder = ByteStringBuilder(input.length)
        var isFirst = true
        for (part in input.splitToSequence('%')) {
            if (isFirst) {
                builder.append(part.encodeToByteString())
                isFirst = false
                continue
            }
            require(part.length >= 2) {
                "invalid url escape"
            }
            val code = part.substring(0, 2).toInt(16)
            builder.append(code.toByte())
            if (part.length > 2) {
                builder.append(part.substring(2).encodeToByteString())
            }
        }
        return builder.toByteString()
    }
}

object DecodeEscapes : UPathDecoder {
    override fun decode(input: String): ByteString {
        val builder = ByteStringBuilder(input.length)
        var isFirst = true
        for (part in input.splitToSequence('\\')) {
            if (isFirst) {
                builder.append(part.encodeToByteString())
                isFirst = false
                continue
            }
            if (part.isEmpty()) {
                builder.append('\\'.code.toByte())
                isFirst = true
                continue
            }
            var escapeLength = 1
            when (val ch = part.first()) {
                'a' -> builder.append(0x07)
                'b' -> builder.append(0x08)
                't' -> builder.append(0x09)
                'n' -> builder.append(0x0a)
                'v' -> builder.append(0x0b)
                'f' -> builder.append(0x0c)
                'r' -> builder.append(0x0d)
                in '0' .. '7' -> {
                    require(part.length >= 3) {
                        "invalid octal escape"
                    }
                    val code = part.substring(0, 3).toInt(8)
                    builder.append(code.toByte())
                    escapeLength = 3
                }

                else -> builder.append(ch.code.toByte())
            }
            if (part.length > escapeLength) {
                builder.append(
                    part.substring(escapeLength).encodeToByteString()
                )
            }
        }
        return builder.toByteString()
    }
}
