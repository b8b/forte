package org.cikit.forte.lib.jinja

import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.TestMethod

class IsUpperTest : TestMethod {
    override fun invoke(subject: Any?, args: NamedArgs): Boolean {
        args.requireEmpty()
        return subject is CharSequence && subject.all { it.isUpperCase() }
    }
}
