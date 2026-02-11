package org.cikit.forte.core

sealed class StringConcatenation : InlineString {

    companion object {
        fun concat(
            left: CharSequence,
            right: CharSequence
        ) : StringConcatenation {
            return if (left.isEmpty()) {
                Empty + right
            } else {
                Single(left) + right
            }
        }

        fun replicate(value: CharSequence, repeat: Int): StringConcatenation {
            return if (repeat <= 0 || value.isEmpty()) {
                Empty
            } else if (repeat == 1) {
                Single(value)
            } else {
                Replicated(value, repeat)
            }
        }
    }

    object Empty : StringConcatenation() {
        override val length: Int
            get() = 0

        override operator fun plus(other: CharSequence): StringConcatenation {
            return if (other.isEmpty()) {
                this
            } else {
                Single(other)
            }
        }

        override fun get(index: Int): Char {
            throw IndexOutOfBoundsException(
                "index $index out of bounds for length: 0"
            )
        }

        override fun appendTo(target: Appendable): Appendable {
            return target
        }

        override fun appendTo(stringBuilder: StringBuilder): StringBuilder {
            return stringBuilder
        }

        override fun toString(): String {
            return ""
        }
    }

    private class Single(
        private val value: CharSequence
    ) : StringConcatenation() {
        override val length: Int
            get() = value.length

        override fun plus(other: CharSequence): StringConcatenation {
            return when {
                other.isEmpty() -> this
                other is Lazy -> Lazy(Linked(this, other.delegate))
                else -> Lazy(Linked(this, other))
            }
        }

        override fun get(index: Int): Char {
            if (index !in 0 until length) {
                throw IndexOutOfBoundsException(
                    "index $index out of bounds for length: $length"
                )
            }
            return value[index]
        }

        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
            return value.subSequence(startIndex, endIndex)
        }

        override fun appendTo(target: Appendable): Appendable {
            if (value is InlineString) {
                value.appendTo(target)
            } else {
                target.append(value)
            }
            return target
        }

        override fun appendTo(stringBuilder: StringBuilder): StringBuilder {
            if (value is InlineString) {
                value.appendTo(stringBuilder)
            } else {
                stringBuilder.append(value)
            }
            return stringBuilder
        }

        override fun toString(): String {
            return value.concatToString()
        }
    }

    private class Linked(
        private val left: StringConcatenation,
        private val value: CharSequence
    ) : StringConcatenation() {
        private val offset: Int = left.length

        override val length: Int
            get() = offset + value.length

        override fun plus(other: CharSequence): StringConcatenation {
            return when {
                other.isEmpty() -> this
                other is Lazy -> Lazy(Linked(this, other.delegate))
                else -> Lazy(Linked(this, other))
            }
        }

        override fun get(index: Int): Char {
            if (index in offset until length) {
                return value[index - offset]
            }
            if (index < offset) {
                return left[index]
            }
            throw IndexOutOfBoundsException(
                "index $index out of bounds for length: $length"
            )
        }

        override fun appendTo(target: Appendable): Appendable {
            left.appendTo(target)
            val currentValue = value
            if (currentValue is InlineString) {
                currentValue.appendTo(target)
            } else {
                target.append(currentValue)
            }
            return target
        }

        override fun appendTo(stringBuilder: StringBuilder): StringBuilder {
            left.appendTo(stringBuilder)
            val currentValue = value
            if (currentValue is InlineString) {
                currentValue.appendTo(stringBuilder)
            } else {
                stringBuilder.append(currentValue)
            }
            return stringBuilder
        }
    }

    private class Replicated(
        private val value: CharSequence,
        private val repeat: Int
    ) : StringConcatenation() {
        override val length: Int
            get() = repeat * value.length

        override fun plus(other: CharSequence): StringConcatenation {
            return when {
                other.isEmpty() -> this
                other is Lazy -> Lazy(Linked(this, other.delegate))
                else -> Lazy(Linked(this, other))
            }
        }

        override fun get(index: Int): Char {
            require(index in 0 until length) {
                "index $index out of bounds for length: $length"
            }
            return value[index % value.length]
        }

        override fun appendTo(target: Appendable): Appendable {
            if (value is InlineString) {
                repeat(repeat) {
                    value.appendTo(target)
                }
            } else {
                repeat(repeat) {
                    target.append(value)
                }
            }
            return target
        }

        override fun appendTo(stringBuilder: StringBuilder): StringBuilder {
            if (value is InlineString) {
                repeat(repeat) {
                    value.appendTo(stringBuilder)
                }
            } else {
                repeat(repeat) {
                    stringBuilder.append(value)
                }
            }
            return stringBuilder
        }
    }

    private class Lazy(
        var delegate: StringConcatenation
    ) : StringConcatenation() {
        override val length: Int
            get() = delegate.length

        override fun plus(other: CharSequence): StringConcatenation {
            return when {
                other.isEmpty() -> this
                else -> Lazy(delegate + other)
            }
        }

        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
            return delegate.subSequence(startIndex, endIndex)
        }

        override fun appendTo(target: Appendable): Appendable {
            return delegate.appendTo(target)
        }

        override fun appendTo(stringBuilder: StringBuilder): StringBuilder {
            return delegate.appendTo(stringBuilder)
        }

        override fun get(index: Int): Char {
            return delegate[index]
        }

        override fun toString(): String {
            val currentDelegate = delegate
            if (currentDelegate is Single) {
                return currentDelegate.toString()
            }
            val rendered = currentDelegate.toString()
            delegate = Single(rendered)
            return rendered
        }
    }

    abstract operator fun plus(other: CharSequence): StringConcatenation

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        return toString().subSequence(startIndex, endIndex)
    }

    override fun toString(): String {
        return buildString {
            appendTo(this)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is StringConcatenation) {
            return this.toString() == other.toString()
        }
        return toString() == other
    }

    override fun hashCode(): Int {
        return toString().hashCode()
    }
}
