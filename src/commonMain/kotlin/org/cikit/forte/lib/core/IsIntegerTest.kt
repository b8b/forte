package org.cikit.forte.lib.core

import org.cikit.forte.core.Context
import org.cikit.forte.core.DependencyAware
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.TestMethod

class IsIntegerTest private constructor(
    private val number: FilterNumber
): TestMethod, DependencyAware {

    constructor(ctx: Context<*>) : this(ctx.filterNumber)

    override fun withDependencies(ctx: Context<*>): DependencyAware {
        val number = ctx.filterNumber
        return if (number === this.number) {
            this
        } else {
            IsIntegerTest(number)
        }
    }

    override fun invoke(subject: Any?, args: NamedArgs): Boolean {
        args.requireEmpty()
        return number(subject).isInt
    }
}
