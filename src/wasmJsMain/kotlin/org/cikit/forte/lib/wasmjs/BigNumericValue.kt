package org.cikit.forte.lib.wasmjs

import com.ionspin.kotlin.bignum.integer.BigInteger
import org.cikit.forte.core.NumericValue
import org.cikit.forte.core.typeName
import kotlin.math.pow

class BigNumericValue(
    val converted: BigInteger
) : Number(), NumericValue {

    override val value: Any
        get() = this

    override val isInt: Boolean
        get() = true

    override val isFloat: Boolean
        get() = false

    override val hasDecimalPart: Boolean
        get() = false

    override fun plus(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            val newValue = converted.plus(other.converted)
            BigNumericValue(newValue)
        }

        is BigNumericValue -> {
            val newValue = converted.plus(other.converted)
            BigNumericValue(newValue)
        }

        is FloatNumericValue -> {
            val newValue = converted.doubleValue().plus(other.converted)
            FloatNumericValue(newValue, newValue)
        }

        else -> error(
            "binary operator plus is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun minus(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            val newValue = converted.minus(other.converted)
            BigNumericValue(newValue)
        }

        is BigNumericValue -> {
            val newValue = converted.minus(other.converted)
            BigNumericValue(newValue)
        }

        is FloatNumericValue -> {
            val newValue = converted.doubleValue() - other.converted
            FloatNumericValue(newValue, newValue)
        }

        else -> error(
            "binary operator minus is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun mul(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            val otherBig = BigInteger.fromInt(other.converted)
            val newValue = converted.multiply(otherBig)
            BigNumericValue(newValue)
        }

        is BigNumericValue -> {
            val newValue = converted.multiply(other.converted)
            BigNumericValue(newValue)
        }

        is FloatNumericValue -> {
            val newValue = converted.doubleValue() * other.converted
            FloatNumericValue(newValue, newValue)
        }

        else -> error(
            "binary operator mul is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun div(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            val otherBig = BigInteger.fromInt(other.converted)
            val newValue = converted.div(otherBig)
            if (newValue.multiply(otherBig) == converted) {
                BigNumericValue(newValue)
            } else {
                val newFloat = converted.doubleValue() / other.converted
                FloatNumericValue(newFloat, newFloat)
            }
        }

        is BigNumericValue -> {
            val newValue = converted.div(other.converted)
            if (newValue.multiply(other.converted) == converted) {
                BigNumericValue(newValue)
            } else {
                val newFloat = converted.doubleValue() /
                        other.converted.doubleValue()
                FloatNumericValue(newFloat, newFloat)
            }
        }

        is FloatNumericValue -> {
            val newValue = converted.doubleValue() / other.converted
            FloatNumericValue(newValue, newValue)
        }

        else -> error(
            "binary operator div is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun tdiv(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            val newValue = converted.div(other.converted)
            return BigNumericValue(newValue)
        }

        is BigNumericValue -> {
            val newValue = converted.div(other.converted)
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
            return BigNumericValue(newValue)
        }

        is BigNumericValue -> {
            val newValue = converted.rem(other.converted)
            return BigNumericValue(newValue)
        }

        is FloatNumericValue -> {
            val newValue = converted.doubleValue().rem(other.converted)
            FloatNumericValue(newValue, newValue)
        }

        else -> error(
            "binary operator rem is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun pow(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            val newValue = converted.pow(other.converted)
            BigNumericValue(newValue)
        }

        is BigNumericValue -> {
            val newValue = converted.pow(other.converted)
            BigNumericValue(newValue)
        }

        is FloatNumericValue -> {
            val newValue = converted.doubleValue().pow(other.converted)
            FloatNumericValue(newValue, newValue)
        }

        else -> error(
            "binary operator pow is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun toIntValue(): NumericValue = this

    override fun toFloatValue(): NumericValue {
        return FloatNumericValue(value, converted.doubleValue())
    }

    override fun toStringValue(): CharSequence {
        return converted.toString(10)
    }

    override fun toDouble(): Double = converted.doubleValue()

    override fun toFloat(): Float = converted.floatValue()

    override fun toLong(): Long = converted.longValue()

    override fun toInt(): Int = converted.intValue()

    override fun toShort(): Short = converted.shortValue()

    override fun toByte(): Byte = converted.byteValue()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BigNumericValue

        return converted == other.converted
    }

    override fun hashCode(): Int {
        return converted.hashCode()
    }

    override fun toString(): String {
        return "BigNumericValue($converted)"
    }
}
