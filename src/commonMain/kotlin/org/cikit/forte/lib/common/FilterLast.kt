package org.cikit.forte.lib.common

import org.cikit.forte.core.FilterMethod
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.Undefined
import org.cikit.forte.core.typeName

/**
 * jinja-filters.last(seq: 't.Reversible[V]') → 't.Union[V, Undefined]'
 *
 *     Return the last item of a sequence.
 *
 *     Note: Does not work with generators. You may want to explicitly convert it to a list:
 *
 *     {{ data | selectattr('name', '==', 'Jinja') | list | last }}
 */
class FilterLast : FilterMethod {
    override fun invoke(subject: Any?, args: NamedArgs): Any {
        args.requireEmpty()
        return when (subject) {
            is CharSequence -> subject.lastOrNull()
            is List<*> -> subject.lastOrNull()
            is Char -> subject

            else -> throw IllegalArgumentException(
                "invalid operand of type '${typeName(subject)}'"
            )
        } ?: Undefined("sequence is empty")
    }
}
