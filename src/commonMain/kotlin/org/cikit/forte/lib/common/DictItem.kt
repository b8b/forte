package org.cikit.forte.lib.common

class DictItem(
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
