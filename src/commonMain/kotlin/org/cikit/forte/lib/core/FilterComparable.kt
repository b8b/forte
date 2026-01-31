package org.cikit.forte.lib.core

import kotlinx.io.bytestring.ByteString
import org.cikit.forte.core.ComparableValue
import org.cikit.forte.core.Context
import org.cikit.forte.core.FilterMethod
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.NumericValue
import org.cikit.forte.core.concatToString
import org.cikit.forte.core.optional
import org.cikit.forte.core.typeName
import kotlin.math.absoluteValue
import kotlin.reflect.KClass

class FilterComparable(
    val types: Map<KClass<*>,
                (Any?, Any, Boolean) -> ComparableValue> = hashMapOf(
        Byte::class to { orig, value, _: Boolean ->
            value as Byte
            FloatComparableValue(orig, value.toDouble())
        },
        Short::class to { orig, value, _: Boolean ->
            value as Short
            FloatComparableValue(orig, value.toDouble())
        },
        Int::class to { orig, value, _: Boolean ->
            value as Int
            FloatComparableValue(orig, value.toDouble())
        },
        Long::class to { orig, value, _: Boolean ->
            value as Long
            if (value == 0L ||
                64 - value.absoluteValue.countLeadingZeroBits() <= 53)
            {
                FloatComparableValue(orig, value.toDouble())
            } else {
                LongComparableValue(orig, value)
            }
        },
        Float::class to { orig, value, _: Boolean ->
            value as Float
            FloatComparableValue(orig, value.toDouble())
        },
        Double::class to { orig, value, _: Boolean ->
            value as Double
            FloatComparableValue(orig, value)
        },
        Char::class to { orig, value, ignoreCase ->
            value as Char
            StringComparableValue(orig, value.toString(), ignoreCase)
        },
        ByteString::class to { orig, value, _: Boolean ->
            @Suppress("UNCHECKED_CAST")
            GenericComparableValue(orig, value as Comparable<Any>)
        },
    )
) : FilterMethod {

    companion object {
        val KEY: Context.Key.Apply<FilterComparable> =
            Context.Key.Apply.create("comparable", FilterMethod.OPERATOR)
    }

    override val isHidden: Boolean
        get() = true

    override fun invoke(subject: Any?, args: NamedArgs): Any {
        val caseSensitive: Boolean
        args.use {
            caseSensitive = optional("case_sensitive") { false }
        }
        return invoke(subject, subject, ignoreCase = !caseSensitive)
    }

    operator fun invoke(
        subject: Any?,
        originalValue: Any? = subject,
        ignoreCase: Boolean = false
    ): ComparableValue = test(subject, originalValue, ignoreCase)
        ?: error(
            "operand of type '${typeName(subject)}' is not comparable"
        )

    fun test(
        subject: Any?,
        originalValue: Any? = subject,
        ignoreCase: Boolean = false
    ): ComparableValue? {
        return when (subject) {
            null -> null
            is ComparableValue -> subject
            is NumericValue -> subject.value?.let { value ->
                types[value::class]?.invoke(originalValue, value, ignoreCase)
            }
            is CharSequence -> StringComparableValue(
                originalValue,
                subject.concatToString(),
                ignoreCase
            )
            is Iterable<*> -> ListComparableValue(
                originalValue,
                subject.map { invoke(it, it, ignoreCase) }
            )

            else -> types[subject::class]?.invoke(
                originalValue,
                subject,
                ignoreCase
            )
        }
    }

    private class ListComparableValue(
        override val value: Any?,
        val converted: Iterable<ComparableValue>
    ) : ComparableValue {
        override fun compareTo(other: ComparableValue): Int {
            require(other is ListComparableValue) {
                "compareTo undefined for operands of type " +
                        "'${typeName(value)}' and '${typeName(other)}'"
            }
            val itLeft = converted.iterator()
            val itRight = other.converted.iterator()
            while (true) {
                val left = if (itLeft.hasNext()) {
                    itLeft.next()
                } else {
                    null
                }
                val right = if (itRight.hasNext()) {
                    itRight.next()
                } else {
                    null
                }
                if (left == null) {
                    if (right == null) {
                        break
                    }
                    return -1
                } else if (right == null) {
                    return 1
                }
                val result = left.compareTo(right)
                if (result != 0) {
                    return result
                }
            }
            return 0
        }
    }

    private class StringComparableValue(
        override val value: Any?,
        val converted: String,
        val ignoreCase: Boolean = false
    ) : ComparableValue {
        override fun compareTo(other: ComparableValue): Int {
            require(other is StringComparableValue) {
                "compareTo undefined for operands of type " +
                        "'${typeName(value)}' and '${typeName(other)}'"
            }
            return converted.compareTo(other.converted, ignoreCase)
        }
    }

    private class LongComparableValue(
        override val value: Any?,
        val converted: Long
    ) : ComparableValue {
        override fun compareTo(other: ComparableValue): Int {
            return when (other) {
                is LongComparableValue -> converted.compareTo(other.converted)
                is FloatComparableValue -> converted.compareTo(other.converted)
                else -> error(
                    "compareTo undefined for operands of type " +
                            "'${typeName(value)}' and '${typeName(other)}'"
                )
            }
        }
    }

    private class FloatComparableValue(
        override val value: Any?,
        val converted: Double
    ) : ComparableValue {
        override fun compareTo(other: ComparableValue): Int {
            return when (other) {
                is FloatComparableValue -> converted.compareTo(other.converted)
                is LongComparableValue -> converted.compareTo(other.converted)
                else -> error(
                    "compareTo undefined for operands of type " +
                            "'${typeName(value)}' and '${typeName(other)}'"
                )
            }
        }
    }

    private class GenericComparableValue(
        override val value: Any?,
        val converted: Comparable<Any>
    ) : ComparableValue {
        @Suppress("UNCHECKED_CAST")
        override fun compareTo(other: ComparableValue): Int {
            require(other is GenericComparableValue &&
                    converted::class == other.converted::class)
            {
                "compareTo undefined for operands of type " +
                        "'${typeName(converted)}' and '${typeName(other)}'"
            }
            return converted.compareTo(other.converted)
        }
    }

}
