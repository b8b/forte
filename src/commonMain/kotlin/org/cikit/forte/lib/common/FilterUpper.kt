package org.cikit.forte.lib.common

import org.cikit.forte.core.FilterMethod
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.concatToString
import org.cikit.forte.core.typeName

/**
 * jinja-filters.upper(s: str) → str
 *
 *     Convert a value to uppercase.
 */
class FilterUpper : FilterMethod {
    override fun invoke(subject: Any?, args: NamedArgs): Any {
        args.requireEmpty()
        return when (subject) {
            is CharSequence -> subject.concatToString().uppercase()
            is Char -> subject.uppercase()

            else -> throw IllegalArgumentException(
                "invalid operand of type '${typeName(subject)}'"
            )
        }
    }
}
