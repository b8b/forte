package org.cikit.forte.lib.wasmjs

import com.ionspin.kotlin.bignum.integer.BigInteger
import org.cikit.forte.core.ComparableValue
import org.cikit.forte.core.typeName

class BigComparableValue(
    override val value: Any?,
    val converted: BigInteger
) : ComparableValue {
    override fun compareTo(other: ComparableValue): Int {
        return when (other) {
            is BigComparableValue -> converted.compareTo(other.converted)
            is FloatNumericValue -> converted.compareTo(other.converted)
            else -> error(
                "compareTo undefined for operands of type " +
                        "'${typeName(value)}' and '${typeName(other)}'"
            )
        }
    }
}
