package org.cikit.forte.lib.core

import org.cikit.forte.core.ConditionalBinOpFunction
import org.cikit.forte.core.ConditionalResult
import org.cikit.forte.core.typeName

class BinaryOr : ConditionalBinOpFunction {
    override fun checkCondition(left: Any?): ConditionalResult {
        if (left !is Boolean) {
            error(
                "operator `or` undefined for operand " +
                        "of type '${typeName(left)}'"
            )
        }
        return if (left) {
            ConditionalResult.Return(left)
        } else {
            ConditionalResult.Continue
        }
    }

    override fun invoke(left: Any?, right: Any?): Boolean {
        if (right !is Boolean) {
            binOpTypeError("or", left, right)
        }
        return right
    }
}
