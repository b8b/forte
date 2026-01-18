package org.cikit.forte.lib.jinja

import org.cikit.forte.core.*
import org.cikit.forte.lib.core.FilterNumber
import org.cikit.forte.lib.core.filterNumber

class IsEvenTest private constructor(
    private val number: FilterNumber,
    private val two: NumericValue = number(2)
) : TestMethod, DependencyAware {

    constructor(ctx: Context<*>) : this(ctx.filterNumber)

    override fun withDependencies(ctx: Context<*>): IsEvenTest {
        val number = ctx.filterNumber
        return if (number === this.number) {
            this
        } else {
            IsEvenTest(number)
        }
    }

    override fun invoke(subject: Any?, args: NamedArgs): Boolean {
        args.requireEmpty()
        //FIXME have to find out what jinja actually does
        return number(subject).div(two).isFloat
    }
}
