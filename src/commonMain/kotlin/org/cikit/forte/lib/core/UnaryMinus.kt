package org.cikit.forte.lib.core

import org.cikit.forte.core.Context
import org.cikit.forte.core.DependencyAware
import org.cikit.forte.core.NumericValue
import org.cikit.forte.core.UnOpFunction

class UnaryMinus private constructor(
    private val number: FilterNumber,
    private val factor: NumericValue
) : UnOpFunction, DependencyAware {

    constructor(ctx: Context<*>) : this(
        ctx.filterNumber,
        ctx.filterNumber(-1)
    )

    override fun withDependencies(ctx: Context<*>): DependencyAware {
        val number = ctx.filterNumber
        if (number === this.number) {
            return this
        }
        return UnaryMinus(number, number(-1))
    }

    override fun invoke(arg: Any?): Any? {
        val num = number(arg)
        return num.mul(factor).result
    }
}
