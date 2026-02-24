package org.cikit.forte.lib.js

import org.cikit.forte.core.ComparableValue
import org.cikit.forte.core.NumericValue
import org.cikit.forte.core.typeName
import kotlin.math.pow

class BigNumericValue(
    val value: dynamic
) : NumericValue {

    override val result: Number
        get() = value

    override val isInt: Boolean
        get() = true

    override val isFloat: Boolean
        get() = false

    override val hasDecimalPart: Boolean
        get() = false

    override infix operator fun plus(
        other: NumericValue
    ): NumericValue = when (other) {
        is BigNumericValue -> {
            val newValue = BigInt.add(value, other.value)
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
            val newValue = BigInt.subtract(value, other.value)
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
            val newValue = BigInt.multiply(value, other.value)
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
            val newValue = BigInt.divide(value, other.value)
            if (BigInt.eq(value, BigInt.multiply(newValue, other.value))) {
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
            val newValue = BigInt.divide(value, other.value)
            return BigNumericValue(newValue)
        }

        else -> error(
            "binary operator tdiv is undefined for operands of type " +
                    "'${typeName(this)}' and '${typeName(other)}'"
        )
    }

    override fun rem(other: NumericValue): NumericValue = when (other) {
        is BigNumericValue -> {
            val newValue = BigInt.remainder(value, other.value)
            return BigNumericValue(newValue)
        }

        is FloatNumericValue -> {
            val newValue = Number(value).rem(other.value)
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
                // handle small numbers
                val lower = BigInt(-2)
                val upper = BigInt(2)
                if (BigInt.gt(value, lower) && BigInt.lt(value, upper)) {
                    return BigNumericValue(BigInt.pow(value, other.value))
                }
                if (BigInt.gt(other.value, lower) &&
                    BigInt.lt(other.value, upper))
                {
                    return BigNumericValue(BigInt.pow(value, other.value))
                }
                // BitLength(result) ~ BitLength(base) * exp
                // BitLength(result) < maxBitLength
                // => BitLength(base) * exp < maxBitLength
                // => BitLength(base) < maxBitLength / exp
                // => maxBase = 2 ^ (maxBitLength / exp)
                val exp2 = BigInt.divide(
                    BigInt(maxBitLength),
                    other.value
                )
                val maxBase = BigInt.pow(
                    upper,
                    exp2
                )
                if (BigInt.gt(value, maxBase)) {
                    throw ArithmeticException("base or exponent too high")
                }
                val newValue = BigInt.pow(value, other.value)
                BigNumericValue(newValue)
            }

            is FloatNumericValue -> {
                val a = Number(value) as Double
                val b = other.value
                val newValue = a.pow(b)
                FloatNumericValue(newValue)
            }

            else -> error(
                "binary operator pow is undefined for operands of type " +
                        "'${typeName(this)}' and '${typeName(other)}'"
            )
        }
    }

    override fun negate(): NumericValue {
        val newValue = BigInt.negate(value)
        return BigNumericValue(newValue)
    }

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
        if (BigInt.eq(truncated, value)) {
            return Number(truncated) as Int
        }
        return null
    }

    override fun longOrNull(): Long? {
        val truncated = BigInt.asIntN(64, value)
        if (BigInt.eq(truncated, value)) {
            return truncated
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
