package org.cikit.forte.lib.js

import org.cikit.forte.core.FilterMethod
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.NumericValue
import org.cikit.forte.core.concatToString
import org.cikit.forte.core.typeName

class JsFilterString : FilterMethod {
    override fun invoke(subject: Any?, args: NamedArgs): Any {
        args.requireEmpty()
        return when (subject) {
            -0.0 -> "-0.0"
            is CharSequence -> subject.concatToString()
            is Char -> subject.toString()
            is NumericValue -> subject.toStringValue()
            is Number -> subject.toString()
            is Boolean -> if (subject) "True" else "False"
            null -> "None"
            else -> throw IllegalArgumentException(
                "invalid operand of type '${typeName(subject)}'"
            )
        }
    }
}
