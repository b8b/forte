package org.cikit.forte.lib.core

import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.TestMethod

class IsTrueTest : TestMethod {
    override fun invoke(subject: Any?, args: NamedArgs): Boolean {
        args.requireEmpty()
        return subject == true
    }
}
