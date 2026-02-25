package org.cikit.forte.lib.jvm

import org.cikit.forte.core.ComparableValue
import org.cikit.forte.core.NumericValue
import org.cikit.forte.core.typeName
import java.math.BigInteger
import kotlin.math.pow

class IntNumericValue(
    val value: Int
) : NumericValue {

    override val result: Int
        get() = value

    override val isInt: Boolean
        get() = true

    override val isFloat: Boolean
        get() = false

    override val hasDecimalPart: Boolean
        get() = false

    override fun plus(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            try {
                val newValue = Math.addExact(value, other.value)
                IntNumericValue(newValue)
            } catch (_: ArithmeticException) {
                val newValue = value.toBigInteger()
                    .plus(other.value.toBigInteger())
                BigNumericValue(newValue)
            }
        }

        is BigNumericValue -> {
            val newValue = value.toBigInteger().plus(other.value)
            BigNumericValue(newValue)
        }

        is FloatNumericValue -> {
            val newValue = value.toDouble() + other.value
            FloatNumericValue(newValue)
        }

        else -> error(
            "binary operator plus is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun minus(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            try {
                val newValue = Math.subtractExact(value, other.value)
                IntNumericValue(newValue)
            } catch (_: ArithmeticException) {
                val newValue = value.toBigInteger()
                    .minus(other.value.toBigInteger())
                BigNumericValue(newValue)
            }
        }

        is BigNumericValue -> {
            val newValue = value.toBigInteger().minus(other.value)
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
            try {
                val newValue = Math.multiplyExact(value, other.value)
                IntNumericValue(newValue)
            } catch (_: ArithmeticException) {
                val newValue = value.toBigInteger()
                    .multiply(other.value.toBigInteger())
                BigNumericValue(newValue)
            }
        }

        is BigNumericValue -> {
            val newValue = value.toBigInteger().multiply(other.value)
            BigNumericValue(newValue)
        }

        is FloatNumericValue -> {
            val newValue = other.value * value
            FloatNumericValue(newValue)
        }

        else -> error(
            "binary operator mul is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun div(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            val remainder = value % other.value
            if (remainder == 0) {
                val newValue = value / other.value
                IntNumericValue(newValue)
            } else {
                val newValue = value.toDouble() / other.value
                FloatNumericValue(newValue)
            }
        }

        is BigNumericValue -> {
            val big = value.toBigInteger()
            val newValue = big.div(other.value)
            if (newValue.multiply(other.value) == big) {
                BigNumericValue(newValue)
            } else {
                val newValue = value.toDouble() /
                        other.value.toDouble()
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
            val newValue = value / other.value
            return IntNumericValue(newValue)
        }

        is BigNumericValue -> {
            val newValue = value.toBigInteger().div(other.value)
            return BigNumericValue(newValue)
        }

        else -> error(
            "binary operator tdiv is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun rem(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            val newValue = value.rem(other.value)
            IntNumericValue(newValue)
        }

        is FloatNumericValue -> {
            val newValue = value.toDouble().rem(other.value)
            FloatNumericValue(newValue)
        }

        is BigNumericValue -> {
            val newValue = value.toBigInteger().rem(other.value)
            BigNumericValue(newValue)
        }

        else -> error(
            "binary operator rem is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    private fun pow(exp: Int): BigInteger {
        val base = BigInteger.valueOf(value.toLong())
        // handle small numbers
        if (exp in -1..1) {
            return base.pow(exp)
        }
        if (value in -1..1) {
            return base.pow(exp)
        }
        // BitLength(result) ~ BitLength(base) * exp
        // BitLength(result) < maxBitLength
        // => BitLength(base) * exp < maxBitLength
        // => BitLength(base) < maxBitLength / exp
        // => maxBase = 2 ^ (maxBitLength / exp)
        val exp2 = maxBitLength / exp
        val maxBase = BigInteger.valueOf(2L).pow(exp2)
        if (base > maxBase) {
            throw ArithmeticException("base or exponent too high")
        }
        return base.pow(exp)
    }

    override fun pow(other: NumericValue): NumericValue {
        return when (other) {
            is IntNumericValue -> {
                val result = pow(other.value)
                try {
                    IntNumericValue(result.intValueExact())
                } catch (_: ArithmeticException) {
                    BigNumericValue(result)
                }
            }

            is FloatNumericValue -> {
                val newValue = value.toDouble().pow(other.value)
                FloatNumericValue(newValue)
            }

            is BigNumericValue -> {
                val result = pow(other.value.intValueExact())
                try {
                    IntNumericValue(result.intValueExact())
                } catch (_: ArithmeticException) {
                    BigNumericValue(result)
                }
            }

            else -> error(
                "binary operator pow is undefined for operands of type " +
                        "'${typeName(this)}' and '${typeName(other)}'"
            )
        }
    }

    override fun negate(): NumericValue {
        return IntNumericValue(Math.negateExact(value))
    }

    override fun toComparableValue(originalValue: Any?): ComparableValue {
        return FloatComparableValue(originalValue, value.toDouble())
    }

    override fun toIntValue(): NumericValue = this

    override fun toFloatValue(): NumericValue {
        return FloatNumericValue(value.toDouble())
    }

    override fun toStringValue(): CharSequence {
        return value.toString()
    }

    override fun intOrNull(): Int = value

    override fun longOrNull(): Long = value.toLong()

    override fun doubleOrNull(): Double? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as IntNumericValue

        return value == other.value
    }

    override fun hashCode(): Int {
        return value
    }

    override fun toString(): String {
        return "IntNumericValue($value)"
    }
}
