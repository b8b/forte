package org.cikit.forte.lib.js

import org.cikit.forte.core.ComparableValue
import org.cikit.forte.core.typeName

class BigComparableValue(
    override val value: Any?,
    val converted: dynamic
) : ComparableValue {
    override fun compareTo(other: ComparableValue): Int {
        return when (other) {
            is BigComparableValue ->
                dynamicCompareTo(converted, other.converted)
            is FloatComparableValue ->
                dynamicCompareTo(converted, other.converted)
            else -> error(
                "compareTo undefined for operands of type " +
                        "'${typeName(value)}' and '${typeName(other)}'"
            )
        }
    }
}
