package org.cikit.forte.core

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import org.cikit.forte.internal.parseInt

interface FilterMethod : Method {
    companion object {
        val OPERATOR = MethodOperator<FilterMethod>("pipe")
    }

    override val operator: String
        get() = OPERATOR.value
}

fun <R, T: FilterMethod> Context.Builder<R>.defineFilter(
    key: Context.Key.Apply<T>,
    implementation: T
): Context.Builder<R> {
    defineMethod(key, implementation)
    return this
}

fun <R> Context.Builder<R>.defineFilter(
    name: String,
    hidden: Boolean = false,
    rescue: Boolean = false,
    implementation: (subject: Any?, args: NamedArgs) -> Any?
): Context.Builder<R> = object : FilterMethod {
    override val isHidden: Boolean
        get() = hidden
    override val isRescue: Boolean
        get() = rescue
    override fun invoke(subject: Any?, args: NamedArgs): Any? {
        return implementation(subject, args)
    }
}.let { defineFilter(Context.Key.Apply(name, it.operator), it) }

fun <R, T: TestMethod> Context.Builder<R>.defineTest(
    key: Context.Key.Apply<T>,
    implementation: T
): Context.Builder<R> {
    defineMethod(key, implementation)
    return this
}

interface TestMethod : Method {
    companion object {
        val OPERATOR = MethodOperator<TestMethod>("is")
    }

    override val operator: String
        get() = OPERATOR.value

    override fun invoke(subject: Any?, args: NamedArgs): Any?
}

fun <R> Context.Builder<R>.defineTest(
    name: String,
    hidden: Boolean = false,
    rescue: Boolean = false,
    implementation: (subject: Any?, args: NamedArgs) -> Boolean
): Context.Builder<R> = object : TestMethod {
    override val isHidden: Boolean
        get() = hidden
    override val isRescue: Boolean
        get() = rescue
    override fun invoke(subject: Any?, args: NamedArgs): Boolean {
        return implementation(subject, args)
    }
}.let { defineTest(Context.Key.Apply(name, it.operator), it) }

suspend fun <T> Context.Builder<T>.loadJson(
    name: String,
    value: JsonElement
): Context.Builder<T> {
    setVar(name, convertJson(this, value))
    return this
}

private suspend fun convertJson(
    ctx: Context.Evaluator<*>,
    value: JsonElement
): Any? = when (value) {
    is JsonNull -> null
    is JsonPrimitive -> if (value.isString) {
        value.content
    } else {
        value.booleanOrNull ?: try {
            parseInt(value.content)
        } catch (_: NumberFormatException) {
            value.content.toDouble()
        }
    }
    is JsonArray -> {
        val result = ArrayList<Any?>(value.size)
        for (item in value) {
            result.add(convertJson(ctx, item))
        }
        result.toList()
    }
    is JsonObject -> {
        val result = LinkedHashMap<String, Any?>(value.size)
        for ((k, v) in value) {
            result[k] = convertJson(ctx, v)
        }
        val finalResult = ctx.filterDict(result.toMap(), NamedArgs.Empty)
        if (finalResult is Suspended) {
            finalResult.eval(ctx)
        } else {
            finalResult
        }
    }
}
