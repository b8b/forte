package org.cikit.forte.lib.js

external class BigInt {
    companion object {
        fun asIntN(base: Int, n: dynamic): dynamic
    }
}

external fun BigInt(n: dynamic): dynamic

fun BigInt.Companion.add(a: dynamic, b: dynamic): dynamic = js("a + b")
fun BigInt.Companion.subtract(a: dynamic, b: dynamic): dynamic = js("a - b")
fun BigInt.Companion.multiply(a: dynamic, b: dynamic): dynamic = js("a * b")
fun BigInt.Companion.divide(a: dynamic, b: dynamic): dynamic = js("a / b")
fun BigInt.Companion.remainder(a: dynamic, b: dynamic): dynamic = js("a % b")
fun BigInt.Companion.pow(a: dynamic, b: dynamic): dynamic = eval("a ** b")
fun BigInt.Companion.negate(a: dynamic): dynamic = js("-a")
fun BigInt.Companion.gt(a: dynamic, b: dynamic): Boolean = js("a > b")
fun BigInt.Companion.lt(a: dynamic, b: dynamic): Boolean = js("a < b")
fun BigInt.Companion.eq(a: dynamic, b: dynamic): Boolean = js("a == b")
fun BigInt.Companion.compare(a: dynamic, b: dynamic): Int {
    return if (BigInt.eq(a, b)) {
        0
    } else if (BigInt.lt(a, b)) {
        -1
    } else if (BigInt.gt(a, b)) {
        1
    } else {
        error(
            "compareTo undefined for operands of type " +
                    "'bigint': $a <=> $b"
        )
    }
}

external fun Number(a: dynamic): dynamic

fun numberToInt32(a: dynamic): Int = js("a | 0")

fun numberToString(n: dynamic): String {
    return if (js("Number.isNaN(n)")) {
        "NaN"
    } else if (js("n === Infinity")) {
        "Infinity"
    } else if (js("n === -Infinity")) {
        "-Infinity"
    } else if (js("Object.is(n, -0.0)")) {
        "-0.0"
    } else {
        val result = n.toString()
        if ("." in result) {
            result
        } else {
            "$result.0"
        }
    }
}
