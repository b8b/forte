package org.cikit.forte.lib.core

import org.cikit.forte.core.Context
import org.cikit.forte.core.FilterMethod
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.Undefined
import org.cikit.forte.core.optional

class FilterDefault : FilterMethod {

    companion object {
        val KEY = Context.Key.Apply.create("default", FilterMethod.OPERATOR)
    }

    override val isRescue: Boolean
        get() = true

    override fun invoke(subject: Any?, args: NamedArgs): Any? {
        val defaultValue: Any?
        val boolean: Boolean
        args.use {
            defaultValue = requireAny("default_value")
            boolean = optional("boolean") { false }
        }
        return if (subject is Undefined || (boolean && subject == false)) {
            defaultValue
        } else {
            subject
        }
    }
}
