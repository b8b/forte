package org.cikit.forte.emitter

import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encode
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

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
            else -> emit(ByteString(value))
        }
    }

    override fun emitScalar(value: ByteString) {
        @OptIn(ExperimentalEncodingApi::class)
        when (byteStringEncoding) {
            ByteStringEncoding.ARRAY -> emitArray {
                for (i in 0 until value.size) {
                    emitScalar(value[i])
                }
            }
            ByteStringEncoding.BASE64 -> emit(Base64.Mime.encode(value))
            ByteStringEncoding.YAML -> {
                val encoded = buildString {
                    append("!!binary ")
                    append(Json.encodeToString(Base64.Mime.encode(value)))
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