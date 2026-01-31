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
        return when (subject) {
            is Undefined -> subject
            is TemplateObject -> when (key) {
                is CharSequence -> subject.getVar(key.concatToString())

                else -> Undefined(
                    "invalid key '$key' of type '${typeName(key)}' for " +
                        " TemplateObject of type '${typeName(subject)}'")
            }
            is Map<*, *> -> {
                if (!subject.containsKey(key)) {
                    Undefined("key '$key' of type '${typeName(key)}' " +
                            "is missing in the Map operand " +
                            "of type '${typeName(subject)}'")
                } else {
                    subject[key]
                }
            }
            is List<*> -> when (key) {
                is Int -> subject.getOrElse(key) {
                    Undefined("index $key out of bounds for List operand " +
                            "of type '${typeName(subject)}' " +
                            "with size ${subject.size}")
                }
                else -> Undefined(
                    "invalid key '$key' of type '${typeName(key)}' for " +
                            " List operand of type '${typeName(subject)}'"
                )
            }
            is CharSequence -> when (key) {
                is Int -> if (key !in subject.indices) {
                    Undefined("index $key out of bounds for String operand " +
                            "of type '${typeName(subject)}' " +
                            "with size ${subject.length}")
                } else {
                    subject[key]
                }
                else -> Undefined(
                    "invalid key '$key' of type '${typeName(key)}' for " +
                            " String operand of type '${typeName(subject)}'"
                )
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
