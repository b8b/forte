package org.cikit.forte.internal

import com.ionspin.kotlin.bignum.integer.BigInteger
import org.cikit.forte.core.concatToString
import org.cikit.forte.lib.wasmjs.BigNumericValue

actual fun parseInt(input: CharSequence): Number {
    val inputStr = input.concatToString()
    val intResult = inputStr.toIntOrNull()
    if (intResult == null) {
        val newValue = BigInteger.parseString(inputStr, 10)
        return BigNumericValue(newValue)
    }
    return intResult
}
