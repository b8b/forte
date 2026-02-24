package org.cikit.forte.lib.wasmjs

import com.ionspin.kotlin.bignum.integer.BigInteger
import org.cikit.forte.core.ComparableValue
import org.cikit.forte.core.NumericValue
import org.cikit.forte.core.typeName
import kotlin.math.pow
import kotlin.math.sign

class FloatNumericValue(
    val value: Double,
): NumericValue {

    override val result: Double
        get() = value

    override val isInt: Boolean
        get() = false

    override val isFloat: Boolean
        get() = true

    override val hasDecimalPart: Boolean
        get() = value % 1 != 0.0

    override fun plus(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            val newValue = value + other.value
            FloatNumericValue(newValue)
        }

        is BigNumericValue -> {
            val newValue = value + other.value.doubleValue()
            FloatNumericValue(newValue)
        }

        is FloatNumericValue -> {
            val newValue = value + other.value
            FloatNumericValue(newValue)
        }

        else -> error(
            "binary operator plus is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun minus(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            val newValue = value - other.value
            FloatNumericValue(newValue)
        }

        is BigNumericValue -> {
            val newValue = value - other.value.doubleValue()
            FloatNumericValue(newValue)
        }

        is FloatNumericValue -> {
            val newValue = value - other.value
            FloatNumericValue(newValue)
        }

        else -> error(
            "binary operator minus is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun mul(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            val newValue = value * other.value
            FloatNumericValue(newValue)
        }

        is BigNumericValue -> {
            val newValue = value * other.value.doubleValue()
            FloatNumericValue(newValue)
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
            FloatNumericValue(newValue)
        }

        is BigNumericValue -> {
            val newValue = value / other.value.doubleValue()
            FloatNumericValue(newValue)
        }

        is FloatNumericValue -> {
            val newValue = value / other.value
            FloatNumericValue(newValue)
        }

        else -> error(
            "binary operator div is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun tdiv(other: NumericValue): NumericValue {
        error(
            "binary operator tdiv is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun rem(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            val newValue = value % other.value.toDouble()
            FloatNumericValue(newValue)
        }

        is BigNumericValue -> {
            val newValue = value % other.value.doubleValue()
            FloatNumericValue(newValue)
        }

        is FloatNumericValue -> {
            val newValue = value % other.value
            FloatNumericValue(newValue)
        }

        else -> error(
            "binary operator rem is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun pow(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            val newValue = value.pow(other.value)
            FloatNumericValue(newValue)
        }

        is BigNumericValue -> {
            val newValue = value.pow(other.value.doubleValue())
            FloatNumericValue(newValue)
        }

        is FloatNumericValue -> {
            val newValue = value.pow(other.value)
            FloatNumericValue(newValue)
        }

        else -> error(
            "binary operator pow is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun negate(): NumericValue = FloatNumericValue(value * -1.0)

    override fun toComparableValue(originalValue: Any?): ComparableValue {
        if (value.isNaN()) {
            error("NaN is not comparable")
        }
        if (!hasDecimalPart && value.isFinite()) {
            return FloatComparableValue.DirectComparableValue(
                originalValue,
                value
            )
        }
        if (value.sign == -1.0) {
            return FloatComparableValue.NegativeComparableValue(
                originalValue,
                value
            )
        }
        return FloatComparableValue.PositiveComparableValue(
            originalValue,
            value
        )
    }

    override fun toIntValue(): NumericValue {
        val newValue = BigInteger.tryFromDouble(value)
        return BigNumericValue(newValue)
    }

    override fun toFloatValue(): NumericValue = this

    override fun toStringValue(): CharSequence {
        return value.toString()
    }

    override fun intOrNull(): Int? = null

    override fun longOrNull(): Long? = null

    override fun doubleOrNull(): Double = value

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as FloatNumericValue

        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return "FloatNumericValue($value)"
    }
}
