package org.cikit.forte.lib.common

import org.cikit.forte.core.Context

fun <R> Context.Builder<R>.defineCommonExtensions(): Context.Builder<R> {
    defineMethod("first", FilterFirst())
    defineMethod("last", FilterLast())

    defineMethod("dictsort", FilterDictSort(this))
    defineMethod("items", FilterItems())
    defineMethod("sort", FilterSort(this, unique = false))
    defineMethod("unique", FilterSort(this, unique = true))
    defineMethod("sum", FilterSum(this))
    defineMethod("min", FilterMinMax(this, min = true))
    defineMethod("max", FilterMinMax(this, min = false))

    defineMethod("map", FilterMap())
    defineMethod("selectattr", FilterSelectAttr(this, cond = true))
    defineMethod("rejectattr", FilterSelectAttr(this, cond = false))
    defineMethod("select", FilterSelect(cond = true))
    defineMethod("reject", FilterSelect(cond = false))

    defineMethod("join", FilterJoin())

    defineMethod("lower", FilterLower())
    defineMethod("replace", FilterReplace(this))
    defineMethod("trim", FilterTrim())
    defineMethod("upper", FilterUpper())

    defineMethod("tojson", FilterToJson(this))

    return this
}
