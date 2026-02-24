package org.cikit.forte.lib.core

import org.cikit.forte.core.Function
import org.cikit.forte.core.*

class RangeFunction(
    private val number: FilterNumber
) : Function, BinOpFunction, DependencyAware {

    constructor(ctx: Context<*>) : this(ctx.filterNumber)

    override fun withDependencies(ctx: Context<*>): DependencyAware {
        val number = ctx.filterNumber
        if (number === this.number) {
            return this
        }
        return RangeFunction(number)
    }

    override val isHidden: Boolean
        get() = false

    override fun invoke(args: NamedArgs): Iterable<Int> {
        val start: Int
        val end: Int
        val step: Int
        if (args.values.size == 1) {
            args.use {
                start = 0
                end = require("stop", number::requireInt)
                step = 1
            }
        } else {
            args.use {
                start = require("start", number::requireInt)
                end = require("stop", number::requireInt)
                step = optional("step", number::requireInt) { 1 }
            }
        }
        return if (step == 1) {
            range(start, end)
        } else {
            range(start, end, step)
        }
    }

    override fun invoke(left: Any?, right: Any?): Iterable<Int> {
        if (left !is Int || right !is Int) {
            binOpTypeError("range", left, right)
        }
        return range(left, right)
    }

    private fun range(start: Int, stop: Int) = start until stop

    private fun range(
        start: Int,
        stop: Int,
        step: Int
    ): Iterable<Int> {
        if (step > 0) {
            if (start >= stop) {
                return IntRange.EMPTY
            }
            return start until stop step step
        }
        if (step < 0) {
            if (start <= stop) {
                return IntRange.EMPTY
            }
            return start downTo (stop + 1) step (- step)
        }
        error("range step cannot be zero")
    }
}
