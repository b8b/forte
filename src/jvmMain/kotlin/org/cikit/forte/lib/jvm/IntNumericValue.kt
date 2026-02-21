package org.cikit.forte.lib.jvm

import org.cikit.forte.core.ComparableValue
import org.cikit.forte.core.NumericValue
import org.cikit.forte.core.typeName
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
            val newValue = value / other.value
            if (newValue * other.value == value) {
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

    override fun pow(other: NumericValue): NumericValue {
        val bitLength = when (value) {
            0, 1 -> return this
            in 2 .. Int.MAX_VALUE -> {
                Int.SIZE_BITS - value.countLeadingZeroBits()
            }

            else -> {
                Int.SIZE_BITS - (0 - value).countLeadingZeroBits()
            }
        }
        return when (other) {
            is IntNumericValue -> {
                val bitLength = bitLength * other.value
                if (bitLength <= 30) {
                    val newValue = value.toDouble()
                        .pow(other.value)
                        .toInt()
                    IntNumericValue(newValue)
                } else if (bitLength > maxBitLength) {
                    throw ArithmeticException("exponent too high")
                } else {
                    val newValue = value.toBigInteger()
                        .pow(other.value)
                    BigNumericValue(newValue)
                }
            }

            is FloatNumericValue -> {
                val newValue = value.toDouble().pow(other.value)
                FloatNumericValue(newValue)
            }

            is BigNumericValue -> {
                val exp = other.value.intValueExact()
                val bitLength = bitLength * exp
                if (bitLength > maxBitLength) {
                    throw ArithmeticException("exponent too high")
                }
                val newValue = value.toBigInteger().pow(exp)
                BigNumericValue(newValue)
            }

            else -> error(
                "binary operator pow is undefined for operands of type " +
                        "'${typeName(this)}' and '${typeName(other)}'"
            )
        }
    }

    override fun negate(): NumericValue = IntNumericValue(value * -1)

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

    override fun toIntOrNull(): Int = value

    override fun toDoubleOrNull(): Double? = null

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
