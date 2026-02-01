package org.cikit.forte.lib.js

import org.cikit.forte.core.ComparableValue
import org.cikit.forte.core.typeName

class FloatComparableValue(
    override val value: Any?,
    val converted: Double
) : ComparableValue {
    override fun compareTo(other: ComparableValue): Int {
        return when (other) {
            is FloatComparableValue ->
                converted.compareTo(other.converted)
            is BigComparableValue ->
                dynamicCompareTo(converted, other.converted)
            else -> error(
                "compareTo undefined for operands of type " +
                        "'${typeName(value)}' and '${typeName(other)}'"
            )
        }
    }
}
