package org.cikit.forte.core

import kotlinx.io.bytestring.ByteString

sealed class GlobMatcher(private val glob: Glob) : UPathMatcher {

    val flavor: Glob.Flavor
        get() = glob.flavor

    val ignoreCase: Boolean
        get() = glob.ignoreCase

    val matchingPower: Glob.MatchingPower
        get() = glob.matchingPower

    val matchingProperties: Glob.MatchingProperties
        get() = glob.matchingProperties

    private class None(glob: Glob) : GlobMatcher(glob) {
        override fun matches(path: UPath): Boolean = false
    }

    private class All(glob: Glob) : GlobMatcher(glob) {
        override fun matches(path: UPath): Boolean = true
    }

    private class Some(private val glob: Glob) : GlobMatcher(glob) {
        override fun matches(path: UPath): Boolean {
            return glob.toRegex().matches(IsoCharSequence(path.encoded))
        }
    }

    companion object {
        internal fun create(
            pattern: ByteString,
            flavor: Glob.Flavor = Glob.Flavor.Default,
            ignoreCase: Boolean = false
        ): GlobMatcher {
            val glob = Glob(IsoCharSequence(pattern), flavor, ignoreCase)
            return if (glob.regex.isSuccess) {
                when (glob.matchingPower) {
                    Glob.MatchingPower.ALL -> All(glob)
                    Glob.MatchingPower.ONE,
                    Glob.MatchingPower.SOME -> Some(glob)
                }
            } else {
                None(glob)
            }
        }
    }
}

fun UPath.asGLobMatcher(
    flavor: Glob.Flavor = Glob.Flavor.Default,
    ignoreCase: Boolean = false
) = GlobMatcher.create(encoded, flavor, ignoreCase)

private class IsoCharSequence private constructor(
    val encoded: ByteString,
    val startIndex: Int,
    val endIndex: Int
) : CharSequence {
    constructor(encoded: ByteString) : this(
        encoded = encoded,
        startIndex = 0,
        endIndex = encoded.size
    )

    override val length: Int
        get() = endIndex - startIndex

    override fun get(index: Int): Char {
        require(index in 0 until length) {
            "index $index out of bounds (0 until $length)"
        }
        return Char(encoded[startIndex + index].toUByte().toInt())
    }

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        require(startIndex in 0 until length) {
            "startIndex $startIndex out of bounds (0 until $length)"
        }
        require(endIndex in startIndex .. length) {
            "endIndex $endIndex out of bound ($startIndex .. $length)"
        }
        return IsoCharSequence(
            encoded,
            this.startIndex + startIndex,
            this.startIndex + endIndex
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as IsoCharSequence

        return encoded == other.encoded &&
                startIndex == other.startIndex &&
                endIndex == other.endIndex
    }

    override fun hashCode(): Int {
        var result = 1

        for (i in startIndex until endIndex) {
            result = result * 31 + encoded[i].toInt()
        }

        return result
    }

    override fun toString(): String {
        val charArray = CharArray(endIndex - startIndex) { i ->
            Char(encoded[i].toUByte().toInt())
        }
        return charArray.concatToString()
    }
}
