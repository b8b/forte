package org.cikit.forte.lib.core

import org.cikit.forte.core.*

sealed class FilterGet : FilterMethod {

    companion object {
        val KEY = Context.Key.Apply.create("get", FilterMethod.OPERATOR)
        val singleArg = listOf("key")
    }

    override val isRescue: Boolean
        get() = true

    override fun invoke(subject: Any?, args: NamedArgs): Any? {
        val key: Any?
        if (args.names === singleArg && args.values.size == 1) {
            key = args.values[0]
        } else {
            args.use { key = requireAny("key") }
        }
        if (subject is Undefined) {
            return subject
        }
        return when (key) {
            is CharSequence -> when (subject) {
                is TemplateObject -> {
                    val finalKey = key.concatToString()
                    val result = subject.getVar(finalKey)
                    if (result is Undefined) {
                        getFromMap(subject, finalKey)
                    } else {
                        result
                    }
                }

                else -> getFromMap(subject, key.concatToString())
            }

            is Int -> when (subject) {
                is List<*> -> subject.getOrElse(key) {
                    Undefined("index $key out of bounds for List operand " +
                            "of type '${typeName(subject)}' " +
                            "with size ${subject.size}")
                }
                is CharSequence -> if (key !in subject.indices) {
                    Undefined("index $key out of bounds for String operand " +
                            "of type '${typeName(subject)}' " +
                            "with size ${subject.length}")
                } else {
                    subject[key]
                }

                else -> getFromMap(subject, key)
            }

            else -> getFromMap(subject, key)
        }
    }

    protected fun getFromMap(subject: Any?, key: Any?): Any? {
        return when (subject) {
            is Map<*, *> -> {
                if (!subject.containsKey(key)) {
                    Undefined("key '$key' of type '${typeName(key)}' " +
                            "is missing in the Map operand " +
                            "of type '${typeName(subject)}'")
                } else {
                    subject[key]
                }
            }

            else -> Undefined(
                "invalid operand " +
                        "of type '${typeName(subject)}' with " +
                        "key '$key' of type '${typeName(key)}'"
            )
        }
    }

    object Hidden : FilterGet() {
        override val isHidden: Boolean
            get() = true
    }
}
