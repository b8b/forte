package org.cikit.forte.internal

import org.cikit.forte.core.concatToString
import org.cikit.forte.lib.wasmjs.BigInt
import org.cikit.forte.lib.wasmjs.BigNumericValue

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

    val intResult = inputStr.toIntOrNull()
    if (intResult == null) {
        val newValue = try {
            BigInt(inputStr)
        } catch (ex: Throwable) {
            throw NumberFormatException(ex.message)
        }
        return BigNumericValue(newValue)
    }
    return intResult
}
