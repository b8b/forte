package org.cikit.forte

import com.ionspin.kotlin.bignum.integer.BigInteger
import org.cikit.forte.core.ComparableValue
import org.cikit.forte.core.Context
import org.cikit.forte.core.NumericValue
import org.cikit.forte.lib.core.FilterComparable
import org.cikit.forte.lib.core.FilterNumber
import org.cikit.forte.lib.wasmjs.BigComparableValue
import org.cikit.forte.lib.wasmjs.BigNumericValue
import org.cikit.forte.lib.wasmjs.FloatComparableValue
import org.cikit.forte.lib.wasmjs.FloatNumericValue
import org.cikit.forte.lib.wasmjs.IntNumericValue
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
            FloatComparableValue.DirectComparableValue(orig, value.toDouble())
        },
        Short::class to { orig, value, _: Boolean ->
            value as Short
            FloatComparableValue.DirectComparableValue(orig, value.toDouble())
        },
        Int::class to { orig, value, _: Boolean ->
            value as Int
            FloatComparableValue.DirectComparableValue(orig, value.toDouble())
        },
        Long::class to { orig, value, _: Boolean ->
            value as Long
            if (value == 0L ||
                64 - value.absoluteValue.countLeadingZeroBits() <= 53)
            {
                FloatComparableValue.DirectComparableValue(
                    orig,
                    value.toDouble()
                )
            } else {
                BigComparableValue(orig, BigInteger.fromLong(value))
            }
        },
        Float::class to { orig, value, _: Boolean ->
            value as Double
            FloatNumericValue(value).toComparableValue(orig)
        },
        Double::class to { orig, value, _: Boolean ->
            value as Double
            FloatNumericValue(value).toComparableValue(orig)
        },
        BigInteger::class to { orig, value, _: Boolean ->
            BigComparableValue(orig, value as BigInteger)
        },
    )
    defineMethod(
        FilterComparable.KEY,
        FilterComparable.DefaultFilterComparable(
            comparableTypes + wasmJsComparableTypes
        )
    )
    val wasmJsNumericTypes: Map<KClass<*>,
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
            BigNumericValue(BigInteger.fromLong(value))
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
        FilterNumber.DefaultFilterNumber(wasmJsNumericTypes)
    )
    return this
}
