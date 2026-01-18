package org.cikit.forte.lib.common

import org.cikit.forte.core.*

class FilterReplace : FilterMethod {
    override fun invoke(subject: Any?, args: NamedArgs): Any {
        val search: String
        val replacement: CharSequence
        val count: Int
        args.use {
            search = require<CharSequence>("old").concatToString()
            replacement = require("new")
            count = optional("count") { -1 }
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
