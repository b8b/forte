package org.cikit.forte.lib.wasmjs

import dev.erikchristensen.javamath2kmp.minusExact
import dev.erikchristensen.javamath2kmp.negateExact
import dev.erikchristensen.javamath2kmp.plusExact
import dev.erikchristensen.javamath2kmp.timesExact
import org.cikit.forte.core.ComparableValue
import org.cikit.forte.core.NumericValue
import org.cikit.forte.core.typeName
import kotlin.math.pow

class IntNumericValue(
    val value: Int,
) : NumericValue {

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
                val newValue = bigIntAdd(
                    BigInt(value.toDouble()),
                    BigInt(other.value.toDouble())
                )
                BigNumericValue(newValue)
            }
        }

        is BigNumericValue -> {
            val newValue = bigIntAdd(BigInt(value.toDouble()), other.value)
            BigNumericValue(newValue)
        }

        is FloatNumericValue -> {
            val newValue = other.value.plus(value)
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
                val newValue = bigIntSubtract(
                    BigInt(value.toDouble()),
                    BigInt(other.value.toDouble())
                )
                BigNumericValue(newValue)
            }
        }

        is BigNumericValue -> {
            val newValue = bigIntSubtract(BigInt(value.toDouble()), other.value)
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
                val newValue = bigIntMultiply(
                    BigInt(value.toDouble()),
                    BigInt(other.value.toDouble())
                )
                BigNumericValue(newValue)
            }
        }

        is BigNumericValue -> {
            val newValue = bigIntMultiply(BigInt(value.toDouble()), other.value)
            BigNumericValue(newValue)
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
            val remainder = value % other.value
            if (remainder == 0) {
                val newValue = value / other.value
                IntNumericValue(newValue)
            } else {
                val newValue = value.toDouble() / other.value
                FloatNumericValue(newValue)
            }
        }

        is BigNumericValue -> {
            val big = BigInt(value.toDouble())
            val remainder = bigIntRemainder(big, other.value)
            if (bigIntEq(remainder, BigInt(0.0))) {
                val newValue = bigIntDivide(big, other.value)
                BigNumericValue(newValue)
            } else {
                val newValue = value.toDouble() / Number(other.value)
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
            val big = BigInt(value.toDouble())
            val newValue = bigIntDivide(big, other.value)
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
            val newValue = bigIntRemainder(
                BigInt(value.toDouble()),
                other.value
            )
            return BigNumericValue(newValue)
        }

        is FloatNumericValue -> {
            val newValue = value.rem(other.value)
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
                restrictedPow(
                    BigInt(value.toDouble()),
                    other.value,
                    maxBitLength
                )
            }

            is IntNumericValue -> {
                val result = restrictedPow(
                    BigInt(value.toDouble()),
                    BigInt(other.value.toDouble()),
                    maxBitLength
                )
                val intResult = result.intOrNull()
                if (intResult == null) {
                    result
                } else {
                    IntNumericValue(intResult)
                }
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

    override fun negate(): NumericValue {
        return IntNumericValue(value.negateExact())
    }

    override fun toComparableValue(originalValue: Any?): ComparableValue {
        return FloatComparableValue.DirectComparableValue(
            originalValue,
            value.toDouble()
        )
    }

    override fun toIntValue(): NumericValue = this

    override fun toFloatValue(): NumericValue {
        return FloatNumericValue(value.toDouble())
    }

    override fun toStringValue(): CharSequence {
        return value.toString()
    }

    override fun intOrNull(): Int = value

    override fun longOrNull(): Long = value.toLong()

    override fun doubleOrNull(): Double? = null

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
