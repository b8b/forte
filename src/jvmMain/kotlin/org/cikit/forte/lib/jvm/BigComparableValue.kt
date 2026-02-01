package org.cikit.forte.lib.jvm

import org.cikit.forte.core.ComparableValue
import org.cikit.forte.core.typeName
import java.math.BigDecimal
import java.math.BigInteger

class BigComparableValue(
    override val value: Any?,
    val converted: BigInteger
) : ComparableValue {
    override fun compareTo(other: ComparableValue): Int {
        return when (other) {
            is BigComparableValue -> converted.compareTo(other.converted)
            is FloatComparableValue -> BigDecimal(converted)
                .compareTo(BigDecimal.valueOf(other.converted))
            else -> error(
                "compareTo undefined for operands of type " +
                        "'${typeName(value)}' and '${typeName(other)}'"
            )
        }
    }
}
