package org.cikit.forte.internal

import org.cikit.forte.core.concatToString
import java.math.BigInteger

actual fun parseInt(input: CharSequence): Number {
    val inputStr = input.concatToString()
    return inputStr.toIntOrNull()
        ?: BigInteger(inputStr)
}
