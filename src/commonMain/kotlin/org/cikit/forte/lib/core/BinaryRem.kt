package org.cikit.forte.lib.core

import org.cikit.forte.core.BinOpFunction
import org.cikit.forte.core.Context
import org.cikit.forte.core.DependencyAware

class BinaryRem private constructor(
    private val number: FilterNumber
): BinOpFunction, DependencyAware {

    constructor(ctx: Context<*>) : this(ctx.filterNumber)

    override fun withDependencies(ctx: Context<*>): DependencyAware {
        val number = ctx.filterNumber
        return if (number === this.number) {
            this
        } else {
            BinaryRem(number)
        }
    }

    override fun invoke(left: Any?, right: Any?): Any? {
        return number(left).rem(number(right)).result
    }
}
