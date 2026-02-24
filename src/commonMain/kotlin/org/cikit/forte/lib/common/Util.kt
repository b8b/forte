package org.cikit.forte.lib.common

import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.json.JsonElement
import org.cikit.forte.core.Context
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.NumericValue
import org.cikit.forte.core.Suspended
import org.cikit.forte.core.Undefined
import org.cikit.forte.emitter.Emitter

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

suspend fun Emitter.suspendingEmit(
    ctx: Context.Evaluator<*>,
    value: Any?
) {
    when (value) {
        null -> emitNull()
        is JsonElement -> emit(JsonElement.serializer(), value)
        is Boolean -> emitScalar(value)
        is NumericValue -> emitScalar(value)
        is Number -> emitScalar(value)
        is CharSequence -> emitScalar(value)
        is ByteString -> emitScalar(value)
        is ByteArray -> emitScalar(value)
        is Map<*, *> -> {
            emitStartObject()
            for ((k, v) in value) {
                var key = ctx.filterString(k, NamedArgs.Empty)
                if (key is Suspended) {
                    key = key.eval(ctx)
                }
                if (key is Undefined) {
                    error(key.message)
                }
                emitScalar(key.toString())
                suspendingEmit(ctx, v)
            }
            emitEndObject()
        }
        is Iterable<*> -> {
            emitStartArray()
            for (item in value) {
                suspendingEmit(ctx, item)
            }
            emitEndArray()
        }
        else -> {
            var str = ctx.filterString(value, NamedArgs.Empty)
            if (str is Suspended) {
                str = str.eval(ctx)
            }
            if (str is Undefined) {
                error(str.message)
            }
            emitScalar(str.toString())
        }
    }
}
