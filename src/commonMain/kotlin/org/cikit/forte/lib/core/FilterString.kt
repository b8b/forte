package org.cikit.forte.lib.core

import org.cikit.forte.core.Context
import org.cikit.forte.core.FilterMethod
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.NumericValue
import org.cikit.forte.core.concatToString
import org.cikit.forte.core.typeName

class FilterString : FilterMethod {

    companion object {
        val KEY = Context.Key.Apply.create("string", FilterMethod.OPERATOR)
    }

    override fun invoke(subject: Any?, args: NamedArgs): Any {
        args.requireEmpty()
        return when (subject) {
            is CharSequence -> subject.concatToString()
            is Char -> subject.toString()
            is NumericValue -> subject.toStringValue()
            is Number -> /* FIXME avoid toString() */ subject.toString()
            is Boolean -> if (subject) "true" else "false"
            null -> "null"
            else -> throw IllegalArgumentException(
                "invalid operand of type '${typeName(subject)}'"
            )
        }
    }
}
