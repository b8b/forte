package org.cikit.forte.emitter

import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.KSerializer
import org.cikit.forte.core.typeName

interface Emitter {
    fun emitNull()

    fun emitScalar(value: Boolean)
    fun emitScalar(value: Number)
    fun emitScalar(value: CharSequence)
    fun emitScalar(value: ByteArray)
    fun emitScalar(value: ByteString)

    fun emitStartArray()
    fun emitEndArray()

    fun emitStartObject()
    fun emitEndObject()

    fun <T> emit(serializer: KSerializer<T>, value: T)

    fun emit(value: Any?) {
        when (value) {
            null -> emitNull()
            is Map<*, *> -> {
                emitObject {
                    for ((k, v) in value) {
                        emit(k)
                        emit(v)
                    }
                }
            }
            is Iterable<*> -> {
                emitArray(value)
            }
            is ByteString -> {
                emitScalar(value)
            }
            is ByteArray -> {
                emitScalar(value)
            }
            is Boolean -> emitScalar(value)
            is Number -> emitScalar(value)
            is CharSequence -> emitScalar(value)

            else -> error(
                "cannot convert value of type '${typeName(value)}' to json"
            )
        }
    }

    fun <T> emitArray(block: () -> T): T {
        emitStartArray()
        val result = block()
        emitEndArray()
        return result
    }

    fun emitArray(value: Iterable<Any?>) = emitArray {
        for (item in value) {
            emit(item)
        }
    }

    fun <T> emitObject(block: () -> T): T {
        emitStartObject()
        val result: T = block()
        emitEndObject()
        return result
    }

    fun emitObject(value: Map<String, Any?>) = emitObject {
        for ((k, v) in value) {
            emitScalar(k)
            emit(v)
        }
    }
}

