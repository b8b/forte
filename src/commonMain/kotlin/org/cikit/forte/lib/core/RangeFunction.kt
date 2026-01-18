package org.cikit.forte.lib.core

import org.cikit.forte.core.BinOpFunction
import org.cikit.forte.core.Function
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.require

class RangeFunction : Function, BinOpFunction {
    override val isHidden: Boolean
        get() = false

    override fun invoke(args: NamedArgs): Any {
        val start: Number
        val end: Number
        if (args.values.size == 1) {
            args.use {
                start = 0
                end = require("end_inclusive")
            }
        } else {
            args.use {
                start = require("start")
                end = require("end_inclusive")
            }
        }
        return range(start, end)
    }

    override fun invoke(left: Any?, right: Any?): Any {
        return range(left, right)
    }

    private fun range(subject: Any?, other: Any?): Any = when (subject) {
        is Int -> when (other) {
            is Long -> (subject .. other).toList()
            is Number -> (subject..other.toInt()).toList()
            else -> binOpTypeError("range", subject, other)
        }

        is Long -> when (other) {
            is Number -> (subject..other.toLong()).toList()
            else -> binOpTypeError("range", subject, other)
        }

        is Number -> when (other) {
            is Long -> (subject.toLong()..other).toList()
            is Number -> (subject.toInt()..other.toInt()).toList()
            else -> binOpTypeError("range", subject, other)
        }

        else -> binOpTypeError("range", subject, other)
    }
}
