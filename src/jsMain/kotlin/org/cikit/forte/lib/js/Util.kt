package org.cikit.forte.lib.js

val dynamicPlus = { a: dynamic, b: dynamic ->
    js("a + b")
}

val dynamicMinus = { a: dynamic, b: dynamic ->
    js("a - b")
}

val dynamicMultiply = { a: dynamic, b: dynamic ->
    js("a * b")
}

val dynamicDivide = { a: dynamic, b: dynamic ->
    js("a / b")
}

val dynamicReminder = { a: dynamic, b: dynamic ->
    js("a % b")
}

val dynamicPow = { a: dynamic, b: dynamic ->
    eval("a ** b")
}

val toBigInt = { a: Any -> js("BigInt(a)") }

val dynamicToNumber = { a: dynamic ->
    js("Number(a)") as Double
}

val dynamicCompareTo = { a: dynamic, b: dynamic ->
    js("a < b ? -1 : (a > b ? 1 : 0)") as Int
}
