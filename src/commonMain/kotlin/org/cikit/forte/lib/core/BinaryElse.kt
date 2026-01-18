package org.cikit.forte.lib.core

import org.cikit.forte.core.ConditionalBinOpFunction
import org.cikit.forte.core.ConditionalResult
import org.cikit.forte.core.Undefined

class BinaryElse : ConditionalBinOpFunction {

    override val isRescue: Boolean
        get() = true

    override fun checkCondition(left: Any?): ConditionalResult {
        return if (left is Undefined) {
            ConditionalResult.Continue
        } else {
            ConditionalResult.Return(left)
        }
    }

    override fun invoke(left: Any?, right: Any?): Any? {
        return if (left is Undefined) {
            right
        } else {
            left
        }
    }
}
