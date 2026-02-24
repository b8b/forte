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
            return BigNumericValue(subject as Long)
        }
        if (js("typeof(subject) === 'number'")) {
            return try {
                BigNumericValue(BigInt(subject))
            } catch (_: Throwable) {
                FloatNumericValue(subject as Double)
            }
        }
        return when (subject) {
            null -> null
            is Boolean -> FloatNumericValue(if (subject) 1.0 else 0.0)
            is CharSequence -> try {
                BigNumericValue(parseInt(subject) as Long)
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
            return BigNumericValue(n)
        }
        require(js("typeof(n) === 'number'")) {
            "not a number"
        }
        val truncated = numberToInt32(n)
        return if (truncated == n) {
            BigNumericValue(BigInt(truncated))
        } else {
            FloatNumericValue(n as Double)
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
            val truncated = BigInt.asIntN(32, n)
            require(BigInt.eq(truncated, n)) {
                "integer not in int32 range"
            }
            return numberToInt32(Number(truncated))
        }
        require(js("typeof(n) === 'number'")) {
            "value is not a number"
        }
        val truncated = numberToInt32(n)
        require(truncated == n) {
            "value is not an integer"
        }
        return truncated
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
        val truncated = BigInt.asIntN(64, n)
        require(truncated == n) {
            "integer not in int64 range"
        }
        return truncated
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
