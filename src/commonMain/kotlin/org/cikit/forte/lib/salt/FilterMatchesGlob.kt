package org.cikit.forte.lib.salt

import org.cikit.forte.core.*

class FilterMatchesGlob : FilterMethod {
    override fun invoke(subject: Any?, args: NamedArgs): Any {
        val pattern: CharSequence
        val ignoreCase: Boolean
        args.use {
            pattern = require("pattern")
            ignoreCase = optional("ignore_case") { true }
        }
        val re = Glob(pattern, ignoreCase = ignoreCase).toRegex()
        return when (subject) {
            is CharSequence -> re.matches(subject)
            else -> throw IllegalArgumentException(
                "invalid operand of type '${typeName(subject)}'"
            )
        }
    }
}
