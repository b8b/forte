package org.cikit.forte.lib.core

import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.TestMethod

class IsCallableTest : TestMethod {
    override fun invoke(subject: Any?, args: NamedArgs): Boolean {
        args.requireEmpty()
        return false
    }
}
