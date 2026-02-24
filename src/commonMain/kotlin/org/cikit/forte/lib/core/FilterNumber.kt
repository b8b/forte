package org.cikit.forte.lib.core

import org.cikit.forte.core.*
import org.cikit.forte.internal.parseInt
import kotlin.math.pow
import kotlin.reflect.KClass

interface FilterNumber : FilterMethod {

    companion object {
        val KEY: Context.Key.Apply<FilterNumber> =
            Context.Key.Apply.create("number", FilterMethod.OPERATOR)
    }

    val types: Map<KClass<*>, (Any) -> NumericValue>

    override fun invoke(subject: Any?, args: NamedArgs): NumericValue {
        args.requireEmpty()
        return invoke(subject)
    }

    operator fun invoke(subject: Any?): NumericValue

    fun requireNumber(n: Any?): NumericValue

    fun requireInt(n: Any?): Int {
        return requireNumber(n).intOrNull()
            ?: error("cannot convert argument 'n' to int")
    }

    fun requireLong(n: Any?): Long {
        return requireNumber(n).longOrNull()
            ?: error("cannot convert argument 'n' to long")
    }

    fun requireDouble(n: Any?): Double {
        return requireNumber(n).doubleOrNull()
            ?: error("cannot convert argument 'n' to double")
    }

    class DefaultFilterNumber(
        override val types: Map<KClass<*>, (Any) -> NumericValue> = hashMapOf(
            Byte::class to { value ->
                value as Byte
                IntNumericValue(value.toInt())
            },
            Short::class to { value ->
                value as Short
                IntNumericValue(value.toInt())
            },
            Int::class to { value ->
                value as Int
                IntNumericValue(value)
            },
            Long::class to { value ->
                value as Long
                LongNumericValue(value)
            },
            Float::class to { value ->
                value as Float
                FloatNumericValue(value.toDouble())
            },
            Double::class to { value ->
                value as Double
                FloatNumericValue(value)
            },
        )
    ) : FilterNumber {

        override val isHidden: Boolean
            get() = true

        override operator fun invoke(subject: Any?): NumericValue {
            return when (subject) {
                is NumericValue -> subject
                null -> null
                is Boolean -> IntNumericValue(if (subject) 1 else 0)
                is CharSequence -> try {
                    requireNumber(parseInt(subject))
                } catch (_: Throwable) {
                    val str: String = subject.toString()
                    FloatNumericValue(str.toDouble())
                }
                is Char -> IntNumericValue(subject.digitToInt())

                else -> types[subject::class]?.invoke(subject)
            } ?: error(
                "cannot convert operand of type ${typeName(subject)} to number"
            )
        }

        override fun requireNumber(n: Any?): NumericValue {
            return when (n) {
                is NumericValue -> n
                null -> null
                else -> types[n::class]?.invoke(n)
            } ?: throw IllegalArgumentException(
                "operand of type ${typeName(n)} is not a number"
            )
        }
    }

    private class IntNumericValue(
        val value: Int
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
                val newValue = value + other.value
                IntNumericValue(newValue)
            }

            is LongNumericValue -> {
                val newValue = value.toLong() + other.value
                LongNumericValue(newValue)
            }

            is FloatNumericValue -> {
                val newValue = value.toDouble() + other.value
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
                IntNumericValue(newValue)
            }

            is LongNumericValue -> {
                val newValue = value.toLong() - other.value
                LongNumericValue(newValue)
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
                val newValue = value * other.value
                IntNumericValue(newValue)
            }

            is LongNumericValue -> {
                val newValue = value.toLong() * other.value
                LongNumericValue(newValue)
            }

            is FloatNumericValue -> {
                val newValue = value.toDouble() * other.value
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
                if (newValue * other.value == value) {
                    IntNumericValue(newValue)
                } else {
                    val newFloat = value.toDouble() / other.value
                    FloatNumericValue(newFloat)
                }
            }

            is LongNumericValue -> {
                val big = value.toLong()
                val newValue = big / other.value
                if (newValue * other.value == big) {
                    LongNumericValue(newValue)
                } else {
                    val newFloat = value.toDouble() /
                            other.value.toDouble()
                    FloatNumericValue(newFloat)
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

            is LongNumericValue -> {
                val newValue = value.toLong() / other.value
                return LongNumericValue(newValue)
            }

            else -> error(
                "binary operator tdiv is undefined for operands of type " +
                        "'${typeName(this)}' and '${typeName(other)}'"
            )
        }

        override fun rem(other: NumericValue): NumericValue = when (other) {
            is IntNumericValue -> {
                val newValue = value.rem(other.value)
                IntNumericValue(newValue)
            }

            is LongNumericValue -> {
                val newValue = value.toLong().rem(other.value)
                LongNumericValue(newValue)
            }

            is FloatNumericValue -> {
                val newValue = value.toDouble().rem(other.value)
                FloatNumericValue(newValue)
            }

            else -> error(
                "binary operator rem is undefined for operands of type " +
                        "'${typeName(this)}' and '${typeName(other)}'"
            )
        }

        override fun pow(other: NumericValue): NumericValue = when (other) {
            is IntNumericValue -> {
                val newValue = value.toDouble().pow(other.value).toInt()
                IntNumericValue(newValue)
            }

            is LongNumericValue -> {
                val newValue = value.toDouble()
                    .pow(other.value.toDouble())
                    .toLong()
                LongNumericValue(newValue)
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

        override fun negate(): NumericValue = IntNumericValue(value * -1)

        override fun toComparableValue(originalValue: Any?): ComparableValue {
            return FilterComparable.FloatComparableValue(this, value.toDouble())
        }

        override fun toIntValue(): NumericValue = this

        override fun toFloatValue(): NumericValue {
            return FloatNumericValue(value.toDouble())
        }

        override fun toStringValue(): CharSequence = value.toString()

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

    private class LongNumericValue(
        val value: Long
    ) : NumericValue {

        override val result: Long
            get() = value

        override val isInt: Boolean
            get() = true

        override val isFloat: Boolean
            get() = false

        override val hasDecimalPart: Boolean
            get() = false

        override fun plus(other: NumericValue): NumericValue = when (other) {
            is IntNumericValue -> {
                val newValue = value + other.value.toLong()
                LongNumericValue(newValue)
            }

            is LongNumericValue -> {
                val newValue = value + other.value
                LongNumericValue(newValue)
            }

            is FloatNumericValue -> {
                val newValue = value.toDouble() + other.value
                FloatNumericValue(newValue)
            }

            else -> error(
                "binary operator plus is undefined for operands of type " +
                        "'${typeName(this)}' and '${typeName(other)}'"
            )
        }

        override fun minus(other: NumericValue): NumericValue = when (other) {
            is IntNumericValue -> {
                val newValue = value - other.value.toLong()
                LongNumericValue(newValue)
            }

            is LongNumericValue -> {
                val newValue = value - other.value
                LongNumericValue(newValue)
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
                val newValue = value * other.value.toLong()
                LongNumericValue(newValue)
            }

            is LongNumericValue -> {
                val newValue = value * other.value
                LongNumericValue(newValue)
            }

            is FloatNumericValue -> {
                val newValue = value.toDouble() * other.value
                FloatNumericValue(newValue)
            }

            else -> error(
                "binary operator mul is undefined for operands of type " +
                        "'${typeName(this)}' and '${typeName(other)}'"
            )
        }

        override fun div(other: NumericValue): NumericValue = when (other) {
            is IntNumericValue -> {
                val otherBig = other.value.toLong()
                val newValue = value / otherBig
                if (newValue * otherBig == value) {
                    LongNumericValue(newValue)
                } else {
                    val newFloat = value.toDouble() / other.value
                    FloatNumericValue(newFloat)
                }
            }

            is LongNumericValue -> {
                val newValue = value / other.value
                if (newValue * other.value == value) {
                    LongNumericValue(newValue)
                } else {
                    val newFloat = value.toDouble() /
                            other.value.toDouble()
                    FloatNumericValue(newFloat)
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
                val newValue = value / other.value.toLong()
                return LongNumericValue(newValue)
            }

            is LongNumericValue -> {
                val newValue = value / other.value
                return LongNumericValue(newValue)
            }

            else -> error(
                "binary operator tdiv is undefined for operands of type " +
                        "'${typeName(this)}' and '${typeName(other)}'"
            )
        }

        override fun rem(other: NumericValue): NumericValue = when (other) {
            is IntNumericValue -> {
                val newValue = value.rem(other.value.toLong())
                LongNumericValue(newValue)
            }

            is LongNumericValue -> {
                val newValue = value.rem(other.value)
                LongNumericValue(newValue)
            }

            is FloatNumericValue -> {
                val newValue = value.toDouble().rem(other.value)
                FloatNumericValue(newValue)
            }

            else -> error(
                "binary operator rem is undefined for operands of type " +
                        "'${typeName(this)}' and '${typeName(other)}'"
            )
        }

        override fun pow(other: NumericValue): NumericValue = when (other) {
            is IntNumericValue -> {
                val newValue = value.toDouble().pow(other.value).toInt()
                IntNumericValue(newValue)
            }

            is LongNumericValue -> {
                val newValue = value.toDouble()
                    .pow(other.value.toDouble())
                    .toLong()
                LongNumericValue(newValue)
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

        override fun negate(): NumericValue = LongNumericValue(value * -1L)

        override fun toComparableValue(originalValue: Any?): ComparableValue {
            return FilterComparable.LongComparableValue(this, value)
        }

        override fun toIntValue(): NumericValue = this

        override fun toFloatValue(): NumericValue {
            return FloatNumericValue(value.toDouble())
        }

        override fun toStringValue(): CharSequence = value.toString()

        override fun intOrNull(): Int? {
            val truncated = value.toInt()
            if (truncated.compareTo(value) == 0) {
                return truncated
            }
            return null
        }

        override fun longOrNull(): Long = value

        override fun doubleOrNull(): Double? = null

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as LongNumericValue

            return value == other.value
        }

        override fun hashCode(): Int {
            return value.hashCode()
        }

        override fun toString(): String {
            return "LongNumericValue($value)"
        }
    }

    private class FloatNumericValue(
        val value: Double
    ) : NumericValue {

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
                val newValue = value + other.value.toDouble()
                FloatNumericValue(newValue)
            }

            is LongNumericValue -> {
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
                val newValue = value - other.value.toDouble()
                FloatNumericValue(newValue)
            }

            is LongNumericValue -> {
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
                val newValue = value * other.value.toDouble()
                FloatNumericValue(newValue)
            }

            is LongNumericValue -> {
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
                val newValue = value / other.value.toDouble()
                FloatNumericValue(newValue)
            }

            is LongNumericValue -> {
                val newFloat = value / other.value.toDouble()
                FloatNumericValue(newFloat)
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

            is LongNumericValue -> {
                val newValue = value % other.value.toDouble()
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
                val newValue = value.pow(other.value.toDouble())
                FloatNumericValue(newValue)
            }

            is LongNumericValue -> {
                val newValue = value.pow(other.value.toDouble())
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
            return FilterComparable.FloatComparableValue(this, value)
        }

        override fun toIntValue(): NumericValue {
            return LongNumericValue(value.toLong())
        }

        override fun toFloatValue(): NumericValue = this

        override fun toStringValue(): CharSequence = value.toString()

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
}
