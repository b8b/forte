package org.cikit.forte.lib.common

import org.cikit.forte.core.FilterMethod
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.typeName

/**
 * jinja-filters.items(value: Mapping[K, V] | jinja2.runtime.Undefined) → Iterator[Tuple[K, V]]
 *
 *     Return an iterator over the (key, value) items of a mapping.
 *
 *     This implementation fails on Undefined. Use x|default({})|items to handle it.
 */
class FilterItems : FilterMethod {
    override fun invoke(subject: Any?, args: NamedArgs): Any {
        args.requireEmpty()
        require(subject is Map<*, *>) {
            "invalid operand of type '${typeName(subject)}'"
        }
        return DictItems(subject)
    }

    private class DictItems(val map: Map<*, *>) : Iterable<DictItem> {
        override fun iterator(): Iterator<DictItem> {
            return DictItemIterator(map.entries.iterator())
        }
    }

    private class DictItemIterator(
        val entryIterator: Iterator<Map.Entry<*, *>>
    ) : Iterator<DictItem> {
        override fun next(): DictItem {
            val (k, v) = entryIterator.next()
            return DictItem(k, v)
        }

        override fun hasNext(): Boolean {
            return entryIterator.hasNext()
        }
    }
}
