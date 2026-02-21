package org.cikit.forte

import org.cikit.forte.core.Context
import org.cikit.forte.lib.core.FilterComparable
import org.cikit.forte.lib.core.FilterNumber
import org.cikit.forte.lib.core.FilterString
import org.cikit.forte.lib.js.JsFilterComparable
import org.cikit.forte.lib.js.JsFilterNumber
import org.cikit.forte.lib.js.JsFilterString

actual fun <R>
        Context.Builder<R>.definePlatformExtensions(): Context.Builder<R>
{
    val comparableTypes = getMethod(FilterComparable.KEY)
        ?.types
        ?: error("${FilterComparable.KEY} is not defined")
    defineMethod(FilterComparable.KEY, JsFilterComparable(comparableTypes))
    val numericTypes = getMethod(FilterNumber.KEY)
        ?.types
        ?: error("${FilterNumber.KEY} is not defined")
    defineMethod(FilterNumber.KEY, JsFilterNumber(numericTypes))
    defineMethod(FilterString.KEY, JsFilterString())
    return this
}
