package org.cikit.forte.lib.core

import org.cikit.forte.core.*
import org.cikit.forte.internal.parseInt

class FilterInt private constructor(
    private val number: FilterNumber
): FilterMethod, DependencyAware {

    constructor(ctx: Context<*>) : this(ctx.filterNumber)

    override fun withDependencies(ctx: Context<*>): DependencyAware {
        val number = ctx.filterNumber
        return if (number === this.number) {
            this
        } else {
            FilterInt(number)
        }
    }

    override fun invoke(subject: Any?, args: NamedArgs): NumericValue {
        args.requireEmpty()
        return when (subject) {
            is NumericValue -> subject.toIntValue()
            is Number -> number.requireNumber(subject).toIntValue()
            is Boolean -> number(if (subject) 1 else 0).toIntValue()
            is CharSequence -> number(subject).toIntValue()
            is Char -> number(subject.digitToInt()).toIntValue()
            else -> throw IllegalArgumentException(
                "invalid operand of type '${typeName(subject)}'"
            )
        }
    }
}
