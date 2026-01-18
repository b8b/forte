package org.cikit.forte.lib.core

import org.cikit.forte.core.BinOpFunction
import org.cikit.forte.core.Context
import org.cikit.forte.core.DependencyAware

class BinaryGe private constructor(
    private val comparable: FilterComparable
) : BinOpFunction, DependencyAware {

    constructor(ctx: Context<*>) : this(ctx.filterComparable)

    override fun withDependencies(ctx: Context<*>): BinaryGe {
        val comparable = ctx.filterComparable
        return if (comparable === this.comparable) {
            this
        } else {
            BinaryGe(comparable)
        }
    }

    override fun invoke(left: Any?, right: Any?): Any? {
        return comparable(left) >= comparable(right)
    }
}
