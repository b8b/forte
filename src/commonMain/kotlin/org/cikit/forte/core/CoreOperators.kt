package org.cikit.forte.core

internal enum class CoreOperators(val value: String) {
    Invoke("invoke"),
    Filter("pipe"),
    Test("is"),
    TestNot("is_not")
}
