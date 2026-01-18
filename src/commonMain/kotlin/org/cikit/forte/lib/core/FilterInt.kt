package org.cikit.forte.lib.core

import org.cikit.forte.core.*

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

    override fun invoke(subject: Any?, args: NamedArgs): Any {
        args.requireEmpty()
        return when (subject) {
            is Number -> number(subject).toIntValue()
            is Boolean -> if (subject) 1 else 0
            is CharSequence -> subject.concatToString().toLong()
            is Char -> subject.digitToInt()
            else -> throw IllegalArgumentException(
                "invalid operand of type '${typeName(subject)}'"
            )
        }
    }
}
