package org.cikit.forte.lib.core

import org.cikit.forte.core.BinOpFunction
import org.cikit.forte.core.Context
import org.cikit.forte.core.DependencyAware

class BinaryPlus private constructor(
    private val number: FilterNumber
): BinOpFunction, DependencyAware {

    constructor(ctx: Context<*>) : this(ctx.filterNumber)

    override fun withDependencies(ctx: Context<*>): DependencyAware {
        val number = ctx.filterNumber
        return if (number === this.number) {
            this
        } else {
            BinaryPlus(number)
        }
    }

    override fun invoke(left: Any?, right: Any?): Any? {
        return number(left).plus(number(right)).value
    }
}

