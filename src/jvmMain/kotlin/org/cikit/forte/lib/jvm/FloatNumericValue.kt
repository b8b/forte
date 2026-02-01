package org.cikit.forte.lib.jvm

import org.cikit.forte.core.ComparableValue
import org.cikit.forte.core.NumericValue
import org.cikit.forte.core.typeName
import kotlin.math.pow

class FloatNumericValue(
    val value: Double
): Number(), NumericValue {

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
            val newValue = value + other.value.toDouble()
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
            val newValue = value - other.value.toDouble()
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
            val newValue = value * other.value.toDouble()
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
            val newValue = value / other.value.toDouble()
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
        is FloatNumericValue -> {
            val newValue = value % other.value
            FloatNumericValue(newValue)
        }

        is IntNumericValue -> {
            val newValue = value % other.value
            FloatNumericValue(newValue)
        }

        is BigNumericValue -> {
            val newValue = value % other.value.toDouble()
            FloatNumericValue(newValue)
        }

        else -> error(
            "binary operator rem is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun pow(other: NumericValue): NumericValue = when (other) {
        is FloatNumericValue -> {
            val newValue = value.pow(other.value)
            FloatNumericValue(newValue)
        }

        is IntNumericValue -> {
            val newValue = value.pow(other.value)
            FloatNumericValue(newValue)
        }

        is BigNumericValue -> {
            val newValue = value.pow(other.value.toDouble())
            FloatNumericValue(newValue)
        }

        else -> error(
            "binary operator rem is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun toComparableValue(originalValue: Any?): ComparableValue {
        return FloatComparableValue(originalValue, value)
    }

    override fun toIntValue(): NumericValue {
        val value = value.toBigDecimal().toBigInteger()
        return BigNumericValue(value)
    }

    override fun toFloatValue(): NumericValue = this

    override fun toStringValue(): CharSequence {
        return value.toString()
    }

    override fun toDouble(): Double = value

    override fun toFloat(): Float = value.toFloat()

    override fun toLong(): Long = value.toLong()

    override fun toInt(): Int = value.toInt()

    override fun toShort(): Short = value.toInt().toShort()

    override fun toByte(): Byte = value.toInt().toByte()

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
