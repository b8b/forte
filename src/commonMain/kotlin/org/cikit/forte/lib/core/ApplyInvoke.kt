package org.cikit.forte.lib.core

import org.cikit.forte.core.Method
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.typeName

class ApplyInvoke : Method {
    override fun invoke(subject: Any?, args: NamedArgs): Any {
        throw IllegalArgumentException(
            "operand of type '${typeName(subject)}' is not callable"
        )
    }
}
