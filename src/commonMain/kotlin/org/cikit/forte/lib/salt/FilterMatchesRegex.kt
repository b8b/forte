package org.cikit.forte.lib.salt

import org.cikit.forte.core.*

class FilterMatchesRegex : FilterMethod {
    override fun invoke(subject: Any?, args: NamedArgs): Any {
        val pattern: CharSequence
        val ignoreCase: Boolean
        args.use {
            pattern = require("pattern")
            ignoreCase = optional("ignore_case") { true }
        }
        val re = if (ignoreCase) {
            Regex(pattern.concatToString(), RegexOption.IGNORE_CASE)
        } else {
            Regex(pattern.concatToString())
        }
        return when (subject) {
            is CharSequence -> re.matches(subject)

            else -> throw IllegalArgumentException(
                "invalid operand of type '${typeName(subject)}'"
            )
        }
    }
}
