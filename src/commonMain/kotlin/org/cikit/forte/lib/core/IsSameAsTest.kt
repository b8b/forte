package org.cikit.forte.lib.core

import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.NumericValue
import org.cikit.forte.core.TestMethod
import org.cikit.forte.core.Undefined

class IsSameAsTest : TestMethod {
    override fun invoke(subject: Any?, args: NamedArgs): Boolean {
        val other: Any?
        args.use {
            other = requireAny("other")
        }
        val isNaN = when (subject) {
            is NumericValue -> subject.doubleOrNull()?.isNaN() == true
            is Float -> subject.isNaN()
            is Double -> subject.isNaN()

            else -> false
        }
        if (isNaN) {
            val otherIsNaN = when (other) {
                is NumericValue -> other.doubleOrNull()?.isNaN() == true
                is Double -> other.isNaN()
                is Float -> other.isNaN()

                else -> false
            }
            if (otherIsNaN) {
                return true
            }
        }
        return subject !is Undefined && subject == other
    }
}
