package org.cikit.forte.lib.jvm

import org.cikit.forte.core.ComparableValue
import org.cikit.forte.core.NumericValue
import org.cikit.forte.core.typeName
import java.math.BigInteger
import kotlin.math.pow

class BigNumericValue(
    val value: BigInteger
) : NumericValue {

    override val result: BigInteger
        get() = value

    override val isInt: Boolean
        get() = true

    override val isFloat: Boolean
        get() = false

    override val hasDecimalPart: Boolean
        get() = false

    override fun plus(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            val newValue = value.plus(other.value.toBigInteger())
            BigNumericValue(newValue)
        }

        is BigNumericValue -> {
            val newValue = value.plus(other.value)
            BigNumericValue(newValue)
        }

        is FloatNumericValue -> {
            val newValue = value.toDouble().plus(other.value)
            FloatNumericValue(newValue)
        }

        else -> error(
            "binary operator plus is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun minus(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            val newValue = value.minus(other.value.toBigInteger())
            BigNumericValue(newValue)
        }

        is BigNumericValue -> {
            val newValue = value.minus(other.value)
            BigNumericValue(newValue)
        }

        is FloatNumericValue -> {
            val newValue = value.toDouble() - other.value
            FloatNumericValue(newValue)
        }

        else -> error(
            "binary operator minus is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun mul(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            val newValue = value.multiply(other.value.toBigInteger())
            BigNumericValue(newValue)
        }

        is BigNumericValue -> {
            val newValue = value.multiply(other.value)
            BigNumericValue(newValue)
        }

        is FloatNumericValue -> {
            val newValue = value.toDouble() * other.value
            FloatNumericValue(newValue)
        }

        else -> error(
            "binary operator mul is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun div(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            val otherBig = other.value.toBigInteger()
            val newValue = value.div(otherBig)
            if (newValue.multiply(otherBig) == value) {
                BigNumericValue(newValue)
            } else {
                val newValue = value.toDouble() / other.value
                FloatNumericValue(newValue)
            }
        }

        is BigNumericValue -> {
            val newValue = value.div(other.value)
            if (newValue.multiply(other.value) == value) {
                BigNumericValue(newValue)
            } else {
                val newValue = value.toDouble() / other.value.toDouble()
                FloatNumericValue(newValue)
            }
        }

        is FloatNumericValue -> {
            val newValue = value.toDouble() / other.value
            FloatNumericValue(newValue)
        }

        else -> error(
            "binary operator div is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun tdiv(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            val newValue = value.div(other.value.toBigInteger())
            return BigNumericValue(newValue)
        }

        is BigNumericValue -> {
            val newValue = value.div(other.value)
            return BigNumericValue(newValue)
        }

        else -> error(
            "binary operator tdiv is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun rem(other: NumericValue): NumericValue = when (other) {
        is BigNumericValue -> {
            val newValue = value.rem(other.value)
            BigNumericValue(newValue)
        }

        is IntNumericValue -> {
            val newValue = value.rem(other.value.toBigInteger())
            BigNumericValue(newValue)
        }

        is FloatNumericValue -> {
            val newValue = value.toDouble().rem(other.value)
            FloatNumericValue(newValue)
        }

        else -> error(
            "binary operator rem is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    private fun pow(exp: Int): NumericValue {
        // handle small numbers
        val lower = BigInteger.valueOf(-2)
        val upper = BigInteger.valueOf(2)
        if (exp in -1..1) {
            return BigNumericValue(value.pow(exp))
        }
        if (value > lower && value < upper) {
            return BigNumericValue(value.pow(exp))
        }
        // BitLength(result) ~ BitLength(base) * exp
        // BitLength(result) < maxBitLength
        // => BitLength(base) * exp < maxBitLength
        // => BitLength(base) < maxBitLength / exp
        // => maxBase = 2 ^ (maxBitLength / exp)
        val exp2 = maxBitLength / exp
        val maxBase = upper.pow(exp2)
        if (value > maxBase) {
            throw ArithmeticException("base or exponent too high")
        }
        return BigNumericValue(value.pow(exp))
    }

    override fun pow(other: NumericValue): NumericValue {
        return when (other) {
            is BigNumericValue -> {
                pow(other.value.intValueExact())
            }

            is IntNumericValue -> {
                pow(other.value)
            }

            is FloatNumericValue -> {
                val newValue = value.toDouble().pow(other.value)
                FloatNumericValue(newValue)
            }

            else -> error(
                "binary operator pow is undefined for operands of type " +
                        "'${typeName(this)}' and '${typeName(other)}'"
            )
        }
    }

    override fun negate(): NumericValue {
        val newValue = value.times(BigInteger.valueOf(-1L))
        return BigNumericValue(newValue)
    }

    override fun toComparableValue(originalValue: Any?): ComparableValue {
        return BigComparableValue(originalValue, value)
    }

    override fun toIntValue(): NumericValue = this

    override fun toFloatValue(): NumericValue {
        return FloatNumericValue(value.toDouble())
    }

    override fun toStringValue(): CharSequence {
        return value.toString(10)
    }

    override fun intOrNull(): Int? = try {
        value.intValueExact()
    } catch (_: ArithmeticException) {
        null
    }

    override fun longOrNull(): Long? = try {
        value.longValueExact()
    } catch (_: ArithmeticException) {
        null
    }

    override fun doubleOrNull(): Double? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BigNumericValue

        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return "BigNumericValue($value)"
    }
}
