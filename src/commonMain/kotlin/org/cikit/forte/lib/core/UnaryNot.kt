package org.cikit.forte.lib.core

import org.cikit.forte.core.Context
import org.cikit.forte.core.UnOpFunction
import org.cikit.forte.core.typeName

class UnaryNot : UnOpFunction {

    companion object {
        val KEY = Context.Key.Unary("not")
    }

    override fun invoke(arg: Any?): Boolean {
        require(arg is Boolean) {
            "operator `not` is undefined for operand " +
                    "of type '${typeName(arg)}'"
        }
        return !arg
    }
}
