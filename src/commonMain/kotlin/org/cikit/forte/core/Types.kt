package org.cikit.forte.core

fun typeName(value: Any?): String {
    if (value == null) {
        return "null"
    }
    return value::class.simpleName ?: value::class.toString()
}

fun CharSequence.concatToString() = this as? String
    ?: CharArray(length) { i -> get(i) }.concatToString()
