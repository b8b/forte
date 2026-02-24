package org.cikit.forte.lib.core

import kotlinx.io.bytestring.ByteString
import org.cikit.forte.core.Context
import org.cikit.forte.core.DependencyAware
import org.cikit.forte.core.FilterMethod
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.typeName

class FilterLength private constructor(
    private val number: FilterNumber
) : FilterMethod, DependencyAware {
    constructor(ctx: Context<*>) : this(ctx.filterNumber)

    override fun withDependencies(ctx: Context<*>): DependencyAware {
        val number = ctx.filterNumber
        if (number === this.number) {
            return this
        }
        return FilterLength(number)
    }

    override fun invoke(subject: Any?, args: NamedArgs): Number {
        args.requireEmpty()
        val result = when (subject) {
            is Map<*, *> -> subject.size
            is Collection<*> -> subject.size
            is CharSequence -> subject.length
            is ByteString -> subject.size
            else -> throw IllegalArgumentException(
                "invalid operand of type '${typeName(subject)}'"
            )
        }
        return result
    }
}
