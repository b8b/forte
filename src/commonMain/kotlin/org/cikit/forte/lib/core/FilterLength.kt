package org.cikit.forte.lib.core

import kotlinx.io.bytestring.ByteString
import org.cikit.forte.core.FilterMethod
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.typeName

class FilterLength : FilterMethod {
    override fun invoke(subject: Any?, args: NamedArgs): Int {
        args.requireEmpty()
        return when (subject) {
            is Map<*, *> -> subject.size
            is Collection<*> -> subject.size
            is CharSequence -> subject.length
            is ByteString -> subject.size
            else -> throw IllegalArgumentException(
                "invalid operand of type '${typeName(subject)}'"
            )
        }
    }
}
