package org.cikit.forte.lib.jvm

import org.cikit.forte.core.ComparableValue
import org.cikit.forte.core.NumericValue
import org.cikit.forte.core.typeName
import java.math.BigDecimal
import kotlin.math.pow

class FloatNumericValue(
    override val value: Any?,
    val converted: Double
): Number(), NumericValue, ComparableValue {

    override val isInt: Boolean
        get() = true

    override val isFloat: Boolean
        get() = false

    override val hasDecimalPart: Boolean
        get() = converted % 1 != 0.0

    override fun plus(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            val newValue = converted + other.converted
            FloatNumericValue(newValue, newValue)
        }

        is BigNumericValue -> {
            val newValue = converted + other.converted.toDouble()
            FloatNumericValue(newValue, newValue)
        }

        is FloatNumericValue -> {
            val newValue = converted + other.converted
            FloatNumericValue(newValue, newValue)
        }

        else -> error(
            "binary operator plus is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun minus(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            val newValue = converted - other.converted
            FloatNumericValue(newValue, newValue)
        }

        is BigNumericValue -> {
            val newValue = converted - other.converted.toDouble()
            FloatNumericValue(newValue, newValue)
        }

        is FloatNumericValue -> {
            val newValue = converted - other.converted
            FloatNumericValue(newValue, newValue)
        }

        else -> error(
            "binary operator minus is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun mul(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            val newValue = converted * other.converted
            FloatNumericValue(newValue, newValue)
        }

        is BigNumericValue -> {
            val newValue = converted * other.converted.toDouble()
            FloatNumericValue(newValue, newValue)
        }

        is FloatNumericValue -> {
            val newValue = other.converted * converted
            FloatNumericValue(newValue, newValue)
        }

        else -> error(
            "binary operator mul is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun div(other: NumericValue): NumericValue = when (other) {
        is IntNumericValue -> {
            val newValue = converted / other.converted
            FloatNumericValue(newValue, newValue)
        }

        is BigNumericValue -> {
            val newFloat = converted / other.converted.toDouble()
            FloatNumericValue(newFloat, newFloat)
        }

        is FloatNumericValue -> {
            val newValue = converted / other.converted
            FloatNumericValue(newValue, newValue)
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
            val newValue = converted % other.converted
            FloatNumericValue(newValue, newValue)
        }

        is IntNumericValue -> {
            val newValue = converted % other.converted
            FloatNumericValue(newValue, newValue)
        }

        is BigNumericValue -> {
            val newValue = converted % other.converted.toDouble()
            FloatNumericValue(newValue, newValue)
        }

        else -> error(
            "binary operator rem is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun pow(other: NumericValue): NumericValue = when (other) {
        is FloatNumericValue -> {
            val newValue = converted.pow(other.converted)
            FloatNumericValue(newValue, newValue)
        }

        is IntNumericValue -> {
            val newValue = converted.pow(other.converted)
            FloatNumericValue(newValue, newValue)
        }

        is BigNumericValue -> {
            val newValue = converted.pow(other.converted.toDouble())
            FloatNumericValue(newValue, newValue)
        }

        else -> error(
            "binary operator rem is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun compareTo(other: ComparableValue): Int {
        return when (other) {
            is FloatNumericValue -> converted.compareTo(other.converted)
            is BigNumericValue -> BigDecimal.valueOf(converted)
                .compareTo(BigDecimal(other.converted))
            else -> error(
                "compareTo undefined for operands of type " +
                        "'${typeName(value)}' and '${typeName(other)}'"
            )
        }
    }

    override fun toIntValue(): NumericValue {
        val converted = converted.toBigDecimal().toBigInteger()
        return BigNumericValue(converted, converted)
    }

    override fun toFloatValue(): NumericValue = this

    override fun toStringValue(): CharSequence {
        return converted.toString()
    }

    override fun toDouble(): Double = converted

    override fun toFloat(): Float = converted.toFloat()

    override fun toLong(): Long = converted.toLong()

    override fun toInt(): Int = converted.toInt()

    override fun toShort(): Short = converted.toInt().toShort()

    override fun toByte(): Byte = converted.toInt().toByte()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as FloatNumericValue

        return converted == other.converted
    }

    override fun hashCode(): Int {
        return converted.hashCode()
    }

    override fun toString(): String {
        return "FloatNumericValue($converted)"
    }
}
