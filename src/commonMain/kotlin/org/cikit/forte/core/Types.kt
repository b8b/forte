package org.cikit.forte.core

fun typeName(value: Any?): String {
    if (value == null) {
        return "null"
    }
    return value::class.simpleName ?: value::class.toString()
}
