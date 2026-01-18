package org.cikit.forte.lib.jinja

import org.cikit.forte.core.Context
import org.cikit.forte.core.DependencyAware
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.TestMethod
import org.cikit.forte.lib.core.testIterable

class IsSequenceTest private constructor(
    private val iterable: TestMethod
) : TestMethod, DependencyAware {

    constructor(ctx: Context<*>) : this(ctx.testIterable)

    override fun withDependencies(ctx: Context<*>): IsSequenceTest {
        val iterable = ctx.testIterable
        return if (iterable === this.iterable) {
            this
        } else {
            IsSequenceTest(iterable)
        }
    }

    override fun invoke(subject: Any?, args: NamedArgs): Any? {
        return iterable(subject, args)
    }
}
