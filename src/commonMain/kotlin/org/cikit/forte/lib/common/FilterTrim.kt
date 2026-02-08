package org.cikit.forte.lib.common

import org.cikit.forte.core.FilterMethod
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.optional
import org.cikit.forte.core.typeName

/**
 * jinja-filters.trim(value: str, chars: str | None = None) → str
 *
 *     Strip leading and trailing characters, by default whitespace.
 */
class FilterTrim : FilterMethod {
    override fun invoke(subject: Any?, args: NamedArgs): Any {
        val chars: CharSequence
        args.use {
            chars = optional("chars") { "" }
        }
        return when (subject) {
            is CharSequence -> if (chars.isEmpty()) {
                subject.trim()
            } else {
                subject.trim(*CharArray(chars.length) { i -> chars[i] })
            }

            else -> throw IllegalArgumentException(
                "invalid operand of type '${typeName(subject)}'"
            )
        }
    }
}
