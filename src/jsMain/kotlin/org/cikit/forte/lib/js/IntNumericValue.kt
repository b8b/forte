package org.cikit.forte.lib.js

import dev.erikchristensen.javamath2kmp.minusExact
import dev.erikchristensen.javamath2kmp.plusExact
import dev.erikchristensen.javamath2kmp.timesExact
import org.cikit.forte.core.ComparableValue
import org.cikit.forte.core.NumericValue
import org.cikit.forte.core.typeName
import kotlin.math.pow

class IntNumericValue(
    val value: Int
) : Number(), NumericValue {

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
                val newValue = value.plusExact(other.value)
                IntNumericValue(newValue)
            } catch (_: ArithmeticException) {
                val newValue = dynamicPlus(
                    toBigInt(value),
                    toBigInt(other.value)
                )
                BigNumericValue(newValue)
            }
        }

        is BigNumericValue -> {
            val newValue = dynamicPlus(toBigInt(value), other.value)
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
                val newValue = value.minusExact(other.value)
                IntNumericValue(newValue)
            } catch (_: ArithmeticException) {
                val newValue = dynamicMinus(
                    toBigInt(value),
                    toBigInt(other.value)
                )
                BigNumericValue(newValue)
            }
        }

        is BigNumericValue -> {
            val newValue = dynamicMinus(toBigInt(value), other.value)
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
                val newValue = value.timesExact(other.value)
                IntNumericValue(newValue)
            } catch (_: ArithmeticException) {
                val newValue = dynamicMultiply(
                    toBigInt(value),
                    toBigInt(other.value)
                )
                BigNumericValue(newValue)
            }
        }

        is BigNumericValue -> {
            val newValue = dynamicMultiply(toBigInt(value), other.value)
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
            val newValue = value / other.value
            if (newValue * other.value == value) {
                IntNumericValue(newValue)
            } else {
                val newValue = value.toDouble() / other.value
                FloatNumericValue(newValue)
            }
        }

        is BigNumericValue -> {
            val big = toBigInt(value)
            val newValue = dynamicDivide(big, other.value)
            val chk = dynamicMultiply(newValue, other.value)
            if (dynamicCompareTo(chk, big) == 0) {
                BigNumericValue(newValue)
            } else {
                val newValue = value.toDouble() /
                        dynamicToNumber(other.value)
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
            val newValue = dynamicDivide(toBigInt(value), other.value)
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
            return IntNumericValue(newValue)
        }

        is BigNumericValue -> {
            val newValue = dynamicReminder(toBigInt(value), other.value)
            return BigNumericValue(newValue)
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
                val bitLength = bitLength.timesExact(other.value)
                if (bitLength <= 30) {
                    val newValue = value.toDouble()
                        .pow(other.value)
                        .toInt()
                    IntNumericValue(newValue)
                } else if (bitLength > maxBitLength) {
                    throw ArithmeticException("exponent too high")
                } else {
                    val newValue = dynamicPow(
                        toBigInt(value),
                        toBigInt(other.value)
                    )
                    BigNumericValue(newValue)
                }
            }

            is BigNumericValue -> {
                val bitLength = dynamicMultiply(
                    other.value,
                    toBigInt(bitLength)
                )
                val cmp = dynamicCompareTo(bitLength, toBigInt(maxBitLength))
                if (cmp > 0) {
                    throw ArithmeticException("exponent too high")
                }
                val newValue = dynamicPow(toBigInt(value), other.value)
                BigNumericValue(newValue)
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

    override fun toDouble(): Double = value.toDouble()

    override fun toFloat(): Float = value.toFloat()

    override fun toLong(): Long = value.toLong()

    override fun toInt(): Int = value

    override fun toShort(): Short = value.toShort()

    override fun toByte(): Byte = value.toByte()

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
