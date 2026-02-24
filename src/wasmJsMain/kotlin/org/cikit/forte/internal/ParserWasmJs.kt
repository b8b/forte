package org.cikit.forte.internal

import com.ionspin.kotlin.bignum.integer.BigInteger
import org.cikit.forte.core.concatToString
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
        val newValue = BigInteger.parseString(inputStr, 10)
        return BigNumericValue(newValue)
    }
    return intResult
}
