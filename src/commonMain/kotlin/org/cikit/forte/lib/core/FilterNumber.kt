package org.cikit.forte.lib.core

import org.cikit.forte.core.*
import kotlin.math.pow
import kotlin.reflect.KClass

class FilterNumber(
    val types: Map<KClass<*>, (Any?, Any) -> NumericValue> = hashMapOf(
        Byte::class to { orig, value ->
            value as Byte
            IntNumericValue(orig, value.toInt())
        },
        Short::class to { orig, value ->
            value as Short
            IntNumericValue(orig, value.toInt())
        },
        Int::class to { orig, value ->
            value as Int
            IntNumericValue(orig, value)
        },
        Long::class to { orig, value ->
            value as Long
            LongNumericValue(orig, value)
        },
        Float::class to { orig, value ->
            value as Float
            FloatNumericValue(orig, value.toDouble())
        },
        Double::class to { orig, value ->
            value as Double
            FloatNumericValue(orig, value)
        },
    )
) : FilterMethod {

    companion object {
        val KEY: Context.Key.Apply<FilterNumber> =
            Context.Key.Apply.create("number", FilterMethod.OPERATOR)
    }

    override val isHidden: Boolean
        get() = true

    override fun invoke(subject: Any?, args: NamedArgs): NumericValue {
        args.requireEmpty()
        return invoke(subject)
    }

    operator fun invoke(subject: Any?): NumericValue {
        return when (subject) {
            null -> null
            is NumericValue -> subject
            else -> types[subject::class]?.invoke(
                subject,
                subject,
            )
        } ?: error("operand of type ${typeName(subject)} is not a number")
    }

    private class IntNumericValue(
        override val value: Any?,
        val converted: Int
    ): Number(), NumericValue {

        override val isInt: Boolean
            get() = true

        override val isFloat: Boolean
            get() = false

        override val hasDecimalPart: Boolean
            get() = false

        override fun plus(other: NumericValue): NumericValue = when (other) {
            is IntNumericValue -> {
                val newValue = converted + other.converted
                //FIXME check for overflow
                IntNumericValue(newValue, newValue)
            }

            is LongNumericValue -> {
                val newValue = converted + other.converted
                //FIXME check for overflow
                LongNumericValue(newValue, newValue)
            }

            is FloatNumericValue -> {
                val newValue = other.converted.plus(converted)
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
                //FIXME check for overflow
                IntNumericValue(newValue, newValue)
            }

            is LongNumericValue -> {
                val newValue = converted.toLong() - other.converted
                //FIXME check for overflow
                LongNumericValue(newValue, newValue)
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
                val newValue = converted * other.converted
                //FIXME check for overflow
                IntNumericValue(newValue, newValue)
            }

            is LongNumericValue -> {
                val newValue = converted.toLong() * other.converted
                //FIXME check for overflow
                LongNumericValue(newValue, newValue)
            }

            is FloatNumericValue -> {
                val newValue = converted * other.converted
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
                if (newValue * other.converted == converted) {
                    IntNumericValue(newValue, newValue)
                } else {
                    val newFloat = converted.toDouble() / other.converted
                    FloatNumericValue(newFloat, newFloat)
                }
            }

            is LongNumericValue -> {
                val big = converted.toLong()
                val newValue = big.div(other.converted)
                if (newValue * other.converted == big) {
                    LongNumericValue(newValue, newValue)
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
                val newValue = converted / other.converted
                return IntNumericValue(newValue, newValue)
            }

            is LongNumericValue -> {
                val big = converted.toLong()
                val newValue = big.div(other.converted)
                return LongNumericValue(newValue, newValue)
            }

            else -> error(
                "binary operator tdiv is undefined for operands of type " +
                        "'${typeName(this)}' and '${typeName(other)}'"
            )
        }

        override fun rem(other: NumericValue): NumericValue = when (other) {
            is IntNumericValue -> {
                val newValue = converted.rem(other.converted)
                IntNumericValue(newValue, newValue)
            }

            is LongNumericValue -> {
                val newValue = converted.rem(other.converted)
                LongNumericValue(newValue, newValue)
            }

            is FloatNumericValue -> {
                val newValue = converted.rem(other.converted)
                FloatNumericValue(newValue, newValue)
            }

            else -> error(
                "binary operator rem is undefined for operands of type " +
                        "'${typeName(this)}' and '${typeName(other)}'"
            )
        }

        override fun pow(other: NumericValue): NumericValue = when (other){
            is IntNumericValue -> {
                val newValue = converted.toDouble().pow(other.converted)
                FloatNumericValue(newValue, newValue)
            }

            is LongNumericValue -> {
                val newValue = converted.toDouble().pow(
                    other.converted.toDouble()
                )
                FloatNumericValue(newValue, newValue)
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

        override fun toIntValue(): NumericValue = this

        override fun toFloatValue(): NumericValue {
            return FloatNumericValue(value, converted.toDouble())
        }

        override fun toStringValue(): CharSequence = converted.toString()

        override fun toDouble(): Double = converted.toDouble()

        override fun toFloat(): Float = converted.toFloat()

        override fun toLong(): Long = converted.toLong()

        override fun toInt(): Int = converted

        override fun toShort(): Short = converted.toShort()

        override fun toByte(): Byte = converted.toByte()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as IntNumericValue

            return converted == other.converted
        }

        override fun hashCode(): Int {
            return converted
        }

        override fun toString(): String {
            return "IntNumericValue($converted)"
        }
    }

    private class LongNumericValue(
        override val value: Any?,
        val converted: Long
    ): Number(), NumericValue {

        override val isInt: Boolean
            get() = true

        override val isFloat: Boolean
            get() = false

        override val hasDecimalPart: Boolean
            get() = false

        override fun plus(other: NumericValue): NumericValue = when (other) {
            is IntNumericValue -> {
                val newValue = converted + other.converted
                //FIXME check for overflow
                LongNumericValue(newValue, newValue)
            }

            is LongNumericValue -> {
                val newValue = converted + other.converted
                //FIXME check for overflow
                LongNumericValue(newValue, newValue)
            }

            is FloatNumericValue -> {
                val newValue = other.converted.plus(converted)
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
                //FIXME check for overflow
                LongNumericValue(newValue, newValue)
            }

            is LongNumericValue -> {
                val newValue = converted - other.converted
                //FIXME check for overflow
                LongNumericValue(newValue, newValue)
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
                val newValue = converted * other.converted
                //FIXME check for overflow
                LongNumericValue(newValue, newValue)
            }

            is LongNumericValue -> {
                val newValue = converted * other.converted
                //FIXME check for overflow
                LongNumericValue(newValue, newValue)
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
                val otherBig = other.converted.toLong()
                val newValue = converted / otherBig
                if (newValue * otherBig == converted) {
                    LongNumericValue(newValue, newValue)
                } else {
                    val newFloat = converted.toDouble() / other.converted
                    FloatNumericValue(newFloat, newFloat)
                }
            }

            is LongNumericValue -> {
                val newValue = converted / other.converted
                if (newValue * other.converted == converted) {
                    LongNumericValue(newValue, newValue)
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
                val newValue = converted / other.converted
                return LongNumericValue(newValue, newValue)
            }

            is LongNumericValue -> {
                val newValue = converted / other.converted
                return LongNumericValue(newValue, newValue)
            }

            else -> error(
                "binary operator tdiv is undefined for operands of type " +
                        "'${typeName(this)}' and '${typeName(other)}'"
            )
        }

        override fun rem(other: NumericValue): NumericValue = when (other) {
            is IntNumericValue -> {
                val newValue = converted.rem(other.converted)
                LongNumericValue(newValue, newValue)
            }

            is LongNumericValue -> {
                val newValue = converted.rem(other.converted)
                LongNumericValue(newValue, newValue)
            }

            is FloatNumericValue -> {
                val newValue = converted.rem(other.converted)
                FloatNumericValue(newValue, newValue)
            }

            else -> error(
                "binary operator rem is undefined for operands of type " +
                        "'${typeName(this)}' and '${typeName(other)}'"
            )
        }

        override fun pow(other: NumericValue): NumericValue = when (other){
            is IntNumericValue -> {
                val newValue = converted.toDouble().pow(other.converted)
                FloatNumericValue(newValue, newValue)
            }

            is LongNumericValue -> {
                val newValue = converted.toDouble().pow(
                    other.converted.toDouble()
                )
                FloatNumericValue(newValue, newValue)
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

        override fun toIntValue(): NumericValue = this

        override fun toFloatValue(): NumericValue {
            return FloatNumericValue(value, converted.toDouble())
        }

        override fun toStringValue(): CharSequence = converted.toString()

        override fun toDouble(): Double = converted.toDouble()

        override fun toFloat(): Float = converted.toFloat()

        override fun toLong(): Long = converted

        override fun toInt(): Int = converted.toInt()

        override fun toShort(): Short = converted.toInt().toShort()

        override fun toByte(): Byte = converted.toInt().toByte()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as LongNumericValue

            return converted == other.converted
        }

        override fun hashCode(): Int {
            return converted.hashCode()
        }

        override fun toString(): String {
            return "LongNumericValue($converted)"
        }
    }

    private class FloatNumericValue(
        override val value: Any?,
        val converted: Double
    ): Number(), NumericValue {

        override val isInt: Boolean
            get() = false

        override val isFloat: Boolean
            get() = true

        override val hasDecimalPart: Boolean
            get() = converted % 1 != 0.0

        override fun plus(other: NumericValue): NumericValue = when (other) {
            is IntNumericValue -> {
                val newValue = converted + other.converted
                FloatNumericValue(newValue, newValue)
            }

            is LongNumericValue -> {
                val newValue = converted + other.converted
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

            is LongNumericValue -> {
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

            is LongNumericValue -> {
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

            is LongNumericValue -> {
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
            is IntNumericValue -> {
                val newValue = converted % other.converted
                FloatNumericValue(newValue, newValue)
            }

            is LongNumericValue -> {
                val newValue = converted % other.converted.toDouble()
                FloatNumericValue(newValue, newValue)
            }

            is FloatNumericValue -> {
                val newValue = converted % other.converted
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
                FloatNumericValue(newValue, newValue)
            }

            is LongNumericValue -> {
                val newValue = converted.pow(other.converted.toDouble())
                FloatNumericValue(newValue, newValue)
            }

            is FloatNumericValue -> {
                val newValue = converted.pow(other.converted)
                FloatNumericValue(newValue, newValue)
            }

            else -> error(
                "binary operator pow is undefined for operands of type " +
                        "'${typeName(this)}' and '${typeName(other)}'"
            )
        }

        override fun toIntValue(): NumericValue {
            return LongNumericValue(value, converted.toLong())
        }

        override fun toFloatValue(): NumericValue = this

        override fun toStringValue(): CharSequence = converted.toString()

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
}
