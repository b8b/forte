package org.cikit.forte.lib.wasmjs

import com.ionspin.kotlin.bignum.integer.BigInteger
import dev.erikchristensen.javamath2kmp.timesExact
import org.cikit.forte.core.ComparableValue
import org.cikit.forte.core.NumericValue
import org.cikit.forte.core.typeName
import kotlin.math.pow

class BigNumericValue(
    val value: BigInteger,
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
            val newValue = value.plus(other.value)
            BigNumericValue(newValue)
        }

        is BigNumericValue -> {
            val newValue = value.plus(other.value)
            BigNumericValue(newValue)
        }

        is FloatNumericValue -> {
            val newValue = value.doubleValue().plus(other.value)
            FloatNumericValue(newValue)
        }

        else -> error(
            "binary operator plus is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun minus(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            val newValue = value.minus(other.value)
            BigNumericValue(newValue)
        }

        is BigNumericValue -> {
            val newValue = value.minus(other.value)
            BigNumericValue(newValue)
        }

        is FloatNumericValue -> {
            val newValue = value.doubleValue() - other.value
            FloatNumericValue(newValue)
        }

        else -> error(
            "binary operator minus is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun mul(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            val otherBig = BigInteger.fromInt(other.value)
            val newValue = value.multiply(otherBig)
            BigNumericValue(newValue)
        }

        is BigNumericValue -> {
            val newValue = value.multiply(other.value)
            BigNumericValue(newValue)
        }

        is FloatNumericValue -> {
            val newValue = value.doubleValue() * other.value
            FloatNumericValue(newValue)
        }

        else -> error(
            "binary operator mul is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun div(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            val otherBig = BigInteger.fromInt(other.value)
            val newValue = value.div(otherBig)
            if (newValue.multiply(otherBig) == value) {
                BigNumericValue(newValue)
            } else {
                val newValue = value.doubleValue() / other.value
                FloatNumericValue(newValue)
            }
        }

        is BigNumericValue -> {
            val newValue = value.div(other.value)
            if (newValue.multiply(other.value) == value) {
                BigNumericValue(newValue)
            } else {
                val newValue = value.doubleValue() /
                        other.value.doubleValue()
                FloatNumericValue(newValue)
            }
        }

        is FloatNumericValue -> {
            val newValue = value.doubleValue() / other.value
            FloatNumericValue(newValue)
        }

        else -> error(
            "binary operator div is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun tdiv(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            val newValue = value.div(other.value)
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
        is IntNumericValue -> {
            val newValue = value.rem(other.value)
            return BigNumericValue(newValue)
        }

        is BigNumericValue -> {
            val newValue = value.rem(other.value)
            return BigNumericValue(newValue)
        }

        is FloatNumericValue -> {
            val newValue = value.doubleValue().rem(other.value)
            FloatNumericValue(newValue)
        }

        else -> error(
            "binary operator rem is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun pow(other: NumericValue): NumericValue {
        val bitLength = value.bitLength()
        return when (other) {
            is IntNumericValue -> {
                val bitLength = bitLength.timesExact(other.value)
                if (bitLength > maxBitLength) {
                    throw ArithmeticException("exponent too high")
                }
                val newValue = value.pow(other.value)
                BigNumericValue(newValue)
            }

            is BigNumericValue -> {
                val exp = other.value.intValue(true)
                val bitLength = bitLength.timesExact(exp)
                if (bitLength > maxBitLength) {
                    throw ArithmeticException("exponent too high")
                }
                val newValue = value.pow(exp)
                BigNumericValue(newValue)
            }

            is FloatNumericValue -> {
                val newValue = value.doubleValue().pow(other.value)
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
        return FloatNumericValue(value.doubleValue())
    }

    override fun toStringValue(): CharSequence {
        return value.toString(10)
    }

    override fun toDouble(): Double = value.doubleValue()

    override fun toFloat(): Float = value.floatValue()

    override fun toLong(): Long = value.longValue()

    override fun toInt(): Int = value.intValue()

    override fun toShort(): Short = value.shortValue()

    override fun toByte(): Byte = value.byteValue()

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
