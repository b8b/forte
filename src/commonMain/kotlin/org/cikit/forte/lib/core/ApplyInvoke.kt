package org.cikit.forte.lib.core

import org.cikit.forte.core.Context
import org.cikit.forte.core.Function
import org.cikit.forte.core.Method
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.typeName

class ApplyInvoke : Method {
    companion object {
        val KEY = Context.Key.Apply.create("invoke", Method.OPERATOR)
    }

    override val isHidden: Boolean
        get() = true

    override fun invoke(subject: Any?, args: NamedArgs): Any? {
        if (subject is Function) {
            return subject.invoke(args)
        }
        throw IllegalArgumentException(
            "operand of type '${typeName(subject)}' is not callable"
        )
    }
}
