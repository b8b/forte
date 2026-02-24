package org.cikit.forte.lib.wasmjs

import com.ionspin.kotlin.bignum.integer.BigInteger
import org.cikit.forte.core.ComparableValue
import org.cikit.forte.core.typeName

sealed interface FloatComparableValue : ComparableValue {
    val converted: Double

    class DirectComparableValue(
        override val value: Any?,
        override val converted: Double
    ) : FloatComparableValue {
        override fun compareTo(other: ComparableValue): Int {
            return when (other) {
                is FloatComparableValue -> {
                    converted.compareTo(other.converted)
                }

                is BigComparableValue -> {
                    // converted is finite and has no fractional part
                    other.converted.compareTo(converted) * -1
                }

                else -> error(
                    "compareTo undefined for operands of type " +
                            "'${typeName(value)}' and '${typeName(other)}'"
                )
            }
        }
    }

    class NegativeComparableValue(
        override val value: Any?,
        override val converted: Double
    ) : FloatComparableValue {
        override fun compareTo(other: ComparableValue): Int {
            return when (other) {
                is FloatComparableValue -> {
                    converted.compareTo(other.converted)
                }
                is BigComparableValue -> {
                    if (converted == Double.NEGATIVE_INFINITY) {
                        -1
                    } else {
                        val cmp = BigInteger
                            .tryFromDouble(converted, exactRequired = false)
                            .compareTo(other.converted)
                        if (cmp == 0) {
                            -1
                        } else {
                            cmp
                        }
                    }
                }

                else -> error(
                    "compareTo undefined for operands of type " +
                            "'${typeName(value)}' and '${typeName(other)}'"
                )
            }
        }
    }

    class PositiveComparableValue(
        override val value: Any?,
        override val converted: Double
    ) : FloatComparableValue {
        override fun compareTo(other: ComparableValue): Int {
            return when (other) {
                is FloatComparableValue -> {
                    converted.compareTo(other.converted)
                }
                is BigComparableValue -> {
                    if (converted == Double.POSITIVE_INFINITY) {
                        1
                    } else {
                        val cmp = BigInteger
                            .tryFromDouble(converted, exactRequired = false)
                            .compareTo(other.converted)
                        if (cmp == 0) {
                            1
                        } else {
                            cmp
                        }
                    }
                }

                else -> error(
                    "compareTo undefined for operands of type " +
                            "'${typeName(value)}' and '${typeName(other)}'"
                )
            }
        }
    }
}
