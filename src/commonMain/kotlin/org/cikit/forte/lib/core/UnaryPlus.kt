package org.cikit.forte.lib.core

import org.cikit.forte.core.NumericValue
import org.cikit.forte.core.UnOpFunction
import org.cikit.forte.core.typeName

class UnaryPlus : UnOpFunction {
    override fun invoke(arg: Any?): Any? {
        return when (arg) {
            is NumericValue -> arg
            is Number -> arg

            else -> throw IllegalArgumentException(
                "unary operator `plus` undefined for operand " +
                        "of type '${typeName(arg)}'"
            )
        }
    }
}
