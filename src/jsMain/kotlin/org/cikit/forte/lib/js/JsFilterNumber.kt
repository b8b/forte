package org.cikit.forte.lib.js

import org.cikit.forte.core.NumericValue
import org.cikit.forte.core.typeName
import org.cikit.forte.internal.parseInt
import org.cikit.forte.lib.core.FilterNumber
import kotlin.reflect.KClass

class JsFilterNumber(
    override val types: Map<KClass<*>, (Any) -> NumericValue> = emptyMap()
) : FilterNumber {

    override val isHidden: Boolean
        get() = true

    override operator fun invoke(subject: Any?): NumericValue {
        if (subject is NumericValue) {
            return subject
        }
        if (js("typeof(subject) === 'bigint'")) {
            return BigNumericValue(subject.asDynamic())
        }
        if (js("typeof(subject) === 'number'")) {
            return try {
                BigNumericValue(BigInt(subject as Double))
            } catch (_: Throwable) {
                FloatNumericValue(subject as Double)
            }
        }
        return when (subject) {
            null -> null
            is Boolean -> FloatNumericValue(if (subject) 1.0 else 0.0)
            is CharSequence -> try {
                BigNumericValue(parseInt(subject).asDynamic())
            } catch (_: Throwable) {
                val str: String = subject.toString()
                FloatNumericValue(str.toDouble())
            }
            is Char -> FloatNumericValue(subject.digitToInt().toDouble())

            else -> types[subject::class]?.invoke(subject)
        } ?: error(
            "cannot convert operand of type ${typeName(subject)} to number"
        )
    }

    override fun requireNumber(n: Any?): NumericValue {
        if (n is NumericValue) {
            return n
        }
        if (js("typeof(n) === 'bigint'")) {
            return BigNumericValue(n.asDynamic())
        }
        require(js("typeof(n) === 'number'")) {
            "not a number"
        }
        val truncated: Double = numberToInt32(n as Double)
        return if (truncated == n) {
            BigNumericValue(BigInt(truncated))
        } else {
            FloatNumericValue(n)
        }
    }

    override fun requireInt(n: Any?): Int {
        if (n is NumericValue) {
            val result = n.intOrNull()
            require(result != null) {
                if (n.isInt) {
                    "integer not in int32 range"
                } else {
                    "value is not an integer"
                }
            }
            return result
        }
        if (js("typeof(n) === 'bigint'")) {
            val truncated = BigInt.asIntN(32, n.asDynamic())
            require(bigIntEq(truncated, n.asDynamic())) {
                "integer not in int32 range"
            }
            return numberToInt32(Number(truncated)).toInt()
        }
        require(js("typeof(n) === 'number'")) {
            "value is not a number"
        }
        val truncated = numberToInt32(n as Double)
        require(truncated == n) {
            "value is not an integer"
        }
        return truncated.toInt()
    }

    override fun requireLong(n: Any?): Long {
        if (n is NumericValue) {
            val result = n.longOrNull()
            require(result != null) {
                if (n.isInt) {
                    "integer not in int64 range"
                } else {
                    "value is not an integer"
                }
            }
            return result
        }
        require(js("typeof(n) === 'bigint'")) {
            "value is not a long integer"
        }
        val truncated = BigInt.asIntN(64, n.asDynamic())
        require(truncated == n) {
            "integer not in int64 range"
        }
        return truncated.asDynamic()
    }

    override fun requireDouble(n: Any?): Double {
        if (n is NumericValue) {
            val result = n.doubleOrNull()
            require(result != null) {
                "value is not a float"
            }
            return result
        }
        require(js("typeof(n) === 'number'")) {
            "value is not a number"
        }
        return n as Double
    }
}
