package org.cikit.forte.lib.core

import org.cikit.forte.core.Context
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.TestMethod

class IsIterableTest : TestMethod {

    companion object {
        val KEY = Context.Key.Apply.create("iterable", TestMethod.OPERATOR)
    }

    override fun invoke(subject: Any?, args: NamedArgs): Boolean {
        args.requireEmpty()
        return subject is Iterable<*> || subject is CharSequence
    }
}
