package org.cikit.forte.lib.core

import org.cikit.forte.core.BinOpFunction
import org.cikit.forte.core.Context
import org.cikit.forte.core.DependencyAware

class BinaryIn private constructor(
    private val isIn: IsInTest
) : BinOpFunction, DependencyAware {

    constructor(ctx: Context<*>) : this(ctx.testIn)

    override fun withDependencies(ctx: Context<*>): BinaryIn {
        val isIn = ctx.testIn
        return if (isIn === this.isIn) {
            this
        } else {
            BinaryIn(isIn)
        }
    }

    override fun invoke(left: Any?, right: Any?): Any {
        return isIn.test(left, right)
    }
}
