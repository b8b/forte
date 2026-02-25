package org.cikit.forte.lib.wasmjs

external class BigInt {
    companion object {
        fun asIntN(base: Int, n: BigInt): BigInt
    }
}

external fun BigInt(s: String): BigInt
external fun BigInt(n: Double): BigInt
external fun Number(n: BigInt): Double

@OptIn(ExperimentalWasmJsInterop::class)
fun bigIntAdd(a: BigInt, b: BigInt): BigInt = js("a + b")

@OptIn(ExperimentalWasmJsInterop::class)
fun bigIntSubtract(a: BigInt, b: BigInt): BigInt = js("a - b")

@OptIn(ExperimentalWasmJsInterop::class)
fun bigIntMultiply(a: BigInt, b: BigInt): BigInt = js("a * b")

@OptIn(ExperimentalWasmJsInterop::class)
fun bigIntDivide(a: BigInt, b: BigInt): BigInt = js("a / b")

@OptIn(ExperimentalWasmJsInterop::class)
fun bigIntRemainder(a: BigInt, b: BigInt): BigInt = js("a % b")

@OptIn(ExperimentalWasmJsInterop::class)
fun bigIntPow(a: BigInt, b: BigInt): BigInt = js("a ** b")

@OptIn(ExperimentalWasmJsInterop::class)
fun bigIntNegate(a: BigInt): BigInt = js("-a")

@OptIn(ExperimentalWasmJsInterop::class)
fun bigIntGt(a: BigInt, b: BigInt): Boolean = js("a > b")

@OptIn(ExperimentalWasmJsInterop::class)
fun bigIntLt(a: BigInt, b: BigInt): Boolean = js("a < b")

@OptIn(ExperimentalWasmJsInterop::class)
fun bigIntEq(a: BigInt, b: BigInt): Boolean = js("a == b")

fun bigIntCompare(a: BigInt, b: BigInt): Int {
    return if (bigIntEq(a, b)) {
        0
    } else if (bigIntLt(a, b)) {
        -1
    } else if (bigIntGt(a, b)) {
        1
    } else {
        error(
            "compareTo undefined for operands of type " +
                    "'bigint': $a <=> $b"
        )
    }
}

fun restrictedPow(
    base: BigInt,
    exp: BigInt,
    maxBitLength: Int
): BigNumericValue {
    // handle small numbers
    val lower = BigInt(-2.0)
    val upper = BigInt(2.0)
    if (bigIntGt(base, lower) && bigIntLt(base, upper)) {
        return BigNumericValue(bigIntPow(base, exp))
    }
    if (bigIntGt(exp, lower) && bigIntLt(exp, upper)) {
        return BigNumericValue(bigIntPow(base, exp))
    }
    // BitLength(result) ~ BitLength(base) * exp
    // BitLength(result) < maxBitLength
    // => BitLength(base) * exp < maxBitLength
    // => BitLength(base) < maxBitLength / exp
    // => maxBase = 2 ^ (maxBitLength / exp)
    val exp2 = bigIntDivide(BigInt(maxBitLength.toDouble()), exp)
    val maxBase = bigIntPow(upper, exp2)
    if (bigIntGt(base, maxBase)) {
        throw ArithmeticException("base or exponent too high")
    }
    val newValue = bigIntPow(base, exp)
    return BigNumericValue(newValue)
}
