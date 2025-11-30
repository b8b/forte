package org.cikit.forte.core

import org.cikit.forte.types.InlineString

fun typeName(value: Any?): String {
    if (value == null) {
        return "null"
    }
    return value::class.simpleName ?: value::class.toString()
}

fun CharSequence.concatToString() = this as? String
    ?: (this as? InlineString)?.toString()
    ?: CharArray(length) { i -> get(i) }.concatToString()
