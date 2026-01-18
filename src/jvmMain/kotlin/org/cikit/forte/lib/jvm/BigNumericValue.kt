package org.cikit.forte.lib.jvm

import org.cikit.forte.core.ComparableValue
import org.cikit.forte.core.NumericValue
import org.cikit.forte.core.typeName
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.pow

class BigNumericValue(
    override val value: Any?,
    val converted: BigInteger
) : Number(), NumericValue, ComparableValue {

    override val isInt: Boolean
        get() = true

    override val isFloat: Boolean
        get() = false

    override val hasDecimalPart: Boolean
        get() = false

    override fun plus(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            val newValue = converted.plus(other.converted.toBigInteger())
            BigNumericValue(newValue, newValue)
        }

        is BigNumericValue -> {
            val newValue = converted.plus(other.converted)
            BigNumericValue(newValue, newValue)
        }

        is FloatNumericValue -> {
            val newValue = converted.toDouble().plus(other.converted)
            FloatNumericValue(newValue, newValue)
        }

        else -> error(
            "binary operator plus is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun minus(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            val newValue = converted.minus(other.converted.toBigInteger())
            BigNumericValue(newValue, newValue)
        }

        is BigNumericValue -> {
            val newValue = converted.minus(other.converted)
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
            val otherBig = other.converted.toBigInteger()
            val newValue = converted.multiply(otherBig)
            BigNumericValue(newValue, newValue)
        }

        is BigNumericValue -> {
            val newValue = converted.multiply(other.converted)
            BigNumericValue(newValue, newValue)
        }

        is FloatNumericValue -> {
            val newValue = converted.toDouble() * other.converted
            FloatNumericValue(newValue, newValue)
        }

        else -> error(
            "binary operator mul is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun div(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            val otherBig = other.converted.toBigInteger()
            val newValue = converted.div(otherBig)
            if (newValue.multiply(otherBig) == converted) {
                BigNumericValue(newValue, newValue)
            } else {
                val newFloat = converted.toDouble() / other.converted
                FloatNumericValue(newFloat, newFloat)
            }
        }

        is BigNumericValue -> {
            val newValue = converted.div(other.converted)
            if (newValue.multiply(other.converted) == converted) {
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
            val newValue = converted.div(other.converted.toBigInteger())
            return BigNumericValue(newValue, newValue)
        }

        is BigNumericValue -> {
            val newValue = converted.div(other.converted)
            return BigNumericValue(newValue, newValue)
        }

        else -> error(
            "binary operator tdiv is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun rem(other: NumericValue): NumericValue = when (other) {
        is BigNumericValue -> {
            val newValue = converted.rem(other.converted)
            BigNumericValue(newValue, newValue)
        }

        is IntNumericValue -> {
            val newValue = converted
                .rem(BigInteger.valueOf(other.converted.toLong()))
            BigNumericValue(newValue, newValue)
        }

        is FloatNumericValue -> {
            val newValue = converted.toDouble().rem(other.converted)
            FloatNumericValue(newValue, newValue)
        }

        else -> error(
            "binary operator rem is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun pow(other: NumericValue): NumericValue = when (other) {
        is BigNumericValue -> {
            if (converted == BigInteger.ZERO ||
                converted == BigInteger.ONE)
            {
                this
            } else {
                val newValue = converted.pow(other.converted.intValueExact())
                BigNumericValue(newValue, newValue)
            }
        }

        is IntNumericValue -> {
            val newValue = converted.pow(other.converted)
            BigNumericValue(newValue, newValue)
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

    override fun compareTo(other: ComparableValue): Int {
        return when (other) {
            is BigNumericValue -> converted.compareTo(other.converted)
            is FloatNumericValue -> BigDecimal(converted)
                .compareTo(BigDecimal.valueOf(other.converted))
            else -> error(
                "compareTo undefined for operands of type " +
                        "'${typeName(value)}' and '${typeName(other)}'"
            )
        }
    }

    override fun toIntValue(): NumericValue = this

    override fun toFloatValue(): NumericValue {
        return FloatNumericValue(value, converted.toDouble())
    }

    override fun toStringValue(): CharSequence {
        return converted.toString(10)
    }

    override fun toDouble(): Double = converted.toDouble()

    override fun toFloat(): Float = converted.toFloat()

    override fun toLong(): Long = converted.toLong()

    override fun toInt(): Int = converted.toInt()

    override fun toShort(): Short = converted.toShort()

    override fun toByte(): Byte = converted.toByte()

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
