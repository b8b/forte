package org.cikit.forte.lib.js

import org.cikit.forte.core.ComparableValue
import org.cikit.forte.core.NumericValue
import org.cikit.forte.core.typeName
import kotlin.math.pow

class BigNumericValue(
    val value: BigInt
) : NumericValue {

    override val result: Number
        get() = value.asDynamic()

    override val isInt: Boolean
        get() = true

    override val isFloat: Boolean
        get() = false

    override val hasDecimalPart: Boolean
        get() = false

    override fun plus(other: NumericValue): NumericValue = when (other) {
        is BigNumericValue -> {
            val newValue = bigIntAdd(value, other.value)
            BigNumericValue(newValue)
        }

        is FloatNumericValue -> {
            val newValue = Number(value) + other.value
            FloatNumericValue(newValue)
        }

        else -> error(
            "binary operator plus is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun minus(other: NumericValue): NumericValue = when (other) {
        is BigNumericValue -> {
            val newValue = bigIntSubtract(value, other.value)
            BigNumericValue(newValue)
        }

        is FloatNumericValue -> {
            val newValue = Number(value) - other.value
            FloatNumericValue(newValue)
        }

        else -> error(
            "binary operator minus is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun mul(other: NumericValue): NumericValue = when (other) {
        is BigNumericValue -> {
            val newValue = bigIntMultiply(value, other.value)
            BigNumericValue(newValue)
        }

        is FloatNumericValue -> {
            val newValue = Number(value) * other.value
            FloatNumericValue(newValue)
        }

        else -> error(
            "binary operator mul is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun div(other: NumericValue): NumericValue = when (other) {
        is BigNumericValue -> {
            val remainder = bigIntRemainder(value, other.value)
            if (bigIntEq(remainder, BigInt(0.0))) {
                val newValue = bigIntDivide(value, other.value)
                BigNumericValue(newValue)
            } else {
                val newValue = Number(value) / Number(other.value)
                FloatNumericValue(newValue)
            }
        }

        is FloatNumericValue -> {
            val newValue = Number(value) / other.value
            FloatNumericValue(newValue)
        }

        else -> error(
            "binary operator div is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun tdiv(other: NumericValue): NumericValue = when (other) {
        is BigNumericValue -> {
            val newValue = bigIntDivide(value, other.value)
            return BigNumericValue(newValue)
        }

        else -> error(
            "binary operator tdiv is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun rem(other: NumericValue): NumericValue = when (other) {
        is BigNumericValue -> {
            val newValue = bigIntRemainder(value, other.value)
            return BigNumericValue(newValue)
        }

        is FloatNumericValue -> {
            val newValue = Number(value) % other.value
            FloatNumericValue(newValue)
        }

        else -> error(
            "binary operator rem is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun pow(other: NumericValue): NumericValue {
        return when (other) {
            is BigNumericValue -> {
                restrictedPow(value, other.value, maxBitLength)
            }

            is FloatNumericValue -> {
                val newValue = Number(value).pow(other.value)
                FloatNumericValue(newValue)
            }

            else -> error(
                "binary operator pow is undefined for operands of type " +
                        "'${typeName(this)}' and '${typeName(other)}'"
            )
        }
    }

    override fun negate(): NumericValue = BigNumericValue(bigIntNegate(value))

    override fun toComparableValue(originalValue: Any?): ComparableValue {
        return BigComparableValue(originalValue, value)
    }

    override fun toIntValue(): NumericValue = this

    override fun toFloatValue(): NumericValue {
        return FloatNumericValue(Number(value))
    }

    override fun toStringValue(): CharSequence {
        return value.toString()
    }

    override fun intOrNull(): Int? {
        val truncated = BigInt.asIntN(32, value)
        if (bigIntEq(truncated, value)) {
            return Number(truncated).toInt()
        }
        return null
    }

    override fun longOrNull(): Long? {
        val truncated = BigInt.asIntN(64, value)
        if (bigIntEq(truncated, value)) {
            return truncated.asDynamic()
        }
        return null
    }

    override fun doubleOrNull(): Double? = null

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
