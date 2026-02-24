package org.cikit.forte.internal

import org.cikit.forte.core.concatToString

actual fun parseInt(input: CharSequence): Number {
    val inputStr = input.concatToString()
    if (inputStr.isEmpty()) throw NumberFormatException("empty string")

    var haveDigit = false
    for (i in inputStr.indices) {
        when (val ch = inputStr[i]) {
            in '0'..'9' -> haveDigit = true
            '+' -> if (i != 0) throw NumberFormatException("sign at $i")
            '-' -> if (i != 0) throw NumberFormatException("sign at $i")
            else -> throw NumberFormatException("invalid '$ch' at $i")
        }
    }
    if (!haveDigit) throw NumberFormatException("no digits")

    val newValue = try {
        js("BigInt(inputStr)")
    } catch (e: dynamic) {
        throw NumberFormatException(e.toString())
    }
    return newValue
}
