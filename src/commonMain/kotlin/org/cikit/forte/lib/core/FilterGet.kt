package org.cikit.forte.lib.core

import org.cikit.forte.core.*

interface FilterGet : FilterMethod {

    companion object {
        val KEY: Context.Key.Apply<FilterGet> = Context.Key.Apply.create(
            "get",
            FilterMethod.OPERATOR
        )
        val singleArg = listOf("key")
    }

    override fun invoke(subject: Any?, args: NamedArgs): Any? {
        val numArgs = args.values.size
        if (numArgs == 1) {
            if (args.names === singleArg) {
                return getComputed(subject, args.values[0])
            }
            val numNames = args.names.size
            if (numNames == 0 || (numNames == 1 && args.names[0] == "key")) {
                return getComputed(subject, args.values[0])
            }
            if (numNames == 1) {
                when (args.names[0]) {
                    "key" -> return getComputed(subject, args.values[0])
                    "identifier" -> {
                        val identifier: String
                        args.use {
                            identifier = require("identifier")
                        }
                        return getConst(subject, identifier)
                    }

                    else -> return getSlice(subject, args)
                }
            }
            return getSlice(subject, args)
        } else {
            return getSlice(subject, args)
        }
    }

    fun getConst(subject: Any?, identifier: String): Any? {
        return getComputed(subject, identifier)
    }

    fun getComputed(subject: Any?, key: Any?): Any?

    fun getSlice(subject: Any?, args: NamedArgs): Any?

    sealed class DefaultFilterGet(
        protected val number: FilterNumber
    ) : FilterGet {

        override val isRescue: Boolean
            get() = true

        override fun getComputed(subject: Any?, key: Any?): Any? {
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

                is Number -> {
                    val key = number(key).toIntOrNull() ?: error(
                        "cannot convert argument 'key' " +
                                "of type ${typeName(key)} to int"
                    )
                    when (subject) {
                        is List<*> -> subject.getOrElse(key) {
                            Undefined(
                                "index $key out of bounds for List operand " +
                                        "of type '${typeName(subject)}' " +
                                        "with size ${subject.size}"
                            )
                        }

                        is CharSequence -> if (key !in subject.indices) {
                            Undefined(
                                "index $key out of bounds for String operand " +
                                        "of type '${typeName(subject)}' " +
                                        "with size ${subject.length}"
                            )
                        } else {
                            subject[key]
                        }

                        else -> getFromMap(subject, key)
                    }
                }

                else -> getFromMap(subject, key)
            }
        }

        override fun getSlice(subject: Any?, args: NamedArgs): Any? {
            return when (subject) {
                is CharSequence -> sliceString(subject, args)
                is List<*> -> sliceList(subject, args)
                is Char -> sliceString(subject.toString(), args)

                else -> throw IllegalArgumentException(
                    "invalid operand of type '${typeName(subject)}'"
                )
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

        protected fun sliceString(
            input: CharSequence,
            args: NamedArgs
        ): String {
            val r = range(input.length, args)
            if (r.isEmpty()) {
                return ""
            }
            if (r.step == 1) {
                return input.substring(r.first, r.last + 1)
            }
            return buildString {
                for (i in r) {
                    append(input[i])
                }
            }
        }

        protected fun sliceList(
            input: List<*>,
            args: NamedArgs
        ): List<Any?> {
            val r = range(input.size, args)
            if (r.isEmpty()) {
                return emptyList()
            }
            if (r.step == 1) {
                return input.subList(r.first, r.last + 1)
            }
            return buildList {
                for (i in r) {
                    add(input[i])
                }
            }
        }

        private fun range(size: Int, args: NamedArgs): IntProgression {
            var start: Int
            var defaultStart = false
            var end: Int
            var defaultEnd = false
            val step: Int
            args.use {
                start = optional(
                    "start",
                    convertValue = { v ->
                        number(v).toIntOrNull() ?: error(
                            "cannot convert arg 'start' to int"
                        )
                    },
                    defaultValue = { defaultStart = true; 0 }
                )
                end = optional(
                    "end",
                    convertValue = { v ->
                        number(v).toIntOrNull() ?: error(
                            "cannot convert arg 'end' to int"
                        )
                    },
                    defaultValue = { defaultEnd = true; size }
                )
                step = optional(
                    "step",
                    convertValue = { v ->
                        number(v).toIntOrNull() ?: error(
                            "cannot convert arg 'step' of type ${typeName(v)} to int"
                        )
                    },
                    defaultValue = { 1 }
                )
            }
            if (step > 0) {
                if (size <= 0 || start >= size) {
                    return IntRange.EMPTY
                }
                if (start < 0) {
                    start += size
                    if (start < 0) {
                        start = 0
                    }
                }
                if (end < 0) {
                    end += size
                }
                if (end > size) {
                    end = size
                }
                if (start >= end) {
                    return IntRange.EMPTY
                }
                return start until end step step
            }
            if (step < 0) {
                if (size <= 0) {
                    return IntRange.EMPTY
                }
                if (defaultStart || start >= size) {
                    start = size - 1
                } else {
                    if (start < 0) {
                        start += size
                        if (start < 0) {
                            return IntRange.EMPTY
                        }
                    }
                }
                // calculate endInclusive
                if (defaultEnd) {
                    end = 0
                } else {
                    end += if (end < 0) {
                        size + 1
                    } else {
                        1
                    }
                    if (end >= size) {
                        return IntRange.EMPTY
                    }
                    if (end < 0) {
                        end = 0
                    }
                }
                if (start < end) {
                    return IntRange.EMPTY
                }
                return start downTo end step (- step)
            }
            error("slice step cannot be zero")
        }
    }

    class Hidden private constructor(
        number: FilterNumber
    ) : DefaultFilterGet(number), DependencyAware {
        constructor(ctx: Context<*>) : this(ctx.filterNumber)

        override fun withDependencies(ctx: Context<*>): DependencyAware {
            val number = ctx.filterNumber
            if (number === this.number) {
                return this
            }
            return Hidden(number)
        }

        override val isHidden: Boolean
            get() = true
    }

    object NoImplementation : FilterGet {
        override fun invoke(subject: Any?, args: NamedArgs): Any? {
            error("$KEY is not defined")
        }

        override fun getComputed(subject: Any?, key: Any?): Any? {
            error("$KEY is not defined")
        }

        override fun getSlice(subject: Any?, args: NamedArgs): Any? {
            error("$KEY is not defined")
        }
    }
}
