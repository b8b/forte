package org.cikit.forte.lib.core

import org.cikit.forte.core.Context
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.TestMethod
import org.cikit.forte.core.Undefined

class IsDefinedTest : TestMethod {

    companion object {
        val KEY = Context.Key.Apply.create("defined", TestMethod.OPERATOR)
    }

    override val isRescue: Boolean
        get() = true

    override fun invoke(subject: Any?, args: NamedArgs): Boolean {
        args.requireEmpty()
        return subject !is Undefined
    }
}
