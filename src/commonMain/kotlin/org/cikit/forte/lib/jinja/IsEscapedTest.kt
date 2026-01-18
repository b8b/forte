package org.cikit.forte.lib.jinja

import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.TestMethod

class IsEscapedTest : TestMethod {
    override fun invoke(subject: Any?, args: NamedArgs): Boolean {
        args.requireEmpty()
        return false
    }
}
