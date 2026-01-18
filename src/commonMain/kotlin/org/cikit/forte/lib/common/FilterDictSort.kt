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
                val dictEntry = DictEntry(k, v)
                val sortKey = when (by) {
                    DictSortBy.KEY -> k
                    DictSortBy.VALUE -> v
                }
                this += comparable(
                    sortKey,
                    dictEntry,
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

    private class DictEntry(
        val key: Any?,
        val value: Any?
    ) : List<Any?> {
        override val size: Int
            get() = 2

        override fun isEmpty() = false

        override fun contains(element: Any?) = element == key || element == value

        override fun iterator(): Iterator<Any?> = listOf(key, value).iterator()

        override fun containsAll(elements: Collection<Any?>) =
            elements.all(::contains)

        override fun get(index: Int) = when (index) {
            0 -> key
            1 -> value
            else -> throw IllegalArgumentException("index out of bounds: $index")
        }

        override fun indexOf(element: Any?) = when (element) {
            key -> 0
            value -> 1
            else -> -1
        }

        override fun lastIndexOf(element: Any?) = when (element) {
            value -> 1
            key -> 0
            else -> -1
        }

        override fun listIterator() = listOf(key, value).listIterator()

        override fun listIterator(index: Int) = listOf(key, value)
            .listIterator(index)

        override fun subList(fromIndex: Int, toIndex: Int) = listOf(key, value)
            .subList(fromIndex, toIndex)
    }
}
