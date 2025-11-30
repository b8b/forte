package org.cikit.forte.types

interface InlineString : CharSequence

class RawString(val value: String) : CharSequence by value, InlineString {
    override fun equals(other: Any?): Boolean {
        if (other is RawString) {
            return value == other.value
        }
        return value == other
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return value
    }
}
