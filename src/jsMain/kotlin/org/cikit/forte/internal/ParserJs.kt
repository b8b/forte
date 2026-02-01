package org.cikit.forte.internal

import org.cikit.forte.core.concatToString
import org.cikit.forte.lib.js.BigNumericValue

actual fun parseInt(input: CharSequence): Number {
    val inputStr = input.concatToString()
    val intResult = inputStr.toIntOrNull()
    if (intResult == null) {
        val newValue = js("BigInt(inputStr)")
        return BigNumericValue(newValue)
    }
    return intResult
}
