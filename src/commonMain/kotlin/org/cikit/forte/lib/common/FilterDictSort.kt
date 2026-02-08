package org.cikit.forte.lib.common

import org.cikit.forte.core.Context
import org.cikit.forte.core.DependencyAware
import org.cikit.forte.core.FilterMethod
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.concatToString
import org.cikit.forte.core.optional
import org.cikit.forte.core.typeName
import org.cikit.forte.lib.core.FilterComparable
import org.cikit.forte.lib.core.filterComparable

/**
 * jinja-filters.dictsort(value: Mapping[K, V], case_sensitive: bool = False, by: 'te.Literal["key", "value"]' = 'key', reverse: bool = False) → List[Tuple[K, V]]
 *
 *     Sort (key, value) items of a mapping.
 *
 *     {% for key, value in mydict|dictsort %}
 *         sort the dict by key, case insensitive
 *
 *     {% for key, value in mydict|dictsort(reverse=true) %}
 *         sort the dict by key, case insensitive, reverse order
 *
 *     {% for key, value in mydict|dictsort(true) %}
 *         sort the dict by key, case sensitive
 *
 *     {% for key, value in mydict|dictsort(false, 'value') %}
 *         sort the dict by value, case insensitive
 */
class FilterDictSort private constructor(
    private val comparable: FilterComparable
) : FilterMethod, DependencyAware {

    constructor(ctx: Context<*>) : this(ctx.filterComparable)

    override fun withDependencies(ctx: Context<*>): FilterDictSort {
        val comparable = ctx.filterComparable
        return if (comparable === this.comparable) {
            this
        } else {
            FilterDictSort(comparable)
        }
    }

    override fun invoke(subject: Any?, args: NamedArgs): Any {
        val caseSensitive: Boolean
        val by: DictSortBy
        val reverse: Boolean
        args.use {
            caseSensitive = optional("case_sensitive") { false }
            by = optional(
                "by",
                { value ->
                    value as CharSequence
                    DictSortBy.valueOf(
                        value.concatToString()
                            .trim()
                            .uppercase()
                    )
                },
                { DictSortBy.KEY }
            )
            reverse = optional("reverse") { false }
        }
        require(subject is Map<*, *>) {
            "invalid operand of type '${typeName(subject)}'"
        }
        buildList(subject.size) {
            for ((k, v) in subject.entries) {
                val dictItem = DictItem(k, v)
                val sortKey = when (by) {
                    DictSortBy.KEY -> k
                    DictSortBy.VALUE -> v
                }
                this += comparable(
                    sortKey,
                    dictItem,
                    ignoreCase = !caseSensitive
                )
            }
            if (reverse) {
                sortDescending()
            } else {
                sort()
            }
            return map { it.value }
        }
    }
}
