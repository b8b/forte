package org.cikit.forte.lib.common

import org.cikit.forte.core.*
import org.cikit.forte.lib.core.FilterGet

/**
 * jinja-filters.selectattr(value: 't.Iterable[V]', *args: Any, **kwargs: Any) → 't.Iterator[V]'
 *
 *     Filters a sequence of objects by applying a test to the specified attribute of each object,
 *     and only selecting the objects with the test succeeding.
 *
 *     If no test is specified, the attribute’s value will be evaluated as a boolean.
 *
 *     Example usage:
 *
 *     {{ users|selectattr("is_active") }}
 *     {{ users|selectattr("email", "none") }}
 *
 * jinja-filters.rejectattr(value: 't.Iterable[V]', *args: Any, **kwargs: Any) → 't.Iterator[V]'
 *
 *     Filters a sequence of objects by applying a test to the specified attribute of each object,
 *     and rejecting the objects with the test succeeding.
 *
 *     If no test is specified, the attribute’s value will be evaluated as a boolean.
 *
 *     {{ users|rejectattr("is_active") }}
 *     {{ users|rejectattr("email", "none") }}
 *
 */
class FilterSelectAttr private constructor(
    private val get: FilterMethod,
    val cond: Boolean
) : FilterMethod, DependencyAware {

    constructor(ctx: Context<*>, cond: Boolean) : this(ctx.filterGet, cond)

    override fun withDependencies(ctx: Context<*>): FilterSelectAttr {
        val get = ctx.filterGet
        return if (get === this.get) {
            this
        } else {
            FilterSelectAttr(get, cond)
        }
    }

    override fun invoke(subject: Any?, args: NamedArgs): Any {
        require(subject is Iterable<*>) {
            "invalid operand of type '${typeName(subject)}'"
        }
        return Suspended { ctx ->
            val getArgs: NamedArgs
            val test: Method
            val testArgs: NamedArgs
            args.use {
                getArgs = NamedArgs(
                    listOf(requireAny("attribute")),
                    FilterGet.singleArg
                )
                val name = optional<CharSequence>("test") { "true" }
                    .concatToString()
                val key = Context.Key.Apply.create(name, TestMethod.OPERATOR)
                test = ctx.getMethod(key)
                    ?: error("filter '$name' is not defined")
                testArgs = remaining()
            }
            val result = buildList {
                for (item in subject) {
                    try {
                        var mapped = get(item, getArgs)
                        if (mapped is Suspended) {
                            mapped = mapped.eval(ctx)
                        }
                        var selected: Any? = test(mapped, testArgs)
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
