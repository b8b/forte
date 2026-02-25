package org.cikit.forte.lib.core

import org.cikit.forte.core.BinOpFunction
import org.cikit.forte.core.Context
import org.cikit.forte.core.DependencyAware

class BinaryLt private constructor(
    private val comparable: FilterComparable
) : BinOpFunction, DependencyAware {

    constructor(ctx: Context<*>) : this(ctx.filterComparable)

    override fun withDependencies(ctx: Context<*>): BinaryLt {
        val comparable = ctx.filterComparable
        return if (comparable === this.comparable) {
            this
        } else {
            BinaryLt(comparable)
        }
    }

    override fun invoke(left: Any?, right: Any?): Boolean {
        val leftComparable = comparable.test(left) ?: return false
        val rightComparable = comparable.test(right) ?: return false
        return leftComparable < rightComparable
    }
}
