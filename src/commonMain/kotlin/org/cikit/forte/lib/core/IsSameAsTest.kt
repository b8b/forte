package org.cikit.forte.lib.core

import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.TestMethod
import org.cikit.forte.core.Undefined

class IsSameAsTest : TestMethod {
    override fun invoke(subject: Any?, args: NamedArgs): Boolean {
        val other: Any?
        args.use {
            other = requireAny("other")
        }
        return subject !is Undefined && subject == other
    }
}
