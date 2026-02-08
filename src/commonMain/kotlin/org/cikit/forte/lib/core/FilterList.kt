package org.cikit.forte.lib.core

import org.cikit.forte.core.*

class FilterList : FilterMethod {
    override fun invoke(subject: Any?, args: NamedArgs): Any {
        val strings: CharSequence
        args.use {
            strings = optional("strings") { "codePoints" }
        }
        return when (subject) {
            is List<*> -> subject
            is MaskedList -> subject.list

            is CharSequence -> {
                when (strings.concatToString()) {
                    "empty" -> emptyList<String>()
                    "chars" -> subject.indices
                        .map { i -> subject.subSequence(i, i + 1) }
                    "codePoints" -> {
                        val result = mutableListOf<CharSequence>()
                        var i = 0
                        while (i < subject.length) {
                            val ch1 = subject[i++]
                            result += if (ch1.isHighSurrogate()) {
                                //TBD verify ch2
                                i++
                                subject.subSequence(i - 2, i)
                            } else {
                                subject.subSequence(i - 1, i)
                            }
                        }
                        result.toList()
                    }

                    else -> throw IllegalArgumentException(
                        "invalid option '$strings' for arg 'strings'"
                    )
                }
            }

            is Map<*, *> -> subject.keys.toList()

            is Iterable<*> -> subject.toList()

            else -> throw IllegalArgumentException(
                "invalid operand of type '${typeName(subject)}'"
            )
        }
    }
}
