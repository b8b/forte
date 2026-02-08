package org.cikit.forte.lib.common

import org.cikit.forte.core.FilterMethod
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.concatToString
import org.cikit.forte.core.typeName

/**
 * jinja-filters.lower(s: str) → str
 *
 *     Convert a value to lowercase.
 */
class FilterLower : FilterMethod {
    override fun invoke(subject: Any?, args: NamedArgs): Any {
        args.requireEmpty()
        return when (subject) {
            is CharSequence -> subject.concatToString().lowercase()
            is Char -> subject.lowercase()

            else -> throw IllegalArgumentException(
                "invalid operand of type '${typeName(subject)}'"
            )
        }
    }
}
