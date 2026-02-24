package org.cikit.forte.lib.common

import org.cikit.forte.core.*
import org.cikit.forte.emitter.JsonEmitter
import org.cikit.forte.lib.core.FilterNumber
import org.cikit.forte.lib.core.filterNumber

/**
 * jinja-filters.tojson(value: Any, indent: int | None = None) → markupsafe.Markup
 *
 *     Serialize an object to a string of JSON.
 *
 *     Parameters:
 *             value – The object to serialize to JSON.
 *             indent – The indent parameter passed to dumps, for pretty-printing the value.
 */
class FilterToJson private constructor(
    private val number: FilterNumber
): FilterMethod, DependencyAware {
    constructor(ctx: Context<*>) : this(ctx.filterNumber)

    override fun withDependencies(ctx: Context<*>): DependencyAware {
        val number = ctx.filterNumber
        if (number === this.number) {
            return this
        }
        return FilterToJson(number)
    }

    override fun invoke(subject: Any?, args: NamedArgs): Any {
        val indent: Int
        args.use {
            indent = optional("indent", number::requireInt) { -1 }
        }
        val target = StringBuilder()
        val emitter = JsonEmitter(target, indent)
        return Suspended { ctx ->
            emitter.suspendingEmit(ctx, subject)
            emitter.close()
            target.toString()
        }
    }
}
