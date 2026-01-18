package org.cikit.forte.lib.core

import org.cikit.forte.core.*

class IsNumberTest private constructor(
    private val number: FilterNumber
): TestMethod, DependencyAware {

    constructor(ctx: Context<*>) : this(ctx.filterNumber)

    override fun withDependencies(ctx: Context<*>): DependencyAware {
        val number = ctx.filterNumber
        return if (number === this.number) {
            this
        } else {
            IsNumberTest(number)
        }
    }

    override fun invoke(subject: Any?, args: NamedArgs): Boolean {
        args.requireEmpty()
        return when (subject) {
            is NumericValue -> true
            is Number -> true
            is Long -> true
            null -> false
            else -> subject::class in number.types
        }
    }
}
