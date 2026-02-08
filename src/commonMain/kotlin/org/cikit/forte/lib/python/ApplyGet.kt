package org.cikit.forte.lib.python

import org.cikit.forte.core.Method
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.Undefined
import org.cikit.forte.core.optional
import org.cikit.forte.core.typeName

/**
 * get
 * Returns the value of the item with the specified key.
 *
 *     Parameters:
 *             keyname – Required. The keyname of the item you want to return the value from
 *             value – Optional. A value to return if the specified key does not exist.
 *
 */
class ApplyGet : Method {
    companion object {
        val NO_DEFAULT_VALUE = Undefined("no default value")
    }

    override fun invoke(subject: Any?, args: NamedArgs): Any? {
        val key: Any?
        val value: Any?
        args.use {
            key = requireAny("keyname")
            value = optional("value") { NO_DEFAULT_VALUE }
        }
        require(subject is Map<*, *>) {
            "invalid operand of type '${typeName(subject)}'"
        }
        if (subject.containsKey(key)) {
            return subject[key]
        }
        if (value === NO_DEFAULT_VALUE) {
            return Undefined("key '$key' of type '${typeName(key)}' " +
                    "is missing in the Map operand " +
                    "of type '${typeName(subject)}'")
        }
        return value
    }
}
