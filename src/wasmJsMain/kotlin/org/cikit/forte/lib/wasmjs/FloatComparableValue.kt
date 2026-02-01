package org.cikit.forte.lib.wasmjs

import org.cikit.forte.core.ComparableValue
import org.cikit.forte.core.typeName

class FloatComparableValue(
    override val value: Any?,
    val converted: Double
) : ComparableValue {
    override fun compareTo(other: ComparableValue): Int {
        return when (other) {
            is FloatComparableValue -> converted.compareTo(other.converted)
            is BigComparableValue -> other.converted.compareTo(converted) * -1
            else -> error(
                "compareTo undefined for operands of type " +
                        "'${typeName(value)}' and '${typeName(other)}'"
            )
        }
    }
}
