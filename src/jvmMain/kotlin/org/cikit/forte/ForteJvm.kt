package org.cikit.forte

import org.cikit.forte.core.ComparableValue
import org.cikit.forte.core.Context
import org.cikit.forte.core.NumericValue
import org.cikit.forte.lib.core.FilterComparable
import org.cikit.forte.lib.core.FilterNumber
import org.cikit.forte.lib.jvm.BigComparableValue
import org.cikit.forte.lib.jvm.BigNumericValue
import org.cikit.forte.lib.jvm.FloatComparableValue
import org.cikit.forte.lib.jvm.FloatNumericValue
import org.cikit.forte.lib.jvm.IntNumericValue
import java.math.BigInteger
import kotlin.math.absoluteValue
import kotlin.reflect.KClass

actual fun <R>
        Context.Builder<R>.definePlatformExtensions(): Context.Builder<R>
{
    val comparableTypes = getMethod(FilterComparable.KEY)
        ?.types
        ?: error("${FilterComparable.KEY} is not defined")
    val jvmComparableTypes: Map<KClass<*>,
                (Any?, Any, Boolean) -> ComparableValue?> = hashMapOf(
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
                BigComparableValue(orig, BigInteger.valueOf(value))
            }
        },
        Float::class to { orig, value, _: Boolean ->
            value as Float
            if (value.isNaN()) {
                null
            } else {
                FloatComparableValue(orig, value.toDouble())
            }
        },
        Double::class to { orig, value, _: Boolean ->
            value as Double
            if (value.isNaN()) {
                null
            } else {
                FloatComparableValue(orig, value)
            }
        },
        BigInteger::class to { orig, value, _: Boolean ->
            value as BigInteger
            BigComparableValue(orig, value)
        },
    )
    defineMethod(
        FilterComparable.KEY,
        FilterComparable.DefaultFilterComparable(
            comparableTypes + jvmComparableTypes
        )
    )
    val jvmNumericTypes: Map<KClass<*>,
                (Any) -> NumericValue> = hashMapOf(
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
            BigNumericValue(BigInteger.valueOf(value))
        },
        Float::class to { value ->
            value as Float
            FloatNumericValue(value.toDouble())
        },
        Double::class to { value ->
            value as Double
            FloatNumericValue(value)
        },
        BigInteger::class to { value ->
            value as BigInteger
            BigNumericValue(value)
        },
    )
    defineMethod(
        FilterNumber.KEY,
        FilterNumber.DefaultFilterNumber(jvmNumericTypes)
    )
    return this
}
