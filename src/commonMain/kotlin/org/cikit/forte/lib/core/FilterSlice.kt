package org.cikit.forte.lib.core

import org.cikit.forte.core.Context
import org.cikit.forte.core.FilterMethod
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.optional
import org.cikit.forte.core.typeName

class FilterSlice : FilterMethod {

    companion object {
        val KEY = Context.Key.Apply.create("slice", FilterMethod.OPERATOR)
    }

    override val isHidden: Boolean
        get() = true

    override fun invoke(subject: Any?, args: NamedArgs): Any {
        return when (subject) {
            is CharSequence -> sliceString(subject, args)
            is List<*> -> sliceList(subject, args)
            is Char -> sliceString(subject.toString(), args)

            else -> throw IllegalArgumentException(
                "invalid operand of type '${typeName(subject)}'"
            )
        }
    }

    private fun sliceString(input: CharSequence, args: NamedArgs): String {
        val r = range(input.length, args)
        if (r.isEmpty()) {
            return ""
        }
        if (r.step == 1) {
            return input.substring(r.first, r.last + 1)
        }
        return buildString {
            for (i in r) {
                append(input[i])
            }
        }
    }

    private fun sliceList(input: List<*>, args: NamedArgs): List<Any?> {
        val r = range(input.size, args)
        if (r.isEmpty()) {
            return emptyList()
        }
        if (r.step == 1) {
            return input.subList(r.first, r.last + 1)
        }
        return buildList {
            for (i in r) {
                add(input[i])
            }
        }
    }

    private fun range(size: Int, args: NamedArgs): IntProgression {
        var start: Int
        var defaultStart = false
        var end: Int
        var defaultEnd = false
        val step: Int
        args.use {
            start = optional("start") { defaultStart = true; 0 }
            end = optional("end") { defaultEnd = true; size }
            step = optional("step") { 1 }
        }
        if (step > 0) {
            if (size <= 0 || start >= size) {
                return IntRange.EMPTY
            }
            if (start < 0) {
                start += size
                if (start < 0) {
                    start = 0
                }
            }
            if (end < 0) {
                end += size
            }
            if (end > size) {
                end = size
            }
            if (start >= end) {
                return IntRange.EMPTY
            }
            return start until end step step
        }
        if (step < 0) {
            if (size <= 0) {
                return IntRange.EMPTY
            }
            if (defaultStart || start >= size) {
                start = size - 1
            } else {
                if (start < 0) {
                    start += size
                    if (start < 0) {
                        return IntRange.EMPTY
                    }
                }
            }
            // calculate endInclusive
            if (defaultEnd) {
                end = 0
            } else {
                end += if (end < 0) {
                    size + 1
                } else {
                    1
                }
                if (end >= size) {
                    return IntRange.EMPTY
                }
                if (end < 0) {
                    end = 0
                }
            }
            if (start < end) {
                return IntRange.EMPTY
            }
            return start downTo end step (- step)
        }
        error("slice step cannot be zero")
    }

}