package org.cikit.forte

import kotlinx.serialization.KSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.ByteString
import okio.ByteString.Companion.toByteString

interface Emitter {
    fun emitNull()

    fun emitScalar(value: Boolean)
    fun emitScalar(value: Number)
    fun emitScalar(value: String)
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
                emitObject(value.mapKeys { (k, _) -> k.toString() })
            }
            is List<*> -> {
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
            is String -> emitScalar(value)
            else -> emitScalar(value.toString())
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

enum class ByteStringEncoding {
    ARRAY, BASE64, YAML
}

class JsonEmitter(
    val target: Appendable,
    val byteStringEncoding: ByteStringEncoding = ByteStringEncoding.ARRAY
) : Emitter {
    private val state = ArrayDeque(listOf(State.DOCUMENT_START))
    private val json = Json

    enum class State {
        DOCUMENT_START,
        OBJECT_START, KEY, VALUE,
        ARRAY_START, ARRAY_VALUE,
        DOCUMENT_END
    }

    private fun emitEncoded(s: String) {
        val newState = when (state.last()) {
            State.DOCUMENT_START -> {
                target.append(s)
                State.DOCUMENT_END
            }
            State.OBJECT_START -> {
                target.append(s)
                State.VALUE
            }
            State.KEY -> {
                target.append(",")
                target.append(s)
                State.VALUE
            }
            State.VALUE -> {
                target.append(":")
                target.append(s)
                State.KEY
            }
            State.ARRAY_START -> {
                target.append(s)
                State.ARRAY_VALUE
            }
            State.ARRAY_VALUE -> {
                target.append(",")
                target.append(s)
                State.ARRAY_VALUE
            }
            State.DOCUMENT_END -> throw IllegalStateException(
                "emit in state $state"
            )
        }
        state.removeLast()
        state.addLast(newState)
    }

    override fun emitStartArray() {
        when (state.last()) {
            State.DOCUMENT_END, State.KEY -> throw IllegalStateException(
                "start array in state $state"
            )
            else -> {
                emitEncoded("[")
                state.addLast(State.ARRAY_START)
            }
        }
    }

    override fun emitEndArray() {
        when (state.last()) {
            State.ARRAY_START, State.ARRAY_VALUE -> state.removeLast()
            else -> throw IllegalStateException(
                "end array in state $state"
            )
        }
        target.append("]")
    }

    override fun emitStartObject() {
        when (state.last()) {
            State.DOCUMENT_END, State.KEY -> throw IllegalStateException(
                "start object in state $state"
            )
            else -> {
                emitEncoded("{")
                state.addLast(State.OBJECT_START)
            }
        }
    }

    override fun emitEndObject() {
        when (state.last()) {
            State.OBJECT_START, State.KEY -> state.removeLast()
            else -> throw IllegalStateException(
                "end object in state $state"
            )
        }
        target.append("}")
    }

    override fun emitNull() {
        emitEncoded("null")
    }
    override fun emitScalar(value: Boolean) {
        when (value) {
            true -> emitEncoded("true")
            false -> emitEncoded("false")
        }
    }

    override fun emitScalar(value: Number) =
        emitEncoded(value.toString())

    override fun emitScalar(value: String) =
        emitEncoded(json.encodeToString(value))

    override fun emitScalar(value: ByteArray) {
        when (byteStringEncoding) {
            ByteStringEncoding.ARRAY -> emitArray {
                for (element in value) {
                    emitScalar(element)
                }
            }
            else -> emit(value.toByteString())
        }
    }

    override fun emitScalar(value: ByteString) {
        when (byteStringEncoding) {
            ByteStringEncoding.ARRAY -> emitArray {
                for (i in 0 until value.size) {
                    emitScalar(value[i])
                }
            }
            ByteStringEncoding.BASE64 -> emit(value.base64())
            ByteStringEncoding.YAML -> {
                val encoded = buildString {
                    append("!!binary ")
                    append(Json.encodeToString(value.base64()))
                }
                emitEncoded(encoded)
            }
        }
    }

    override fun <T> emit(serializer: KSerializer<T>, value: T) =
        emitEncoded(Json.encodeToString(serializer, value))

    fun close() {
        require(state.singleOrNull() == State.DOCUMENT_END) {
            "close in state $state"
        }
    }

    companion object {
        fun encodeToString(
            byteStringEncoding: ByteStringEncoding = ByteStringEncoding.ARRAY,
            block: Emitter.() -> Unit
        ): String {
            val result = StringBuilder()
            val emitter = JsonEmitter(result, byteStringEncoding)
            emitter.block()
            emitter.close()
            return result.toString()
        }
    }
}

/**
 * TODO: push lines to target Appendable on every emitEnd() rather than buffering the whole output
 */
class YamlEmitter(val target: Appendable) : Emitter {
    private val stack = ArrayDeque(listOf(YamlShallowEmitter()))
    private val emitter: YamlShallowEmitter get() = stack.last()

    override fun emitNull() = emitter.emitNull()
    override fun emitScalar(value: Boolean) = emitter.emitScalar(value)
    override fun emitScalar(value: Number) = emitter.emitScalar(value)
    override fun emitScalar(value: String) = emitter.emitScalar(value)
    override fun emitScalar(value: ByteString) = emitter.emitScalar(value)

    override fun emitScalar(value: ByteArray) {
        emitter.emitScalar(value.toByteString())
    }

    override fun <T> emit(serializer: KSerializer<T>, value: T) {
        emitter.emit(serializer, value)
    }

    override fun emitStartArray() {
        stack.addLast(YamlShallowEmitter())
        emitter.emitStartArray()
    }

    override fun emitEndArray() {
        val forceBlock: Boolean
        val encoded = stack.removeLast().let { removed ->
            removed.emitEndArray()
            removed.close()
            forceBlock = removed.target.singleOrNull() != "[]"
            removed.target.toTypedArray()
        }
        emitter.emitEncoded(*encoded, forceBlock = forceBlock)
    }

    override fun emitStartObject() {
        stack.addLast(YamlShallowEmitter())
        emitter.emitStartObject()
    }

    override fun emitEndObject() {
        val forceBlock: Boolean
        val encoded = stack.removeLast().let { removed ->
            removed.emitEndObject()
            removed.close()
            forceBlock = removed.target.singleOrNull() != "{}"
            removed.target.toTypedArray()
        }
        emitter.emitEncoded(*encoded, forceBlock = forceBlock)
    }

    fun close() {
        val encoded = stack.removeLast().let { removed ->
            removed.close()
            removed.target.toTypedArray()
        }
        require(stack.isEmpty()) {
            "close in state $stack"
        }
        encoded.forEachIndexed { index, line ->
            target.append(line)
            if (index < encoded.indices.last) {
                target.appendLine()
            }
        }
    }

    companion object {
        fun encodeToString(value: Any?) = encodeToString { emit(value) }
        fun encodeToString(block: Emitter.() -> Unit): String {
            val result = StringBuilder()
            val emitter = YamlEmitter(result)
            emitter.block()
            emitter.close()
            return result.toString()
        }
    }
}

private class YamlShallowEmitter(
    val target: MutableList<String> = mutableListOf()
) : Emitter {
    private enum class State {
        INITIAL, ARRAY0, ARRAY, OBJECT0, OBJECT_KEY, OBJECT_VALUE, DONE
    }

    private var state = State.INITIAL
    private var key = ""
    private val json = Json

    fun emitEncoded(vararg values: String, forceBlock: Boolean = false) {
        state = when (state) {
            State.INITIAL -> {
                target += values
                State.DONE
            }
            State.ARRAY0, State.ARRAY -> {
                val valuesIt = values.iterator()
                target += "- ${valuesIt.next()}"
                while (valuesIt.hasNext()) {
                    target += "  ${valuesIt.next()}"
                }
                State.ARRAY
            }
            State.OBJECT0, State.OBJECT_KEY -> {
                key = values.singleOrNull() ?: error(
                    "expected single line for mapping key"
                )
                State.OBJECT_VALUE
            }
            State.OBJECT_VALUE -> {
                val valuesIt = values.iterator()
                val firstValue = valuesIt.next()
                if (forceBlock || valuesIt.hasNext()) {
                    target += "$key:"
                    target += "  $firstValue"
                    while (valuesIt.hasNext()) {
                        target += "  ${valuesIt.next()}"
                    }
                } else {
                    target += "$key: $firstValue"
                }
                State.OBJECT_KEY
            }
            State.DONE -> {
                error("emit in state DONE")
            }
        }
    }

    override fun emitNull() = emitEncoded("null")

    override fun emitScalar(value: Boolean) = emitEncoded(
        when (value) {
            true -> "true"
            false -> "false"
        }
    )

    override fun emitScalar(value: Number) {
        when (value) {
            Float.NaN, Double.NaN -> emitEncoded(".nan")
            Float.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY -> emitEncoded("+.inf")
            Float.NEGATIVE_INFINITY,
            Double.NEGATIVE_INFINITY -> emitEncoded("-.inf")
            else -> emitEncoded(value.toString())
        }
    }

    override fun emitScalar(value: String) {
        if (value.endsWith("\n")) {
            val lines = value.split("\n")
            val encoded = Array(lines.size + 1) { i ->
                if (i == 0) {
                    "|"
                } else {
                    "  ${lines[i - 1]}"
                }
            }
            emitEncoded(*encoded)
        } else {
            emitEncoded(json.encodeToString(value))
        }
    }

    override fun emitScalar(value: ByteArray) = emitScalar(value.toByteString())

    override fun emitScalar(value: ByteString) {
        val base64 = value.base64().chunked(76)
        val encoded = Array(base64.size + 1) { i ->
            if (i == 0) {
                "!!binary |-"
            } else {
                "  ${base64[i - 1]}"
            }
        }
        emitEncoded(*encoded)
    }

    override fun emitStartArray() {
        require(state == State.INITIAL) {
            "startArray in state $state"
        }
        state = State.ARRAY0
    }

    override fun emitEndArray() {
        when (state) {
            State.ARRAY0 -> {
                //empty array
                target += "[]"
            }
            State.ARRAY -> {}
            else -> error("endArray in state $state")
        }
        state = State.DONE
    }

    override fun emitStartObject() {
        require(state == State.INITIAL) {
            "startArray in state $state"
        }
        state = State.OBJECT0
    }

    override fun emitEndObject() {
        when (state) {
            State.OBJECT0 -> {
                //empty object
                target += "{}"
            }
            State.OBJECT_KEY -> {}
            else -> error("endObject in state $state")
        }
        state = State.DONE
    }

    override fun <T> emit(serializer: KSerializer<T>, value: T) = emitEncoded(
        json.encodeToString(serializer, value)
    )

    fun close() {
        require(state == State.DONE) {
            "close in state $state"
        }
    }

}
