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
        require(this === Empty || values.isEmpty()) { "too many args" }
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

    fun remaining(): NamedArgs {
        val remainingNames = mutableListOf<String>()
        val remainingValues = buildList {
            while (index < positionalCount) {
                add(values[index++])
            }
            for (nameIndex in 0 until names.size) {
                val name = names[nameIndex]
                if (!consumed[nameIndex]) {
                    consumed[nameIndex] = true
                    add(values[positionalCount + nameIndex])
                    remainingNames.add(name)
                }
            }
        }
        if (remainingValues.isEmpty()) {
            return NamedArgs.Empty
        }
        return NamedArgs(
            values = remainingValues,
            names = remainingNames.toList()
        )
    }

    fun requireAny(name: String): Any? {
        val nextIndex = next(name)
        require(nextIndex >= 0) { "missing required arg '$name'" }
        return values[nextIndex]
    }

    fun <T: Any> requireNullable(name: String, convertValue: (Any?) -> T?): T? {
        val value = requireAny(name)
        try {
            return convertValue(value)
        } catch (ex: Throwable) {
            throw IllegalArgumentException(
                "cannot convert arg '$name' of type '${typeName(value)}': $ex",
                ex
            )
        }
    }

    fun <T: Any> require(name: String, convertValue: (Any?) -> T): T {
        val value = requireAny(name)
        require(value != null) {
            "invalid null value for arg '$name'"
        }
        try {
            return convertValue(value)
        } catch (ex: Throwable) {
            throw IllegalArgumentException(
                "cannot convert arg '$name' of type '${typeName(value)}': $ex",
                ex
            )
        }
    }

    fun <T : Any> optionalNullable(
        name: String,
        convertValue: (Any?) -> T?,
        defaultValue: () -> T?,
    ): T? {
        val nextIndex = next(name)
        if (nextIndex < 0) {
            return defaultValue()
        }
        val value = values[nextIndex]
        try {
            return convertValue(value)
        } catch (ex: Throwable) {
            throw IllegalArgumentException(
                "cannot convert arg '$name' of type '${typeName(value)}': $ex",
                ex
            )
        }
    }

    fun <T : Any> optional(
        name: String,
        convertValue: (Any?) -> T,
        defaultValue: () -> T
    ): T {
        val nextIndex = next(name)
        if (nextIndex < 0) {
            return defaultValue()
        }
        val value = values[nextIndex]
        try {
            return convertValue(value)
        } catch (ex: Throwable) {
            throw IllegalArgumentException(
                "cannot convert arg '$name' of type '${typeName(value)}': $ex",
                ex
            )
        }
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
    val value = requireAny(name) ?: return null
    require(value is T) {
        "invalid type '${typeName(value)}' " +
                "for arg '$name': expected '${T::class}'"
    }
    return value
}

inline fun <reified T: Any> NamedArgsIterator.require(name: String): T {
    val value = requireAny(name)
    require(value != null) {
        "invalid null value for arg '$name'"
    }
    require(value is T) {
        "invalid type '${typeName(value)}' " +
                "for arg '$name': expected '${T::class}'"
    }
    return value
}

inline fun <reified T: Any> NamedArgsIterator.optional(
    name: String,
    noinline defaultValue: () -> T
) : T {
    return optional(
        name = name,
        convertValue = { value ->
            require(value != null) {
                "invalid null value for arg '$name'"
            }
            require(value is T) {
                "invalid type '${typeName(value)}' " +
                        "for arg '$name': expected '${T::class}'"
            }
            value
        },
        defaultValue = defaultValue
    )
}
