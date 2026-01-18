package org.cikit.forte.lib.jvm

import org.cikit.forte.core.NumericValue
import org.cikit.forte.core.typeName
import java.math.BigInteger
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
                val newValue = Math.addExact(converted, other.converted)
                IntNumericValue(newValue, newValue)
            } catch (_: ArithmeticException) {
                val newValue = BigInteger.valueOf(converted.toLong()).plus(
                    BigInteger.valueOf(other.converted.toLong())
                )
                BigNumericValue(newValue, newValue)
            }
        }

        is BigNumericValue -> {
            val newValue = converted.toBigInteger().plus(other.converted)
            BigNumericValue(newValue, newValue)
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
                val newValue = Math.subtractExact(converted, other.converted)
                IntNumericValue(newValue, newValue)
            } catch (_: ArithmeticException) {
                val newValue = BigInteger.valueOf(converted.toLong()).minus(
                    BigInteger.valueOf(other.converted.toLong())
                )
                BigNumericValue(newValue, newValue)
            }
        }

        is BigNumericValue -> {
            val newValue = converted.toBigInteger().minus(other.converted)
            BigNumericValue(newValue, newValue)
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
                val newValue = Math.multiplyExact(converted, other.converted)
                IntNumericValue(newValue, newValue)
            } catch (_: ArithmeticException) {
                val newValue = BigInteger.valueOf(converted.toLong()).multiply(
                    BigInteger.valueOf(other.converted.toLong())
                )
                BigNumericValue(newValue, newValue)
            }
        }

        is BigNumericValue -> {
            val newValue = converted.toBigInteger().multiply(other.converted)
            BigNumericValue(newValue, newValue)
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
            val big = converted.toBigInteger()
            val newValue = big.div(other.converted)
            if (newValue.multiply(other.converted) == big) {
                BigNumericValue(newValue, newValue)
            } else {
                val newFloat = converted.toDouble() /
                        other.converted.toDouble()
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
            val big = converted.toBigInteger()
            val newValue = big.div(other.converted)
            return BigNumericValue(newValue, newValue)
        }

        else -> error(
            "binary operator tdiv is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun rem(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            val newValue = converted.rem(other.converted)
            IntNumericValue(newValue, newValue)
        }

        is FloatNumericValue -> {
            val newValue = converted.rem(other.converted)
            FloatNumericValue(newValue, newValue)
        }

        is BigNumericValue -> {
            val newValue = converted.toBigInteger().rem(other.converted)
            BigNumericValue(newValue, newValue)
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
                return IntNumericValue(newValue, newValue)
            } catch (_: ArithmeticException) {
                val newValue = BigInteger.valueOf(converted.toLong())
                    .pow(other.converted)
                return BigNumericValue(newValue, newValue)
            }
        }

        is FloatNumericValue -> {
            val newValue = converted.toDouble().pow(other.converted)
            return FloatNumericValue(newValue, newValue)
        }

        is BigNumericValue -> {
            val newValue = BigInteger.valueOf(converted.toLong()).pow(
                other.converted.intValueExact()
            )
            return BigNumericValue(newValue, newValue)
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
                result = Math.multiplyExact(result, b)
            }
            b = Math.multiplyExact(b, b)
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
