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

    override fun invoke(subject: Any?, args: NamedArgs): Any {
        args.requireEmpty()
        return when (subject) {
            is Number -> number(subject).toFloatValue()
            is Boolean -> if (subject) 1.0 else 0.0
            is CharSequence -> subject.concatToString().toDouble()
            is Char -> subject.digitToInt().toDouble()
            else -> throw IllegalArgumentException(
                "invalid operand of type '${typeName(subject)}'"
            )
        }
    }
}
