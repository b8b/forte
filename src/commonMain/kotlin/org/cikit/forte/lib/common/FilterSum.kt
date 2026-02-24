package org.cikit.forte.lib.common

import org.cikit.forte.core.Context
import org.cikit.forte.core.DependencyAware
import org.cikit.forte.core.FilterMethod
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.NumericValue
import org.cikit.forte.core.Suspended
import org.cikit.forte.core.Undefined
import org.cikit.forte.core.typeName
import org.cikit.forte.lib.core.FilterGet
import org.cikit.forte.lib.core.FilterNumber
import org.cikit.forte.lib.core.filterNumber

/**
 * jinja-filters.sum(iterable: 't.Iterable[V]', attribute: str | int | NoneType = None, start: V = 0) → V
 *
 *     Returns the sum of a sequence of numbers plus the value of parameter ‘start’ (which defaults to 0).
 *     When the sequence is empty it returns start.
 *
 *     It is also possible to sum up only certain attributes:
 *
 *     Total: {{ items|sum(attribute='price') }}
 */
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
        val getArgs: NamedArgs?
        var sum: NumericValue
        args.use {
            getArgs = optionalNullable(
                "attribute",
                { attribute ->
                    NamedArgs(listOf(attribute), FilterGet.singleArg)
                },
                { null }
            )
            sum = optional("start", number::requireNumber) { number(0) }
        }
        require(subject is Iterable<*>) {
            "invalid operand of type '${typeName(subject)}'"
        }
        if (getArgs == null) {
            for (item in subject) {
                sum = sum.plus(number.requireNumber(item))
            }
            return sum
        }
        return Suspended { ctx ->
            for (item in subject) {
                var value = ctx.filterGet(item, getArgs)
                if (value is Suspended) {
                    value = value.eval(ctx)
                }
                if (value is Undefined) {
                    error(value.message)
                }
                sum = sum.plus(number.requireNumber(value))
            }
            sum
        }
    }
}
