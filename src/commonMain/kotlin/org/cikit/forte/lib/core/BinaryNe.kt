package org.cikit.forte.lib.core

import org.cikit.forte.core.BinOpFunction
import org.cikit.forte.core.Context
import org.cikit.forte.core.DependencyAware

class BinaryNe private constructor(
    private val comparable: FilterComparable
) : BinOpFunction, DependencyAware {

    constructor(ctx: Context<*>) : this(ctx.filterComparable)

    override fun withDependencies(ctx: Context<*>): BinaryNe {
        val comparable = ctx.filterComparable
        return if (comparable === this.comparable) {
            this
        } else {
            BinaryNe(comparable)
        }
    }

    override fun invoke(left: Any?, right: Any?): Boolean {
        return if (left == right) {
            false
        } else {
            val leftComparable = comparable.test(left)
            val rightComparable = comparable.test(right)
            leftComparable == null ||
                    rightComparable == null ||
                    leftComparable.compareTo(rightComparable) != 0
        }
    }
}
