package org.cikit.forte.lib.core

import org.cikit.forte.core.Context
import org.cikit.forte.core.DependencyAware
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.TestMethod

class IsEqTest private constructor(
    private val comparable: FilterComparable
) : TestMethod, DependencyAware {

    constructor(ctx: Context<*>) : this(ctx.filterComparable)

    override fun withDependencies(ctx: Context<*>): IsEqTest {
        val comparable = ctx.filterComparable
        return if (comparable === this.comparable) {
            this
        } else {
            IsEqTest(comparable)
        }
    }

    override fun invoke(subject: Any?, args: NamedArgs): Boolean {
        val other: Any?
        args.use {
            other = requireAny("other")
        }
        return if (subject == other) {
            true
        } else {
            val left = comparable.test(subject)
            val right = comparable.test(other)
            left != null && right != null && left.compareTo(right) == 0
        }
    }
}
