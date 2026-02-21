package org.cikit.forte.internal

import org.cikit.forte.core.concatToString

actual fun parseInt(input: CharSequence): Number {
    val inputStr = input.concatToString()
    if (inputStr.any { !it.isDigit() && it != '+' && it != '-' }) {
        throw NumberFormatException()
    }
    val newValue = try {
        js("BigInt(inputStr)")
    } catch (e: dynamic) {
        throw NumberFormatException(e.toString())
    }
    return newValue
}
