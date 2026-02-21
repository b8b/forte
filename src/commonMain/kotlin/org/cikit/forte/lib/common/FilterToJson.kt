package org.cikit.forte.lib.common

import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import org.cikit.forte.core.*
import org.cikit.forte.lib.core.FilterNumber
import org.cikit.forte.lib.core.filterNumber

/**
 * jinja-filters.tojson(value: Any, indent: int | None = None) → markupsafe.Markup
 *
 *     Serialize an object to a string of JSON.
 *
 *     Parameters:
 *             value – The object to serialize to JSON.
 *             indent – The indent parameter passed to dumps, for pretty-printing the value.
 */
class FilterToJson private constructor(
    private val number: FilterNumber
): FilterMethod, DependencyAware {
    constructor(ctx: Context<*>) : this(ctx.filterNumber)

    override fun withDependencies(ctx: Context<*>): DependencyAware {
        val number = ctx.filterNumber
        if (number === this.number) {
            return this
        }
        return FilterToJson(number)
    }

    override fun invoke(subject: Any?, args: NamedArgs): Any {
        val indent: Int
        args.use {
            indent = optional(
                "indent",
                convertValue = { v ->
                    number(v).toIntOrNull() ?: error(
                        "cannot convert arg 'indent' to int"
                    )
                },
                defaultValue = { -1 }
            )
        }
        return Suspended { ctx ->
            val result = toJsonElement(ctx, subject)
            if (indent > 0) {
                @OptIn(ExperimentalSerializationApi::class)
                val json = Json {
                    prettyPrint = true
                    prettyPrintIndent = " ".repeat(indent)
                }
                json.encodeToString(result)
            } else {
                Json.encodeToString(result)
            }
        }
    }

    private suspend fun toJsonElement(
        ctx: Context.Evaluator<*>,
        value: Any?
    ): JsonElement = when (value) {
        null -> JsonNull
        is JsonElement -> value
        is Boolean -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
        is CharSequence -> JsonPrimitive(value.concatToString())
        is ByteString -> buildJsonArray {
            for (i in 0 until value.size) {
                add(JsonPrimitive(value[i]))
            }
        }
        is ByteArray -> buildJsonArray {
            for (b in value) {
                add(JsonPrimitive(b))
            }
        }
        is Map<*, *> -> buildJsonObject {
            for ((k, v) in value) {
                var key = ctx.filterString(k, NamedArgs.Empty)
                if (key is Suspended) {
                    key = key.eval(ctx)
                }
                if (key is Undefined) {
                    error(key.message)
                }
                put(key.toString(), toJsonElement(ctx, v))
            }
        }
        is Iterable<*> -> buildJsonArray {
            for (item in value) {
                add(toJsonElement(ctx, item))
            }
        }
        else -> error(
            "cannot convert value of type '${typeName(value)}' to json"
        )
    }
}
