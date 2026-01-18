package org.cikit.forte.lib.core

import org.cikit.forte.core.ConditionalBinOpFunction
import org.cikit.forte.core.ConditionalResult
import org.cikit.forte.core.typeName

class BinaryAnd : ConditionalBinOpFunction {
    override fun checkCondition(left: Any?): ConditionalResult {
        if (left !is Boolean) {
            error(
                "operator `and` undefined for operand " +
                        "of type '${typeName(left)}'"
            )
        }
        return if (left) {
            ConditionalResult.Continue
        } else {
            ConditionalResult.Return(left)
        }
    }

    override fun invoke(left: Any?, right: Any?): Boolean {
        if (right !is Boolean) {
            binOpTypeError("and", left, right)
        }
        return right
    }
}
