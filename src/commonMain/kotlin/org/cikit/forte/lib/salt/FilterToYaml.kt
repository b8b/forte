package org.cikit.forte.lib.salt

import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.json.JsonElement
import org.cikit.forte.core.*
import org.cikit.forte.emitter.Emitter
import org.cikit.forte.emitter.YamlEmitter

class FilterToYaml: FilterMethod {
    override fun invoke(subject: Any?, args: NamedArgs): Any {
        args.requireEmpty()
        val target = StringBuilder()
        val emitter = YamlEmitter(target)
        return Suspended { ctx ->
            emitter.suspendingEmit(ctx, subject)
            emitter.close()
            target.toString()
        }
    }

    private suspend fun Emitter.suspendingEmit(
        ctx: Context.Evaluator<*>,
        value: Any?
    ) {
        when (value) {
            null -> emitNull()
            is JsonElement -> emit(JsonElement.serializer(), value)
            is Boolean -> emitScalar(value)
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
}
