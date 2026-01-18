package org.cikit.forte.lib.jinja

import org.cikit.forte.core.*

class FilterAttr(
    private val get: FilterMethod
) : FilterMethod, DependencyAware {

    constructor(ctx: Context<*>) : this(ctx.filterGet)

    override fun withDependencies(ctx: Context<*>): FilterAttr {
        val get = ctx.filterGet
        return if (get === this.get) {
            this
        } else {
            FilterAttr(get)
        }
    }

    override fun invoke(subject: Any?, args: NamedArgs): Any? {
        if (args.values.firstOrNull() !is CharSequence) {
            args.use {
                require<String>("name")
            }
        }
        return get(subject, args)
    }
}
