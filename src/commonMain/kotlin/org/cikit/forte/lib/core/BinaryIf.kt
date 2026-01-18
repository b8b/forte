package org.cikit.forte.lib.core

import org.cikit.forte.core.ConditionalBinOpFunction
import org.cikit.forte.core.ConditionalResult
import org.cikit.forte.core.Undefined
import org.cikit.forte.core.typeName

class BinaryIf : ConditionalBinOpFunction {

    companion object {
        private val undefined = Undefined(
            "inline if expression evaluated to false"
        )
    }

    override fun checkCondition(left: Any?): ConditionalResult {
        if (left !is Boolean) {
            error(
                "operator `if` undefined for operand " +
                        "of type '${typeName(left)}'"
            )
        }
        return if (left) {
            ConditionalResult.Continue
        } else {
            ConditionalResult.Return(undefined)
        }
    }

    override fun invoke(left: Any?, right: Any?): Any? {
        return right
    }
}
