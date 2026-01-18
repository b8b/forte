package org.cikit.forte.lib.common

import org.cikit.forte.core.*
import org.cikit.forte.lib.core.FilterComparable
import org.cikit.forte.lib.core.FilterGet
import org.cikit.forte.lib.core.filterComparable

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
            reverse = optional("reverse") { false }
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
            buildList {
                add(sortedList[0].value)
                for (i in 1 until sortedList.size) {
                    if (sortedList[i].compareTo(sortedList[i - 1]) != 0) {
                        add(sortedList[i].value)
                    }
                }
            }
        }
    }
}
