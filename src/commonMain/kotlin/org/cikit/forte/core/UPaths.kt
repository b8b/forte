package org.cikit.forte.core

import kotlinx.io.bytestring.*

const val Separator = '/'

private const val BYTE_SEPARATOR = Separator.code.toByte()
private const val BYTE_DOT = '.'.code.toByte()

private val emptyByteString = ByteString()
private val emptyUPath = UPath(emptyByteString)
private val rootUPath = UPath("/")

class UPath private constructor (
    val encoded: ByteString,
    utf8: Boolean
) {
    var isUtf8Only: Boolean = utf8
        private set

    constructor() : this(
        encoded = emptyByteString,
        utf8 = true
    )

    constructor(encoded: ByteString) : this(
        encoded = encoded,
        utf8 = encoded.isEmpty()
    )

    constructor(utfPath: String) : this(
        encoded = if (utfPath.isEmpty()) {
            emptyByteString
        } else {
            utfPath.encodeToByteString()
        },
        utf8 = true
    )

    constructor(base: String, vararg parts: String) : this(
        encoded = UPath(base).append(*parts).encoded,
        utf8 = true
    )

    constructor(base: UPath, vararg parts: String) : this(
        encoded = base.append(*parts).encoded,
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
        if (encoded !== emptyByteString) {
            require(encoded.indexOf(0) < 0) {
                "invalid null byte in UPath"
            }
        }
    }

    override fun toString(): String {
        return encoded.decodeToString()
    }

    val pathString: String
        get() = encoded.decodeToString()

    fun toUrlPath(): String = buildString {
        for (i in encoded.indices) {
            val b = encoded[i].toInt()
            if (b == '/'.code ||
                b == '.'.code ||
                b in '0'.code .. '9'.code ||
                b in 'a'.code .. 'z'.code ||
                b in 'A'.code .. 'Z'.code) {
                append(b.toChar())
            } else {
                append('%')
                val hex = (b and 0xFF).toString(16).uppercase()
                if (hex.length == 1) {
                    append("0")
                }
                append(hex)
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

    val segments: Sequence<UPath>
        get() {
            if (this === emptyUPath) {
                // an empty path has a single segment
                return sequenceOf(this)
            }
            val size = encoded.size
            var startIndex = 0
            var i = 0
            if (i >= size) {
                // an empty path has a single segment
                return sequenceOf(emptyUPath)
            }
            return sequence {
                while (true) {
                    // skip until first slash
                    while (encoded[i] != BYTE_SEPARATOR) {
                        i++
                        if (i >= encoded.size) {
                            // reached last segment (no slashes)
                            yield(
                                UPath(
                                    encoded = encoded.substring(startIndex, i),
                                    utf8 = isUtf8Only
                                )
                            )
                            return@sequence
                        }
                    }
                    if (i > startIndex) {
                        yield(
                            UPath(
                                encoded = encoded.substring(startIndex, i),
                                utf8 = isUtf8Only
                            )
                        )
                    }
                    while (encoded[i] == BYTE_SEPARATOR) {
                        i++
                        if (i >= encoded.size) {
                            return@sequence
                        }
                    }
                    startIndex = i
                }
            }
        }

    val lastSegment: UPath?
        get() {
            if (this === emptyUPath) {
                // an empty path has a single segment
                return this
            }
            var i = encoded.size - 1
            if (i < 0) {
                // an empty path has a single segment
                return emptyUPath
            }
            // skip trailing slashes
            while (encoded[i] == BYTE_SEPARATOR) {
                i--
                if (i < 0) {
                    // path is root (only slashes)
                    return null
                }
            }
            if (i == 0) {
                return UPath(
                    encoded = encoded.substring(0, i + 1),
                    utf8 = isUtf8Only
                )
            }
            // i = index of the last byte of the last segment
            val endIndex = i + 1
            i--
            while (encoded[i] != BYTE_SEPARATOR) {
                i--
                if (i < 0) {
                    return UPath(
                        encoded = encoded.substring(0, endIndex),
                        utf8 = isUtf8Only
                    )
                }
            }
            return UPath(
                encoded = encoded.substring(i + 1, endIndex),
                utf8 = isUtf8Only
            )
        }

    val parent: UPath?
        get() {
            if (this === emptyUPath) {
                return null
            }
            var i = encoded.size - 1
            if (i < 0) {
                return null
            }
            // skip trailing slashes
            while (encoded[i] == BYTE_SEPARATOR) {
                i--
                if (i < 0) {
                    // path is root (only slashes)
                    return null
                }
            }
            while (encoded[i] != BYTE_SEPARATOR) {
                i--
                if (i < 0) {
                    // path is relative with a single segment
                    return null
                }
            }
            // skip trailing slashes of parent
            while (encoded[i] == BYTE_SEPARATOR) {
                i--
                if (i < 0) {
                    return rootUPath
                }
            }
            return UPath(
                encoded = encoded.substring(0, i + 1),
                utf8 = isUtf8Only
            )
        }

    val isAbsolute: Boolean
        get() = encoded.isNotEmpty() && encoded[0] == BYTE_SEPARATOR

    val isHidden: Boolean
        get() = lastSegment?.let {
            it.encoded.isNotEmpty() && it.encoded[0] == BYTE_DOT
        } ?: false

    val name: String
        get() = lastSegment?.pathString ?: ""

    val nameWithoutExtension: String
        get() = lastSegment?.let {
            val i = it.encoded.lastIndexOf(BYTE_DOT)
            if (i < 0) {
                it.encoded.decodeToString()
            } else {
                it.encoded.substring(0, i).decodeToString()
            }
        } ?: ""

    val extension: String
        get() = lastSegment?.let {
            val i = it.encoded.lastIndexOf(BYTE_DOT)
            if (i < 0) {
                ""
            } else {
                it.encoded.substring(i + 1).decodeToString()
            }
        } ?: ""

    fun withoutExtension(): UPath {
        var i = encoded.lastIndexOf(BYTE_SEPARATOR)
        if (i < 0) {
            i = 0
        }
        i = encoded.lastIndexOf(BYTE_DOT, startIndex = i)
        if (i < 0) {
            return this
        }
        return UPath(
            encoded = encoded.substring(0, i),
            utf8 = isUtf8Only
        )
    }

    fun normalize(dots: Boolean = true): UPath {
        val normalized = mutableListOf<UPath>()
        if (!dots) {
            normalized.addAll(segments)
        } else {
            for (segment in segments) {
                when (segment.name) {
                    "." -> {}
                    ".." -> if (normalized.isNotEmpty()) {
                        normalized.removeLast()
                    }

                    else -> normalized.add(segment)
                }
            }
        }
        return if (isAbsolute) {
            rootUPath.append(*normalized.toTypedArray())
        } else {
            emptyUPath.append(*normalized.toTypedArray())
        }
    }

    fun resolve(utfPath: String): UPath = resolve(UPath(utfPath))

    fun resolve(other: UPath): UPath {
        if (other.isAbsolute) {
            return other
        }
        if (other === emptyUPath || other.encoded.isEmpty()) {
            return this
        }
        if (this === emptyUPath || this.encoded.isEmpty()) {
            return other
        }
        var thisEnd = encoded.size - 1
        while (encoded[thisEnd] == BYTE_SEPARATOR) {
            thisEnd--
            if (thisEnd < 0) {
                // this is root path (only slashes)
                if (other.isAbsolute) {
                    return other
                }
                val builder = ByteStringBuilder(other.encoded.size + 1)
                builder.append(BYTE_SEPARATOR)
                builder.append(other.encoded)
                return UPath(
                    encoded = builder.toByteString(),
                    utf8 = other.isUtf8Only
                )
            }
        }
        val builder = ByteStringBuilder(
            // this length
            thisEnd + 1 +
                    // other length
                    other.encoded.size +
                    // separator
                    1
        )
        builder.append(encoded.substring(0, thisEnd + 1))
        builder.append(BYTE_SEPARATOR)
        builder.append(other.encoded)
        return UPath(
            encoded = builder.toByteString(),
            utf8 = isUtf8Only && other.isUtf8Only
        )
    }

    fun append(other: UPath): UPath {
        val right = other.encoded.trimStart()
        if (right.isEmpty()) {
            return this
        }
        val builder = appendable(right.size)
        builder.append(right)
        return UPath(
            encoded = builder.toByteString(),
            utf8 = isUtf8Only && other.isUtf8Only
        )
    }

    fun append(vararg other: UPath): UPath {
        var utf8 = isUtf8Only
        val iterator = other.iterator()
        while (iterator.hasNext()) {
            val first = iterator.next()
            val trimmed = if (iterator.hasNext()) {
                first.encoded.trim()
            } else {
                first.encoded.trimStart()
            }
            if (trimmed.isNotEmpty()) {
                val builder = appendable(trimmed.size)
                builder.append(trimmed)
                if (utf8 && !first.isUtf8Only) {
                    utf8 = false
                }
                while (iterator.hasNext()) {
                    val next = iterator.next()
                    val nextTrimmed = if (iterator.hasNext()) {
                        next.encoded.trim()
                    } else {
                        next.encoded.trimStart()
                    }
                    if (nextTrimmed.isNotEmpty()) {
                        builder.append(BYTE_SEPARATOR)
                        builder.append(nextTrimmed)
                        if (utf8 && !next.isUtf8Only) {
                            utf8 = false
                        }
                    }
                }
                return UPath(
                    encoded = builder.toByteString(),
                    utf8 = utf8
                )
            }
        }
        return this
    }

    fun append(other: String): UPath = append(UPath(other))

    fun append(vararg other: String): UPath {
        val iterator = other.iterator()
        while (iterator.hasNext()) {
            val first = iterator.next()
            val trimmed = if (iterator.hasNext()) {
                first.trim(Separator)
            } else {
                first.trimStart(Separator)
            }
            if (trimmed.isNotEmpty()) {
                val builder = StringBuilder(trimmed)
                while (iterator.hasNext()) {
                    val next = iterator.next()
                    val nextTrimmed = if (iterator.hasNext()) {
                        next.trim(Separator)
                    } else {
                        next.trimStart(Separator)
                    }
                    if (nextTrimmed.isNotEmpty()) {
                        builder.append(Separator)
                        builder.append(nextTrimmed)
                    }
                }
                return append(UPath(builder.toString()))
            }
        }
        return this
    }

    fun relativeTo(other: UPath): UPath {
        require(isAbsolute == other.isAbsolute) {
            "'other' is different type of Path"
        }
        if (encoded == emptyByteString || encoded.isEmpty()) {
            return other
        }
        // find common prefix
        val sIt = normalize().segments.iterator()
        val osIt = other.normalize().segments.iterator()
        while (sIt.hasNext() && osIt.hasNext()) {
            val s = sIt.next()
            val os = osIt.next()
            if (s != os) {
                // ... / a / ...
                // ... / b / 1 / 2 / 3
                //       ^
                val dots = "../".repeat(osIt.asSequence().count() + 1)
                return UPath(dots)
                    .append(s, *sIt.asSequence().toList().toTypedArray())
            }
        }
        if (sIt.hasNext()) {
            require(!osIt.hasNext()) { "internal error" }
            // a / c / c / 1 / 2 / 3
            // a / b / c
            //             ^
            return sIt.next().append(*sIt.asSequence().toList().toTypedArray())
        }
        if (osIt.hasNext()) {
            require(!sIt.hasNext()) { "internal error" }
            // a / b / c
            // a / c / c / 1 / 2 / 3
            //             ^
            return UPath(osIt.asSequence().joinToString("/") { ".." })
        }
        return emptyUPath
    }

    fun relativeToOrNull(other: UPath): UPath? {
        if (isAbsolute != other.isAbsolute) {
            return null
        }
        return relativeTo(other)
    }

    fun relativeToOrSelf(other: UPath): UPath {
        if (isAbsolute != other.isAbsolute) {
            return this
        }
        return relativeTo(other)
    }

    private fun ByteString.trimStart(b: Byte = BYTE_SEPARATOR): ByteString {
        var i = 0
        if (i >= size) {
            return this
        }
        while (this[i] == b) {
            i++
            if (i >= size) {
                return emptyByteString
            }
        }
        return substring(i)
    }

    private fun ByteString.trim(b: Byte = BYTE_SEPARATOR): ByteString {
        var startIndex = 0
        if (startIndex >= size) {
            return this
        }
        while (this[startIndex] == b) {
            startIndex++
            if (startIndex >= size) {
                return emptyByteString
            }
        }
        var endInclusive = size - 1
        if (startIndex == endInclusive) {
            return substring(startIndex)
        }
        while (this[endInclusive] == b) {
            endInclusive--
            if (endInclusive < startIndex) {
                return emptyByteString
            }
        }
        return substring(startIndex, endInclusive + 1)
    }

    private fun appendable(additionalCapacity: Int = 0): ByteStringBuilder {
        var i = encoded.size - 1
        if (i < 0) {
            return ByteStringBuilder(additionalCapacity)
        }
        while (encoded[i] == BYTE_SEPARATOR) {
            i--
            if (i < 0) {
                // this is root (only slashes)
                // append at index 1
                val builder = ByteStringBuilder(additionalCapacity + 1)
                builder.append(BYTE_SEPARATOR)
                return builder
            }
        }
        if (i == encoded.size - 1) {
            // this has no slash at last index
            val builder = ByteStringBuilder(
                encoded.size + 1 + additionalCapacity
            )
            builder.append(encoded)
            builder.append(BYTE_SEPARATOR)
            return builder
        }
        // reuse the slash at index i + 1
        val builder = ByteStringBuilder(i + 2 + additionalCapacity)
        builder.append(encoded.substring(0, i + 2))
        return builder
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as UPath

        return encoded == other.encoded
    }

    override fun hashCode(): Int {
        return encoded.hashCode()
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
