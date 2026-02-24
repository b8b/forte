package org.cikit.forte.lib.core

import org.cikit.forte.core.*

class FilterFloat private constructor(
    private val number: FilterNumber
): FilterMethod, DependencyAware {

    constructor(ctx: Context<*>) : this(ctx.filterNumber)

    override fun withDependencies(ctx: Context<*>): DependencyAware {
        val number = ctx.filterNumber
        return if (number === this.number) {
            this
        } else {
            FilterFloat(number)
        }
    }

    override fun invoke(subject: Any?, args: NamedArgs): NumericValue {
        args.requireEmpty()
        return when (subject) {
            is NumericValue -> subject.toFloatValue()
            is Number -> number.requireNumber(subject).toFloatValue()
            is Boolean -> number(if (subject) 1 else 0).toFloatValue()
            is CharSequence -> number(subject).toFloatValue()
            is Char -> number(subject.digitToInt()).toFloatValue()
            else -> throw IllegalArgumentException(
                "invalid operand of type '${typeName(subject)}'"
            )
        }
    }
}
