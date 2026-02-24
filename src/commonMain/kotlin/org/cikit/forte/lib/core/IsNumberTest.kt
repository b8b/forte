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
            is NumericValue -> !subject.isFloat ||
                    subject.doubleOrNull()?.isNaN() != true
            is Number -> when (subject) {
                is Double -> !subject.isNaN()
                is Float -> !subject.isNaN()

                else -> true
            }
            is Long -> true
            null -> false
            else -> subject::class in number.types
        }
    }
}
