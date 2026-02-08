package org.cikit.forte.lib.common

import org.cikit.forte.core.FilterMethod
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.Undefined
import org.cikit.forte.core.typeName

/**
 * jinja-filters.first(seq: 't.Iterable[V]') → 't.Union[V, Undefined]'
 *
 *     Return the first item of a sequence.
 */
class FilterFirst : FilterMethod {
    override fun invoke(subject: Any?, args: NamedArgs): Any {
        args.requireEmpty()
        return when (subject) {
            is CharSequence -> subject.firstOrNull()
            is Iterable<*> -> subject.firstOrNull()
            is Char -> subject

            else -> throw IllegalArgumentException(
                "invalid operand of type '${typeName(subject)}'"
            )
        } ?: Undefined("sequence is empty")
    }
}
