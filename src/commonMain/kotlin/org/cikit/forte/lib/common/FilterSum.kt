package org.cikit.forte.lib.common

import org.cikit.forte.core.Context
import org.cikit.forte.core.DependencyAware
import org.cikit.forte.core.FilterMethod
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.typeName
import org.cikit.forte.lib.core.FilterNumber
import org.cikit.forte.lib.core.filterNumber

class FilterSum private constructor(
    private val number: FilterNumber
) : FilterMethod, DependencyAware {

    constructor(ctx: Context<*>): this(ctx.filterNumber)

    override fun withDependencies(ctx: Context<*>): FilterSum {
        val number = ctx.filterNumber
        return if (number === this.number) {
            this
        } else {
            FilterSum(number)
        }
    }

    override fun invoke(subject: Any?, args: NamedArgs): Any? {
        args.requireEmpty()
        require(subject is Iterable<*>) {
            "invalid operand of type '${typeName(subject)}'"
        }
        var sum = number(0)
        for (item in subject) {
            sum = sum.plus(number(item))
        }
        return sum.value
    }
}
