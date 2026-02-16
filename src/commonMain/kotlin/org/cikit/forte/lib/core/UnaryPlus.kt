package org.cikit.forte.lib.core

import org.cikit.forte.core.UnOpFunction
import org.cikit.forte.core.typeName

class UnaryPlus : UnOpFunction {
    override fun invoke(arg: Any?): Any? {
        require(arg is Number) {
            "unary operator `plus` undefined for operand " +
                    "of type '${typeName(arg)}'"
        }
        return arg
    }
}