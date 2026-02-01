package org.cikit.forte.lib.js

import org.cikit.forte.core.ComparableValue
import org.cikit.forte.core.NumericValue
import org.cikit.forte.core.typeName
import kotlin.math.pow

class BigNumericValue(
    val value: dynamic
) : Number(), NumericValue {

    override val result: BigNumericValue
        get() = this

    override val isInt: Boolean
        get() = true

    override val isFloat: Boolean
        get() = false

    override val hasDecimalPart: Boolean
        get() = false

    override fun plus(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            val newValue = dynamicPlus(value, toBigInt(other.value))
            BigNumericValue(newValue)
        }

        is BigNumericValue -> {
            val newValue = dynamicPlus(value, other.value)
            BigNumericValue(newValue)
        }

        is FloatNumericValue -> {
            val newValue = dynamicToNumber(value) + other.value
            FloatNumericValue(newValue)
        }

        else -> error(
            "binary operator plus is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun minus(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            val newValue = dynamicMinus(value, toBigInt(other.value))
            BigNumericValue(newValue)
        }

        is BigNumericValue -> {
            val newValue = dynamicMinus(value, other.value)
            BigNumericValue(newValue)
        }

        is FloatNumericValue -> {
            val newValue = dynamicToNumber(value) - other.value
            FloatNumericValue(newValue)
        }

        else -> error(
            "binary operator minus is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun mul(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            val newValue = dynamicMultiply(value, toBigInt(other.value))
            BigNumericValue(newValue)
        }

        is BigNumericValue -> {
            val newValue = dynamicMultiply(value, other.value)
            BigNumericValue(newValue)
        }

        is FloatNumericValue -> {
            val newValue = dynamicToNumber(value) * other.value
            FloatNumericValue(newValue)
        }

        else -> error(
            "binary operator mul is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun div(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            val otherBig = toBigInt(other.value)
            val newValue = dynamicDivide(value, otherBig)
            val chk = dynamicCompareTo(
                value,
                dynamicMultiply(newValue, otherBig)
            )
            if (chk == 0) {
                BigNumericValue(newValue)
            } else {
                val newValue = dynamicToNumber(value) /
                        other.value.toDouble()
                FloatNumericValue(newValue)
            }
        }

        is BigNumericValue -> {
            val newValue = dynamicDivide(value, other.value)
            val chk = dynamicCompareTo(
                value,
                dynamicMultiply(newValue, other.value)
            )
            if (chk == 0) {
                BigNumericValue(newValue)
            } else {
                val newValue = dynamicToNumber(value) /
                        dynamicToNumber(other.value)
                FloatNumericValue(newValue)
            }
        }

        is FloatNumericValue -> {
            val newValue = dynamicToNumber(value) / other.value
            FloatNumericValue(newValue)
        }

        else -> error(
            "binary operator div is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun tdiv(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            val newValue = dynamicDivide(value, toBigInt(other.value))
            return BigNumericValue(newValue)
        }

        is BigNumericValue -> {
            val newValue = dynamicDivide(value, other.value)
            return BigNumericValue(newValue)
        }

        else -> error(
            "binary operator tdiv is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun rem(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            val newValue = dynamicReminder(value, toBigInt(other.value))
            return BigNumericValue(newValue)
        }

        is BigNumericValue -> {
            val newValue = dynamicReminder(value, other.value)
            return BigNumericValue(newValue)
        }

        is FloatNumericValue -> {
            val newValue = dynamicToNumber(value).rem(other.value)
            FloatNumericValue(newValue)
        }

        else -> error(
            "binary operator rem is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun pow(other: NumericValue): NumericValue {
        val maxValueAsDouble = 2.0.pow(maxBitLength.toDouble() + 1.0)
        if (dynamicCompareTo(value, maxValueAsDouble) > 0) {
            throw ArithmeticException("base too high")
        }
        val bitLength = value.toString(2).length
        return when (other) {
            is IntNumericValue -> {
                val bitLength = bitLength.timesExact(other.value)
                if (bitLength > maxBitLength) {
                    throw ArithmeticException("exponent too high")
                }
                val newValue = dynamicPow(value, toBigInt(other.value))
                BigNumericValue(newValue)
            }

            is BigNumericValue -> {
                val bitLength = dynamicMultiply(
                    toBigInt(bitLength),
                    other.value
                )
                val chk = dynamicCompareTo(bitLength, toBigInt(maxBitLength))
                if (chk > 0) {
                    throw ArithmeticException("exponent too high")
                }
                val newValue = dynamicPow(value, other.value)
                BigNumericValue(newValue)
            }

            is FloatNumericValue -> {
                val newValue = dynamicToNumber(value).pow(other.value)
                FloatNumericValue(newValue)
            }

            else -> error(
                "binary operator pow is undefined for operands of type " +
                        "'${typeName(this)}' and '${typeName(other)}'"
            )
        }
    }

    override fun toComparableValue(originalValue: Any?): ComparableValue {
        return BigComparableValue(originalValue, value)
    }

    override fun toIntValue(): NumericValue = this

    override fun toFloatValue(): NumericValue {
        return FloatNumericValue(dynamicToNumber(value))
    }

    override fun toStringValue(): CharSequence {
        return value.toString()
    }

    override fun toDouble(): Double = dynamicToNumber(value)

    override fun toFloat(): Float = toDouble().toFloat()

    override fun toLong(): Long = toDouble().toLong()

    override fun toInt(): Int = toDouble().toInt()

    override fun toShort(): Short = toInt().toShort()

    override fun toByte(): Byte = toInt().toByte()

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
