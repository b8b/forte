package org.cikit.forte.lib.js

import org.cikit.forte.core.ComparableValue
import org.cikit.forte.core.NumericValue
import org.cikit.forte.core.concatToString
import org.cikit.forte.lib.core.FilterComparable
import org.cikit.forte.lib.core.FilterComparable.ListComparableValue
import org.cikit.forte.lib.core.FilterComparable.StringComparableValue
import kotlin.reflect.KClass

class JsFilterComparable(
    override val types: Map<KClass<*>, (Any?, Any, Boolean) -> ComparableValue?>
) : FilterComparable {

    override val isHidden: Boolean
        get() = true

    override fun test(
        subject: Any?,
        originalValue: Any?,
        ignoreCase: Boolean
    ): ComparableValue? {
        return when (subject) {
            null -> null
            is NumericValue -> subject.toComparableValue(originalValue)
            is CharSequence -> StringComparableValue(
                originalValue,
                subject.concatToString(),
                ignoreCase
            )

            is Iterable<*> -> ListComparableValue(
                originalValue,
                subject.map { invoke(it, it, ignoreCase) }
            )

            else -> when (js("typeof(subject)")) {
                "number" -> {
                    FloatNumericValue(subject as Double)
                        .toComparableValue(originalValue)
                }
                "bigint" -> {
                    BigComparableValue(originalValue, subject as Long)
                }

                else -> types[subject::class]?.invoke(
                    originalValue,
                    subject,
                    ignoreCase
                )
            }
        }
    }

}
