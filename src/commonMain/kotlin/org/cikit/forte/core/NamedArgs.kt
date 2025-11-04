package org.cikit.forte.core

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

class NamedArgs(
    val values: List<Any?>,
    val names: List<String>
) {
    companion object {
        val Empty = NamedArgs(emptyList(), emptyList())
    }

    @OptIn(ExperimentalContracts::class)
    fun <T> use(block: NamedArgsIterator.() -> T): T {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        val args = NamedArgsIterator(values, names)
        val result = block(args)
        require(!args.hasRemaining()) { "too many args" }
        return result
    }

    fun requireEmpty() {
        require(values.isEmpty()) { "too many args" }
    }
}

class NamedArgsIterator(
    private val values: List<Any?>,
    private val names: List<String>
) {
    private val positionalCount = values.size - names.size
    private val consumed = BooleanArray(names.size)
    private var index = 0

    fun hasRemaining(): Boolean {
        return index < positionalCount || consumed.any { !it }
    }

    fun requireAny(name: String): Any? {
        val nextIndex = next(name)
        require(nextIndex >= 0) { "missing required arg '$name'" }
        return values[nextIndex]
    }

    fun <T : Any> requireNullable(name: String, type: KClass<T>): T? {
        val nextIndex = next(name)
        require(nextIndex >= 0) { "missing required arg '$name'" }
        return castNullable(name, type, values[nextIndex])
    }

    fun <T : Any> require(name: String, type: KClass<T>): T {
        val nextIndex = next(name)
        require(nextIndex >= 0) { "missing required arg '$name'" }
        return cast(name, type, values[nextIndex])
    }

    fun <T : Any> optionalNullable(
        name: String,
        type: KClass<T>,
        defaultValue: () -> T?,
    ): T? {
        val nextIndex = next(name)
        val finalValue = if (nextIndex < 0) {
            defaultValue()
        } else {
            cast(name, type, values[nextIndex])
        }
        return finalValue
    }

    fun <T : Any> optionalNullable(
        name: String,
        convertValue: (Any?) -> T?,
        defaultValue: () -> T?,
    ): T? {
        val nextIndex = next(name)
        return if (nextIndex < 0) {
            defaultValue()
        } else {
            convertValue(values[nextIndex])
        }
    }

    fun <T : Any> optional(
        name: String,
        type: KClass<T>,
        defaultValue: () -> T
    ): T {
        val nextIndex = next(name)
        return if (nextIndex < 0) {
            defaultValue()
        } else {
            cast(name, type, values[nextIndex])
        }
    }

    fun <T : Any> optional(
        name: String,
        convertValue: (Any?) -> T,
        defaultValue: () -> T
    ): T {
        val nextIndex = next(name)
        return if (nextIndex < 0) {
            defaultValue()
        } else {
            val value = values[nextIndex]
            convertValue(value)
        }
    }

    private fun <T: Any> cast(
        name: String,
        type: KClass<T>,
        value: Any?
    ): T {
        require(value != null) { "invalid null value for arg '$name'" }
        val result = type.safeCast(value)
        require(result != null) {
            "invalid type '${typeName(value)}' " +
                    "for arg '$name': expected '$type'"
        }
        return result
    }

    private fun <T: Any> castNullable(
        name: String,
        type: KClass<T>,
        value: Any?
    ): T? {
        if (value == null) {
            return null
        }
        val result = type.safeCast(value)
        require(result != null) {
            "invalid type '${typeName(value)}' " +
                    "for arg '$name': expected '$type'"
        }
        return result
    }

    private fun next(name: String): Int {
        val nameIndex = names.indexOf(name)
        if (index < positionalCount) {
            //positional
            require(nameIndex < 0) { "arg '${name}' already passed" }
            val result = index
            index++
            return result
        } else if (nameIndex < 0) {
            return -1
        } else {
            require(!consumed[nameIndex]) { "arg '${name} already passed" }
            consumed[nameIndex] = true
            return nameIndex + positionalCount
        }
    }
}

inline fun <reified T: Any> NamedArgsIterator.requireNullable(
    name: String
): T? {
    return requireNullable(name, T::class)
}

inline fun <reified T: Any> NamedArgsIterator.require(name: String): T {
    return require(name, T::class)
}

inline fun <reified T: Any> NamedArgsIterator.optional(
    name: String,
    noinline defaultValue: () -> T
) : T {
    return optional(name, T::class, defaultValue)
}
