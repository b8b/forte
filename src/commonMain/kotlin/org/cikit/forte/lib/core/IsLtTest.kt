package org.cikit.forte.lib.core

import org.cikit.forte.core.Context
import org.cikit.forte.core.DependencyAware
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.TestMethod

class IsLtTest private constructor(
    private val comparable: FilterComparable
) : TestMethod, DependencyAware {

    constructor(ctx: Context<*>) : this(ctx.filterComparable)

    override fun withDependencies(ctx: Context<*>): IsLtTest {
        val comparable = ctx.filterComparable
        return if (comparable === this.comparable) {
            this
        } else {
            IsLtTest(comparable)
        }
    }

    override fun invoke(subject: Any?, args: NamedArgs): Boolean {
        val other: Any?
        args.use {
            other = requireAny("other")
        }
        return comparable(subject) < comparable(other)
    }
}
