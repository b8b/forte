package org.cikit.forte.internal

import org.cikit.forte.core.concatToString
import org.cikit.forte.lib.js.BigNumericValue

actual fun parseInt(input: CharSequence): Number {
    val inputStr = input.concatToString()
    val intResult = inputStr.toIntOrNull()
    if (intResult == null) {
        val newValue = try {
            js("BigInt(inputStr)")
        } catch (e: dynamic) {
            throw NumberFormatException(e.toString())
        }
        return BigNumericValue(newValue)
    }
    return intResult
}
