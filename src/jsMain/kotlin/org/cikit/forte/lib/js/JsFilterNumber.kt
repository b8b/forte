package org.cikit.forte.lib.js

import org.cikit.forte.core.NumericValue
import org.cikit.forte.core.typeName
import org.cikit.forte.lib.core.FilterNumber
import kotlin.reflect.KClass

class JsFilterNumber(
    override val types: Map<KClass<*>, (Any) -> NumericValue>
) : FilterNumber {

    override val isHidden: Boolean
        get() = true

    override operator fun invoke(subject: Any?): NumericValue {
        if (subject is NumericValue) {
            return subject
        }
        return when (js("typeof(subject)")) {
            "number" -> FloatNumericValue(subject as Double)
            "bigint" -> BigNumericValue(subject as Long)
            else -> {
                subject?.let {
                    types[subject::class]?.invoke(subject)
                } ?: error(
                    "operand of type ${typeName(subject)} is not a number"
                )
            }
        }
    }

}
