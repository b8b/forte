package org.cikit.forte.lib.core

import org.cikit.forte.core.BinOpFunction
import org.cikit.forte.core.Context
import org.cikit.forte.core.DependencyAware

class BinaryEq private constructor(
    private val comparable: FilterComparable
) : BinOpFunction, DependencyAware {

    constructor(ctx: Context<*>) : this(ctx.filterComparable)

    override fun withDependencies(ctx: Context<*>): BinaryEq {
        val comparable = ctx.filterComparable
        return if (comparable === this.comparable) {
            this
        } else {
            BinaryEq(comparable)
        }
    }

    override fun invoke(left: Any?, right: Any?): Boolean {
        return if (left == right) {
            true
        } else {
            val leftComparable = comparable.test(left)
            val rightComparable = comparable.test(right)
            leftComparable != null && rightComparable != null &&
                    leftComparable.compareTo(rightComparable) == 0
        }

    }
}
