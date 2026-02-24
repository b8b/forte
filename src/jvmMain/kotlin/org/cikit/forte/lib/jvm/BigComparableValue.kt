package org.cikit.forte.lib.jvm

import org.cikit.forte.core.ComparableValue
import org.cikit.forte.core.typeName
import java.math.BigInteger

class BigComparableValue(
    override val value: Any?,
    val converted: BigInteger
) : ComparableValue {
    override fun compareTo(other: ComparableValue): Int {
        return when (other) {
            is BigComparableValue -> converted.compareTo(other.converted)
            is FloatComparableValue -> other.compareTo(this) * -1
            else -> error(
                "compareTo undefined for operands of type " +
                        "'${typeName(value)}' and '${typeName(other)}'"
            )
        }
    }
}
