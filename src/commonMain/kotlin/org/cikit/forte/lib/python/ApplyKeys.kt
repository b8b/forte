package org.cikit.forte.lib.python

import org.cikit.forte.core.Method
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.typeName

/**
 * keys
 * Returns a new list of the dictionary's keys.
 * Python 3.13
 *
 * dict.keys()
 */
class ApplyKeys : Method {
    override fun invoke(subject: Any?, args: NamedArgs): Any {
        args.requireEmpty()
        return when (subject) {
            is Map<*, *> -> subject.keys

            else -> throw IllegalArgumentException(
                "invalid operand of type '${typeName(subject)}'"
            )
        }
    }
}
