package org.cikit.forte.lib.common

import org.cikit.forte.core.*

/**
 * jinja-filters.select(value: 't.Iterable[V]', *args: Any, **kwargs: Any) → 't.Iterator[V]'
 *
 *     Filters a sequence of objects by applying a test to each object,
 *     and only selecting the objects with the test succeeding.
 *
 *     If no test is specified, each object will be evaluated as a boolean.
 *
 *     Example usage:
 *
 *     {{ numbers|select("odd") }}
 *     {{ numbers|select("odd") }}
 *     {{ numbers|select("divisibleby", 3) }}
 *     {{ numbers|select("lessthan", 42) }}
 *     {{ strings|select("equalto", "mystring") }}
 *
 * jinja-filters.reject(value: 't.Iterable[V]', *args: Any, **kwargs: Any) → 't.Iterator[V]'
 *
 *     Filters a sequence of objects by applying a test to each object,
 *     and rejecting the objects with the test succeeding.
 *
 *     If no test is specified, each object will be evaluated as a boolean.
 *
 *     Example usage:
 *
 *     {{ numbers|reject("odd") }}
 *
 */
class FilterSelect(val cond: Boolean) : FilterMethod {
    override fun invoke(subject: Any?, args: NamedArgs): Any {
        require(subject is Iterable<*>) {
            "invalid operand of type '${typeName(subject)}'"
        }
        return Suspended { ctx ->
            val test: Method
            val testArgs: NamedArgs
            args.use {
                val name = optional<CharSequence>("test") { "true" }
                    .concatToString()
                val key = Context.Key.Apply.create(name, TestMethod.OPERATOR)
                test = ctx.getMethod(key)
                    ?: error("test '$name' is not defined")
                testArgs = remaining()
            }
            val result = buildList {
                for (item in subject) {
                    try {
                        var selected = test(item, testArgs)
                        if (selected is Suspended) {
                            selected = selected.eval(ctx)
                        }
                        if (selected == cond) {
                            add(item)
                        }
                    } catch (ex: EvalException) {
                        throw RuntimeException(ex.errorMessage, ex)
                    }
                }
            }
            MaskedList(result)
        }
    }
}
