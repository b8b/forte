package org.cikit.forte

import org.cikit.forte.core.ComparableValue
import org.cikit.forte.core.Context
import org.cikit.forte.core.NumericValue
import org.cikit.forte.lib.core.FilterComparable
import org.cikit.forte.lib.core.FilterNumber
import org.cikit.forte.lib.js.*
import kotlin.math.absoluteValue
import kotlin.reflect.KClass

actual fun <R>
        Context.Builder<R>.definePlatformExtensions(): Context.Builder<R>
{
    val comparableTypes = getMethod(FilterComparable.KEY)
        ?.types
        ?: error("${FilterComparable.KEY} is not defined")
    val jsComparableTypes: Map<KClass<*>,
                (Any?, Any, Boolean) -> ComparableValue> = hashMapOf(
        Int::class to { orig, value, _: Boolean ->
            value as Int
            FloatComparableValue(orig, value.toDouble())
        },
        Double::class to { orig, value, _: Boolean ->
            value as Double
            FloatComparableValue(orig, value)
        },
        Long::class to { orig, value, _: Boolean ->
            value as Long
            if (js("typeof value === 'bigint'")) {
                BigComparableValue(orig, value)
            } else if (value == 0L ||
                64 - value.absoluteValue.countLeadingZeroBits() <= 53)
            {
                FloatComparableValue(orig, value.toDouble())
            } else {
                BigComparableValue(orig, js("BigInt(value.toString())"))
            }
        }
    )
    defineMethod(
        FilterComparable.KEY,
        FilterComparable.DefaultFilterComparable(
            comparableTypes + jsComparableTypes
        )
    )
    val jsNumericTypes: Map<KClass<*>,
                (Any) -> NumericValue> = hashMapOf(
        Int::class to { value ->
            value as Int
            IntNumericValue(value)
        },
        Double::class to { value ->
            value as Double
            FloatNumericValue(value)
        },
        Long::class to { value ->
            value as Long
            if (js("typeof value === 'bigint'")) {
                BigNumericValue(value)
            } else if (value == 0L ||
                64 - value.absoluteValue.countLeadingZeroBits() <= 53)
            {
                FloatNumericValue(value.toDouble())
            } else {
                BigNumericValue(toBigInt(value.toString()))
            }
        },
    )
    defineMethod(
        FilterNumber.KEY,
        FilterNumber.DefaultFilterNumber(jsNumericTypes)
    )
    return this
}
