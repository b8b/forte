package org.cikit.forte.lib.jinja

import org.cikit.forte.core.*

class FilterAttr : FilterMethod {
    override fun invoke(subject: Any?, args: NamedArgs): Any {
        val name: String
        args.use {
            name = require<String>("name")
        }
        return Undefined("operand of type '${typeName(subject)}' " +
                "has not attribute '$name'")
    }
}
