package org.cikit.forte.lib.python

import org.cikit.forte.core.Context
import org.cikit.forte.core.DependencyAware
import org.cikit.forte.core.Method
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.require
import org.cikit.forte.core.typeName
import org.cikit.forte.lib.core.FilterNumber
import org.cikit.forte.lib.core.filterNumber

/**
 * Python 3.13
 *
 * str.startswith(prefix[, start[, end]])
 *
 * `prefix`
 * The prefix to check for.
 *
 * `start`
 * The starting position for the check.
 *
 * `end`
 * The ending position for the check.
 *
 * empirical testing with python-3.12.3:
 * * `end` seems nonInclusive
 * * `length` seems to be added to negative indices
 */
class ApplyStartsWith private constructor(
    private val number: FilterNumber
) : Method, DependencyAware {
    constructor(ctx: Context<*>) : this(ctx.filterNumber)

    override fun withDependencies(ctx: Context<*>): DependencyAware {
        val number = ctx.filterNumber
        if (number === this.number) {
            return this
        }
        return ApplyStartsWith(number)
    }

    override fun invoke(subject: Any?, args: NamedArgs): Any {
        val input = when (subject) {
            is CharSequence -> subject
            is Char -> subject.toString()

            else -> throw IllegalArgumentException(
                "invalid operand of type '${typeName(subject)}'"
            )
        }
        val prefix: CharSequence
        var start: Int
        var end: Int
        args.use {
            prefix = require("prefix")
            start = optional("start", number::requireInt) { 0 }
            end = optional("end", number::requireInt) { input.length }
        }
        if (start >= input.length) {
            return false
        }
        if (start < 0) {
            start += input.length
        }
        if (start < 0) {
            start = 0
        }
        if (end < 0) {
            end += input.length
        }
        if (end < 0) {
            return false
        }
        if (end > input.length) {
            end = input.length
        }
        return input.substring(start, end).startsWith(prefix)
    }
}
