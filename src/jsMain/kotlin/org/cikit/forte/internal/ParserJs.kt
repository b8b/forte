package org.cikit.forte.internal

import com.ionspin.kotlin.bignum.integer.BigInteger
import org.cikit.forte.core.concatToString
import org.cikit.forte.lib.js.BigNumericValue

actual fun parseInt(input: CharSequence): Number {
    val inputStr = input.concatToString()
    return inputStr.toIntOrNull()
        ?: BigNumericValue(BigInteger.parseString(inputStr, 10))
}
