package org.cikit.forte.lib.wasmjs

import org.cikit.forte.core.ComparableValue
import org.cikit.forte.core.typeName

class BigComparableValue(
    override val value: Any?,
    val converted: BigInt
) : ComparableValue {
    override fun compareTo(other: ComparableValue): Int {
        return when (other) {
            is BigComparableValue -> {
                bigIntCompare(converted, other.converted)
            }
            is FloatComparableValue -> {
                other.compareTo(this) * -1
            }
            else -> error(
                "compareTo undefined for operands of type " +
                        "'${typeName(value)}' and '${typeName(other)}'"
            )
        }
    }
}
