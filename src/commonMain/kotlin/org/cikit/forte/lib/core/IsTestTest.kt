package org.cikit.forte.lib.core

import org.cikit.forte.core.*

class IsTestTest: TestMethod  {
    override fun invoke(subject: Any?, args: NamedArgs): Suspended {
        args.requireEmpty()
        require(subject is CharSequence) {
            "invalid operand of type '${typeName(subject)}'"
        }
        return Suspended { ctx ->
            null != ctx.getMethod(
                Context.Key.Apply(subject.concatToString(), "is")
            )
        }
    }
}
