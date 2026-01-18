package org.cikit.forte.lib.jinja

import org.cikit.forte.core.*
import org.cikit.forte.lib.core.testDefined

class IsUndefinedTest private constructor(
    private val defined: TestMethod
) : TestMethod, DependencyAware {

    constructor(ctx: Context<*>) : this(ctx.testDefined)

    override fun withDependencies(ctx: Context<*>): IsUndefinedTest {
        val defined = ctx.testDefined
        return if (defined === this.defined) {
            this
        } else {
            IsUndefinedTest(defined)
        }
    }

    override val isRescue: Boolean
        get() = true

    override fun invoke(subject: Any?, args: NamedArgs): Any? {
        return when (val result = defined(subject, args)) {
            true -> false
            false -> true
            is Suspended -> Suspended { ctx ->
                when (val result = result.eval(ctx)) {
                    true -> false
                    false -> true
                    else -> result
                }
            }
            else -> result
        }
    }
}
