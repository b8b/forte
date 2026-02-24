package org.cikit.forte.lib.core

import org.cikit.forte.core.Context
import org.cikit.forte.core.DependencyAware
import org.cikit.forte.core.UnOpFunction

class UnaryMinus private constructor(
    private val number: FilterNumber
) : UnOpFunction, DependencyAware {

    constructor(ctx: Context<*>) : this(ctx.filterNumber)

    override fun withDependencies(ctx: Context<*>): DependencyAware {
        val number = ctx.filterNumber
        if (number === this.number) {
            return this
        }
        return UnaryMinus(number)
    }

    override fun invoke(arg: Any?): Any {
        return number.requireNumber(arg).negate()
    }
}
