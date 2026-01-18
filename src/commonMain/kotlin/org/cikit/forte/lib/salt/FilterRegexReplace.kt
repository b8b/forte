package org.cikit.forte.lib.salt

import org.cikit.forte.core.*

class FilterRegexReplace : FilterMethod {
    override fun invoke(subject: Any?, args: NamedArgs): Any {
        val pattern: CharSequence
        val replacement: CharSequence
        val ignoreCase: Boolean
        args.use {
            pattern = require("pattern")
            replacement = require("replacement")
            ignoreCase = optional("ignore_case") { true }
        }
        val re = if (ignoreCase) {
            Regex(pattern.concatToString(), RegexOption.IGNORE_CASE)
        } else {
            Regex(pattern.concatToString())
        }
        return when (subject) {
            is CharSequence -> subject.replace(
                re,
                replacement.concatToString()
            )

            else -> throw IllegalArgumentException(
                "invalid operand of type '${typeName(subject)}'"
            )
        }
    }
}
