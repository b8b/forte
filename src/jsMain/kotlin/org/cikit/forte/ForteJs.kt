package org.cikit.forte

import com.ionspin.kotlin.bignum.integer.BigInteger
import org.cikit.forte.core.ComparableValue
import org.cikit.forte.core.Context
import org.cikit.forte.core.NumericValue
import org.cikit.forte.lib.core.FilterComparable
import org.cikit.forte.lib.core.FilterNumber
import org.cikit.forte.lib.js.BigComparableValue
import org.cikit.forte.lib.js.BigNumericValue
import org.cikit.forte.lib.js.FloatNumericValue
import org.cikit.forte.lib.js.IntNumericValue
import kotlin.math.absoluteValue
import kotlin.reflect.KClass

actual fun <R>
        Context.Builder<R>.definePlatformExtensions(): Context.Builder<R>
{
    val comparableTypes = getMethod(FilterComparable.KEY)
        ?.types
        ?: error("${FilterComparable.KEY} is not defined")
    val wasmJsComparableTypes: Map<KClass<*>,
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
                BigComparableValue(orig, BigInteger.fromLong(value))
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
            BigComparableValue(orig, value as BigInteger)
        },
        BigNumericValue::class to { orig, value, _: Boolean ->
            value as BigNumericValue
            BigComparableValue(orig, value.converted)
        },
    )
    defineMethod(
        FilterComparable.KEY,
        FilterComparable(comparableTypes + wasmJsComparableTypes)
    )
    val wasmJsNumericTypes: Map<KClass<*>,
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
        Long::class to { _, value ->
            value as Long
            BigNumericValue(BigInteger.fromLong(value))
        },
        Float::class to { orig, value ->
            value as Float
            FloatNumericValue(orig, value.toDouble())
        },
        Double::class to { orig, value ->
            value as Double
            FloatNumericValue(orig, value)
        },
        BigInteger::class to { _, value ->
            BigNumericValue(value as BigInteger)
        },
    )
    defineMethod(
        FilterNumber.KEY,
        FilterNumber(wasmJsNumericTypes)
    )
    return this
}
