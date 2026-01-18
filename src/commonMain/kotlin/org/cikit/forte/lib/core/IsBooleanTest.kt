package org.cikit.forte.lib.core

import org.cikit.forte.core.*

class IsBooleanTest : TestMethod {
    override fun invoke(subject: Any?, args: NamedArgs): Boolean {
        args.requireEmpty()
        return subject is Boolean
    }
}
