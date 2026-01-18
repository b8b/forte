package org.cikit.forte.lib.common

import org.cikit.forte.core.FilterMethod
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.typeName

class FilterLast : FilterMethod {
    override fun invoke(subject: Any?, args: NamedArgs): Any? {
        args.requireEmpty()
        return when (subject) {
            is CharSequence -> subject.lastOrNull()
            is Iterable<*> -> subject.lastOrNull()
            else -> throw IllegalArgumentException(
                "invalid operand of type '${typeName(subject)}'"
            )
        }
    }
}
