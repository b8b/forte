package org.cikit.forte.lib.js

import com.ionspin.kotlin.bignum.integer.BigInteger
import dev.erikchristensen.javamath2kmp.minusExact
import dev.erikchristensen.javamath2kmp.plusExact
import dev.erikchristensen.javamath2kmp.timesExact
import org.cikit.forte.core.NumericValue
import org.cikit.forte.core.typeName
import kotlin.math.pow

class IntNumericValue(
    override val value: Any?,
    val converted: Int
) : Number(), NumericValue {

    override val isInt: Boolean
        get() = true

    override val isFloat: Boolean
        get() = false

    override val hasDecimalPart: Boolean
        get() = false

    override fun plus(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            try {
                val newValue = converted.plusExact(other.converted)
                IntNumericValue(newValue, newValue)
            } catch (_: ArithmeticException) {
                val newValue = BigInteger.fromInt(converted)
                    .plus(other.converted)
                BigNumericValue(newValue)
            }
        }

        is BigNumericValue -> {
            val newValue = other.converted.plus(converted)
            BigNumericValue(newValue)
        }

        is FloatNumericValue -> {
            val newValue = other.converted.plus(converted)
            FloatNumericValue(newValue, newValue)
        }

        else -> error(
            "binary operator plus is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun minus(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            try {
                val newValue = converted.minusExact(other.converted)
                IntNumericValue(newValue, newValue)
            } catch (_: ArithmeticException) {
                val newValue = BigInteger.fromInt(converted)
                    .minus(other.converted)
                BigNumericValue(newValue)
            }
        }

        is BigNumericValue -> {
            val newValue = BigInteger.fromInt(converted).minus(other.converted)
            BigNumericValue(newValue)
        }

        is FloatNumericValue -> {
            val newValue = converted.toDouble() - other.converted
            FloatNumericValue(newValue, newValue)
        }

        else -> error(
            "binary operator minus is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun mul(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            try {
                val newValue = converted.timesExact(other.converted)
                IntNumericValue(newValue, newValue)
            } catch (_: ArithmeticException) {
                val newValue = BigInteger.fromInt(converted)
                    .multiply(BigInteger.fromInt(other.converted))
                BigNumericValue(newValue)
            }
        }

        is BigNumericValue -> {
            val newValue = BigInteger.fromInt(converted)
                .multiply(other.converted)
            BigNumericValue(newValue)
        }

        is FloatNumericValue -> {
            val newValue = other.converted * converted
            FloatNumericValue(newValue, newValue)
        }

        else -> error(
            "binary operator mul is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun div(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            val newValue = converted / other.converted
            if (newValue * other.converted == converted) {
                IntNumericValue(newValue, newValue)
            } else {
                val newFloat = converted.toDouble() / other.converted
                FloatNumericValue(newFloat, newFloat)
            }
        }

        is BigNumericValue -> {
            val big = BigInteger.fromInt(converted)
            val newValue = big.div(other.converted)
            if (newValue.multiply(other.converted) == big) {
                BigNumericValue(newValue)
            } else {
                val newFloat = converted.toDouble() /
                        other.converted.doubleValue()
                FloatNumericValue(newFloat, newFloat)
            }
        }

        is FloatNumericValue -> {
            val newValue = converted.toDouble() / other.converted
            FloatNumericValue(newValue, newValue)
        }

        else -> error(
            "binary operator div is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun tdiv(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            val newValue = converted / other.converted
            return IntNumericValue(newValue, newValue)
        }

        is BigNumericValue -> {
            val big = BigInteger.fromInt(converted)
            val newValue = big.div(other.converted)
            return BigNumericValue(newValue)
        }

        else -> error(
            "binary operator tdiv is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun rem(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            val newValue = converted.rem(other.converted)
            return IntNumericValue(newValue, newValue)
        }

        is BigNumericValue -> {
            val newValue = BigInteger.fromInt(converted).rem(other.converted)
            return BigNumericValue(newValue)
        }

        is FloatNumericValue -> {
            val newValue = converted.rem(other.converted)
            FloatNumericValue(newValue, newValue)
        }

        else -> error(
            "binary operator rem is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun pow(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            try {
                val newValue = powExact(converted, other.converted)
                IntNumericValue(newValue, newValue)
            } catch (_: ArithmeticException) {
                val newValue = BigInteger.fromInt(converted)
                    .pow(BigInteger.fromInt(other.converted))
                BigNumericValue(newValue)
            }
        }

        is BigNumericValue -> {
            val newValue = BigInteger.fromInt(converted).pow(other.converted)
            BigNumericValue(newValue)
        }

        is FloatNumericValue -> {
            val newValue = converted.toDouble().pow(other.converted)
            FloatNumericValue(newValue, newValue)
        }

        else -> error(
            "binary operator pow is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    private fun powExact(base: Int, exp: Int): Int {
        if (exp < 0) throw ArithmeticException("Negative exponent")
        var result = 1
        var exponent = exp
        var b = base
        while (exponent > 0) {
            if (exponent and 1 != 0) {
                result = result.timesExact(b)
            }
            b = b.timesExact(b)
            exponent = exponent ushr 1
        }
        return result
    }

    override fun toIntValue(): NumericValue = this

    override fun toFloatValue(): NumericValue {
        return FloatNumericValue(value, converted.toDouble())
    }

    override fun toStringValue(): CharSequence {
        return converted.toString()
    }

    override fun toDouble(): Double = converted.toDouble()

    override fun toFloat(): Float = converted.toFloat()

    override fun toLong(): Long = converted.toLong()

    override fun toInt(): Int = converted

    override fun toShort(): Short = converted.toShort()

    override fun toByte(): Byte = converted.toByte()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as IntNumericValue

        return converted == other.converted
    }

    override fun hashCode(): Int {
        return converted
    }

    override fun toString(): String {
        return "IntNumericValue($converted)"
    }
}
