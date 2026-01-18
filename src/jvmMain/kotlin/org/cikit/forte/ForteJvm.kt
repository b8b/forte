package org.cikit.forte

import org.cikit.forte.core.ComparableValue
import org.cikit.forte.core.Context
import org.cikit.forte.core.NumericValue
import org.cikit.forte.lib.core.FilterComparable
import org.cikit.forte.lib.core.FilterNumber
import org.cikit.forte.lib.jvm.BigNumericValue
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
                (Any?, Any, Boolean) -> ComparableValue> = hashMapOf(
        Byte::class to { orig, value, _: Boolean ->
            value as Byte
            FloatNumericValue(orig, value.toDouble())
        },
        Short::class to { orig, value, _: Boolean ->
            value as Short
            FloatNumericValue(orig, value.toDouble())
        },
        Int::class to { orig, value, _: Boolean ->
            value as Int
            FloatNumericValue(orig, value.toDouble())
        },
        Long::class to { orig, value, _: Boolean ->
            value as Long
            if (value == 0L ||
                64 - value.absoluteValue.countLeadingZeroBits() <= 53)
            {
                FloatNumericValue(orig, value.toDouble())
            } else {
                BigNumericValue(orig, BigInteger.valueOf(value))
            }
        },
        Float::class to { orig, value, _: Boolean ->
            value as Float
            FloatNumericValue(orig, value.toDouble())
        },
        Double::class to { orig, value, _: Boolean ->
            value as Double
            FloatNumericValue(orig, value)
        },
        BigInteger::class to { orig, value, _: Boolean ->
            value as BigInteger
            BigNumericValue(orig, value)
        },
    )
    defineMethod(
        FilterComparable.KEY,
        FilterComparable(comparableTypes + jvmComparableTypes)
    )
    val jvmNumericTypes: Map<KClass<*>,
                (Any?, Any) -> NumericValue> = hashMapOf(
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
            BigNumericValue(orig, BigInteger.valueOf(value))
        },
        Float::class to { orig, value ->
            value as Float
            FloatNumericValue(orig, value.toDouble())
        },
        Double::class to { orig, value ->
            value as Double
            FloatNumericValue(orig, value)
        },
        BigInteger::class to { orig, value ->
            value as BigInteger
            BigNumericValue(orig, value)
        },
    )
    defineMethod(
        FilterNumber.KEY,
        FilterNumber(jvmNumericTypes)
    )
    return this
}
