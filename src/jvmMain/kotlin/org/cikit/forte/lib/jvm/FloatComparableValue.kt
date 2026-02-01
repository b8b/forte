package org.cikit.forte.lib.jvm

import org.cikit.forte.core.ComparableValue
import org.cikit.forte.core.typeName
import java.math.BigDecimal

class FloatComparableValue(
    override val value: Any?,
    val converted: Double
) : ComparableValue {
    override fun compareTo(other: ComparableValue): Int {
        return when (other) {
            is FloatComparableValue -> converted.compareTo(other.converted)
            is BigComparableValue -> BigDecimal.valueOf(converted)
                .compareTo(BigDecimal(other.converted))
            else -> error(
                "compareTo undefined for operands of type " +
                        "'${typeName(value)}' and '${typeName(other)}'"
            )
        }
    }
}
