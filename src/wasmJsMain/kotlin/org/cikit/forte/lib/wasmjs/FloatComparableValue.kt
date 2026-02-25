package org.cikit.forte.lib.wasmjs

import org.cikit.forte.core.ComparableValue
import org.cikit.forte.core.typeName
import kotlin.math.truncate

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
                    bigIntCompare(BigInt(truncate(converted)), other.converted)
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
                        val cmp = bigIntCompare(
                            BigInt(truncate(converted)),
                            other.converted
                        )
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
                        val cmp = bigIntCompare(
                            BigInt(truncate(converted)),
                            other.converted
                        )
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
