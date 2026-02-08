package org.cikit.forte.lib.core

import org.cikit.forte.core.Context
import org.cikit.forte.core.FilterMethod
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.typeName

class FilterDict : FilterMethod {
    companion object {
        val KEY = Context.Key.Apply.create("dict", FilterMethod.OPERATOR)
    }

    override val isHidden: Boolean
        get() = true

    override fun invoke(subject: Any?, args: NamedArgs): Any {
        args.requireEmpty()
        require(subject is Map<*, *>) {
            "invalid operand of type '${typeName(subject)}'"
        }
        return subject
    }
}
