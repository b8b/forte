package org.cikit.forte.emitter

import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encode
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

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
        emitter.emitScalar(ByteString(value))
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
        if (value.endsWith("\n") && !value.first().isWhitespace()) {
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

    override fun emitScalar(value: ByteArray) = emitScalar(ByteString(value))

    override fun emitScalar(value: ByteString) {
        @OptIn(ExperimentalEncodingApi::class)
        val base64 = Base64.Mime.encode(value).split('\n')
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
