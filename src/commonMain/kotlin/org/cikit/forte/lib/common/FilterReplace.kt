package org.cikit.forte.lib.common

import org.cikit.forte.core.*
import org.cikit.forte.lib.core.FilterNumber
import org.cikit.forte.lib.core.filterNumber

/**
 * jinja-filters.replace(s: str, old: str, new: str, count: int | None = None) → str
 *
 *     Return a copy of the value with all occurrences of a substring replaced with a new one.
 *     The first argument is the substring that should be replaced, the second is the replacement string.
 *     If the optional third argument count is given, only the first count occurrences are replaced:
 *
 *     {{ "Hello World"|replace("Hello", "Goodbye") }}
 *         -> Goodbye World
 *
 *     {{ "aaaaargh"|replace("a", "d'oh, ", 2) }}
 *         -> d'oh, d'oh, aaargh
 */
class FilterReplace private constructor(
    private val number: FilterNumber
) : FilterMethod, DependencyAware {
    constructor(ctx: Context<*>): this(ctx.filterNumber)

    override fun withDependencies(ctx: Context<*>): DependencyAware {
        val number = ctx.filterNumber
        if (number === this.number) {
            return this
        }
        return FilterReplace(number)
    }

    override fun invoke(subject: Any?, args: NamedArgs): Any {
        val search: String
        val replacement: CharSequence
        val count: Int
        args.use {
            search = require<CharSequence>("old").concatToString()
            replacement = require("new")
            count = optional(
                "count",
                convertValue = { v ->
                    number(v).toIntOrNull() ?: error(
                        "cannot convert arg 'count' to int"
                    )
                },
                defaultValue =  { -1 }
            )
        }
        require(subject is CharSequence) {
            "invalid operand of type'${typeName(subject)}'"
        }
        return replace(subject, search, replacement, count)
    }

    fun replace(
        subject: CharSequence,
        search: String,
        replacement: CharSequence,
        count: Int
    ): CharSequence {
        if (count == 0) {
            return subject
        }
        var remaining = count
        var i = subject.indexOf(search)
        if (i < 0) {
            return subject
        }
        val target = StringBuilder()
        target.append(subject.take(i))
        target.append(replacement)
        i += search.length
        remaining--
        while (i < subject.length) {
            if (count > 0 && remaining == 0) {
                target.append(subject.subSequence(i, subject.length))
                break
            }
            val j = subject.indexOf(search, startIndex = i)
            if (j < 0) {
                target.append(subject.subSequence(i, subject.length))
                break
            }
            target.append(subject.subSequence(i, j))
            target.append(replacement)
            i = j + search.length
            remaining--
        }
        return target.toString()
    }
}
