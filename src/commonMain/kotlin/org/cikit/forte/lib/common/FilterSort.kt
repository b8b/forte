package org.cikit.forte.lib.common

import org.cikit.forte.core.*
import org.cikit.forte.lib.core.FilterComparable
import org.cikit.forte.lib.core.FilterGet
import org.cikit.forte.lib.core.filterComparable

/**
 * jinja-filters.sort(value: 't.Iterable[V]', reverse: bool = False, case_sensitive: bool = False, attribute: str | int | NoneType = None) → 't.List[V]'
 *
 *     Sort an iterable.
 *
 *     {% for city in cities|sort %}
 *         ...
 *     {% endfor %}
 *
 *     Parameters:
 *             reverse – Sort descending instead of ascending.
 *             case_sensitive – When sorting strings, sort upper and lower case separately.
 *             attribute – When sorting objects or dicts, an attribute or key to sort by.
 *
 *     The sort is stable, it does not change the relative order of elements that compare equal.
 *     This makes it is possible to chain sorts on different attributes and ordering.
 *
 *     {% for user in users|sort(attribute="name")
 *         |sort(reverse=true, attribute="age") %}
 *         ...
 *     {% endfor %}
 *
 * jinja-filters.unique(value: 't.Iterable[V]', case_sensitive: bool = False, attribute: str | int | NoneType = None) → 't.Iterator[V]'
 *
 *     Returns a list of unique items from the given iterable.
 *
 *     {{ ['foo', 'bar', 'foobar', 'FooBar']|unique|list }}
 *         -> ['foo', 'bar', 'foobar']
 *
 *     The unique items are yielded in the same order as their first occurrence in the iterable passed to the filter.
 *
 *     Parameters:
 *             case_sensitive – Treat upper and lower case strings as distinct.
 *             attribute – Filter objects with unique values for this attribute.
 */
class FilterSort private constructor(
    private val comparable: FilterComparable,
    val unique: Boolean
) : FilterMethod, DependencyAware {

    constructor(ctx: Context<*>, unique: Boolean) : this(
        ctx.filterComparable,
        unique
    )

    override fun withDependencies(ctx: Context<*>): FilterSort {
        val comparable = ctx.filterComparable
        return if (comparable === this.comparable) {
            this
        } else {
            FilterSort(comparable, unique)
        }
    }

    override fun invoke(subject: Any?, args: NamedArgs): Any {
        val getArgs: NamedArgs?
        val reverse: Boolean
        val caseSensitive: Boolean
        args.use {
            reverse = if (unique) {
                false
            } else {
                optional("reverse") { false }
            }
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
            return subject
        }
        var item = itemIt.next()
        if (!itemIt.hasNext()) {
            return subject
        }
        return Suspended { ctx ->
            val sortedList = buildList {
                while (true) {
                    var key: Any?
                    if (getArgs != null) {
                        val result = ctx.filterGet(item, getArgs)
                        key = if (result is Suspended) {
                            result.eval(ctx)
                        } else {
                            result
                        }
                        if (key is Undefined) {
                            return@Suspended key
                        }
                    } else {
                        key = item
                    }
                    add(comparable(key, item, ignoreCase = !caseSensitive))
                    if (!itemIt.hasNext()) {
                        break
                    }
                    item = itemIt.next()
                }
                if (reverse) {
                    sortDescending()
                } else {
                    sort()
                }
                if (!unique) {
                    return@Suspended map { it.value }
                }
            }
            val result = buildList {
                add(sortedList[0].value)
                for (i in 1 until sortedList.size) {
                    if (sortedList[i].compareTo(sortedList[i - 1]) != 0) {
                        add(sortedList[i].value)
                    }
                }
            }
            Iterable { result.iterator() }
        }
    }
}
