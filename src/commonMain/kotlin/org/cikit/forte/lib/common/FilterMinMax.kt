package org.cikit.forte.lib.common

import org.cikit.forte.core.*
import org.cikit.forte.lib.core.FilterComparable
import org.cikit.forte.lib.core.FilterGet
import org.cikit.forte.lib.core.filterComparable

/**
 * jinja-filters.max(value: 't.Iterable[V]', case_sensitive: bool = False, attribute: str | int | NoneType = None) → 't.Union[V, Undefined]'
 *
 *     Return the largest item from the sequence.
 *
 *     {{ [1, 2, 3]|max }}
 *         -> 3
 *
 *     Parameters:
 *
 *             case_sensitive – Treat upper and lower case strings as distinct.
 *
 *             attribute – Get the object with the max value of this attribute.
 *
 * jinja-filters.min(value: 't.Iterable[V]', case_sensitive: bool = False, attribute: str | int | NoneType = None) → 't.Union[V, Undefined]'
 *
 *     Return the smallest item from the sequence.
 *
 *     {{ [1, 2, 3]|min }}
 *         -> 1
 *
 *     Parameters:
 *
 *             case_sensitive – Treat upper and lower case strings as distinct.
 *
 *             attribute – Get the object with the min value of this attribute.
 *
 */
class FilterMinMax private constructor(
    private val comparable: FilterComparable,
    private val get: FilterMethod,
    val min: Boolean
) : FilterMethod, DependencyAware {

    constructor(ctx: Context<*>, min: Boolean) : this(
        ctx.filterComparable,
        ctx.filterGet,
        min = min
    )

    override fun withDependencies(ctx: Context<*>): FilterMinMax {
        val comparable = ctx.filterComparable
        val get = ctx.filterGet
        return if (comparable === this.comparable && get === this.get) {
            this
        } else {
            FilterMinMax(comparable, get, min)
        }
    }

    override fun invoke(subject: Any?, args: NamedArgs): Any {
        val getArgs: NamedArgs?
        val caseSensitive: Boolean
        args.use {
            caseSensitive = optional("case_sensitive") { false }
            getArgs = optionalNullable(
                "attribute",
                { attribute ->
                    NamedArgs(listOf(attribute), FilterGet.singleArg)
                },
                { null }
            )
        }
        if (subject !is Iterable<*>) {
            throw IllegalArgumentException(
                "invalid operand of type '${typeName(subject)}'"
            )
        }
        val itemIt = subject.iterator()
        if (!itemIt.hasNext()) {
            return Undefined("sequence is empty")
        }
        return Suspended { ctx ->
            var item0 = itemKey(ctx, itemIt.next(), getArgs, caseSensitive)
            if (!itemIt.hasNext()) {
                return@Suspended item0.value
            }
            while (true) {
                val item1 = itemKey(ctx, itemIt.next(), getArgs, caseSensitive)
                val cmp = item0.compareTo(item1)
                if (cmp < 0 && !min) {
                    item0 = item1
                } else if (cmp > 0 && min) {
                    item0 = item1
                }
                if (!itemIt.hasNext()) {
                    break
                }
            }
            item0.value
        }
    }

    private suspend fun itemKey(
        ctx: Context.Evaluator<*>,
        item: Any?,
        getArgs: NamedArgs?,
        caseSensitive: Boolean
    ): ComparableValue {
        var key: Any?
        if (getArgs != null) {
            val result = get(item, getArgs)
            key = if (result is Suspended) {
                result.eval(ctx)
            } else {
                result
            }
            if (key is Undefined) {
                error(key.message)
            }
        } else {
            key = item
        }
        return comparable(key, item, ignoreCase = !caseSensitive)
    }
}
