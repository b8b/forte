import org.cikit.forte.core.StringConcatenation
import kotlin.test.*

class TestStringConcat {

    @Test
    fun testEmpty() {
        val x1 = StringConcatenation.concat("", "")
        val x2 = StringConcatenation.replicate("", 100)
        val x3 = StringConcatenation.replicate("hello", 0)
        assertEquals(StringConcatenation.Empty, x1)
        assertEquals(StringConcatenation.Empty, x2)
        assertEquals(StringConcatenation.Empty, x3)
    }

    @Test
    fun testConcatWithEmptyLeft() {
        val result = StringConcatenation.concat("", "hello")
        assertEquals("hello", result.toString())
        assertEquals(5, result.length)
    }

    @Test
    fun testConcatWithNonEmptyLeft() {
        val result = StringConcatenation.concat("hello", " world")
        assertEquals("hello world", result.toString())
        assertEquals(11, result.length)
    }

    @Test
    fun testConcatBothNonEmpty() {
        val result = StringConcatenation.concat("foo", "bar")
        assertEquals("foobar", result.toString())
    }

    @Test
    fun testReplicateRepeatZero() {
        val result = StringConcatenation.replicate("hello", 0)
        assertEquals(StringConcatenation.Empty, result)
    }

    @Test
    fun testReplicateRepeatNegative() {
        val result = StringConcatenation.replicate("hello", -5)
        assertEquals(StringConcatenation.Empty, result)
    }

    @Test
    fun testReplicateRepeatOne() {
        val result = StringConcatenation.replicate("hello", 1)
        assertEquals("hello", result.toString())
        assertEquals(5, result.length)
    }

    @Test
    fun testReplicateRepeatMultiple() {
        val result = StringConcatenation.replicate("ab", 3)
        assertEquals("ababab", result.toString())
        assertEquals(6, result.length)
    }

    @Test
    fun testReplicateEmptyValue() {
        val result = StringConcatenation.replicate("", 10)
        assertEquals(StringConcatenation.Empty, result)
    }

    @Test
    fun testEmptyLength() {
        assertEquals(0, StringConcatenation.Empty.length)
    }

    @Test
    fun testEmptyPlusEmpty() {
        val result = StringConcatenation.Empty + ""
        assertEquals(StringConcatenation.Empty, result)
    }

    @Test
    fun testEmptyPlusNonEmpty() {
        val result = StringConcatenation.Empty + "hello"
        assertEquals("hello", result.toString())
    }

    @Test
    fun testEmptyGetThrowsException() {
        assertFailsWith<IndexOutOfBoundsException> {
            StringConcatenation.Empty[0]
        }
    }

    @Test
    fun testEmptyAppendToAppendable() {
        val sb = StringBuilder()
        val result = StringConcatenation.Empty.appendTo(sb as Appendable)
        assertEquals(sb, result)
        assertEquals("", sb.toString())
    }

    @Test
    fun testEmptyAppendToStringBuilder() {
        val sb = StringBuilder()
        val result = StringConcatenation.Empty.appendTo(sb)
        assertEquals(sb, result)
        assertEquals("", sb.toString())
    }

    @Test
    fun testEmptyToString() {
        assertEquals("", StringConcatenation.Empty.toString())
    }

    @Test
    fun testSingleLength() {
        val single = StringConcatenation.concat("hello", "")
        assertEquals(5, single.length)
    }

    @Test
    fun testSinglePlusEmpty() {
        val single = StringConcatenation.concat("hello", "")
        val result = single + ""
        assertEquals(single, result)
    }

    @Test
    fun testSinglePlusNonEmpty() {
        val single = StringConcatenation.concat("hello", "")
        val result = single + " world"
        assertEquals("hello world", result.toString())
    }

    @Test
    fun testSingleGet() {
        val single = StringConcatenation.concat("hello", "")
        assertEquals('h', single[0])
        assertEquals('e', single[1])
        assertEquals('o', single[4])
    }

    @Test
    fun testSingleGetThrowsException() {
        assertFailsWith<IndexOutOfBoundsException> {
            StringConcatenation.concat("test", "")[100]
        }
    }

    @Test
    fun testSingleSubSequence() {
        val single = StringConcatenation.concat("hello", "")
        val sub = single.subSequence(1, 4)
        assertEquals("ell", sub.toString())
    }

    @Test
    fun testSingleAppendToWithString() {
        val single = StringConcatenation.concat("hello", "")
        val sb = StringBuilder()
        single.appendTo(sb)
        assertEquals("hello", sb.toString())
    }

    @Test
    fun testSingleAppendToWithInlineString() {
        val inner = StringConcatenation.concat("inner", "")
        val outer = inner + ""
        val sb = StringBuilder()
        outer.appendTo(sb)
        assertEquals("inner", sb.toString())
    }

    @Test
    fun testSingleToString() {
        val single = StringConcatenation.concat("hello", "")
        assertEquals("hello", single.toString())
    }

    @Test
    fun testLinkedLength() {
        val linked = StringConcatenation.concat("hello", " world")
        assertEquals(11, linked.length)
    }

    @Test
    fun testLinkedPlusEmpty() {
        val linked = StringConcatenation.concat("hello", " world")
        val result = linked + ""
        assertEquals(linked, result)
    }

    @Test
    fun testLinkedPlusNonEmpty() {
        val linked = StringConcatenation.concat("hello", " world")
        val result = linked + "!"
        assertEquals("hello world!", result.toString())
    }

    @Test
    fun testLinkedGetLeftSide() {
        val linked = StringConcatenation.concat("hello", " world")
        assertEquals('h', linked[0])
        assertEquals('o', linked[4])
    }

    @Test
    fun testLinkedGetRightSide() {
        val linked = StringConcatenation.concat("hello", " world")
        assertEquals(' ', linked[5])
        assertEquals('w', linked[6])
        assertEquals('d', linked[10])
    }

    @Test
    fun testLinkedAppendToWithString() {
        val linked = StringConcatenation.concat("hello", " world")
        val sb = StringBuilder()
        linked.appendTo(sb)
        assertEquals("hello world", sb.toString())
    }

    @Test
    fun testLinkedAppendToWithInlineString() {
        val inner = StringConcatenation.concat("inner", "")
        val linked = inner + " suffix"
        val sb = StringBuilder()
        linked.appendTo(sb)
        assertEquals("inner suffix", sb.toString())
    }

    @Test
    fun testReplicatedLength() {
        val replicated = StringConcatenation.replicate("ab", 3)
        assertEquals(6, replicated.length)
    }

    @Test
    fun testReplicatedPlusEmpty() {
        val replicated = StringConcatenation.replicate("ab", 3)
        val result = replicated + ""
        assertEquals(replicated, result)
    }

    @Test
    fun testReplicatedPlusNonEmpty() {
        val replicated = StringConcatenation.replicate("ab", 3)
        val result = replicated + "X"
        assertEquals("abababX", result.toString())
    }

    @Test
    fun testReplicatedGetValid() {
        val replicated = StringConcatenation.replicate("ab", 3)
        assertEquals('a', replicated[0])
        assertEquals('b', replicated[1])
        assertEquals('a', replicated[2])
        assertEquals('b', replicated[3])
        assertEquals('a', replicated[4])
        assertEquals('b', replicated[5])
    }

    @Test
    fun testReplicatedGetOutOfBounds() {
        val replicated = StringConcatenation.replicate("ab", 3)
        assertFailsWith<IllegalArgumentException> {
            replicated[6]
        }
    }

    @Test
    fun testReplicatedAppendToWithString() {
        val replicated = StringConcatenation.replicate("ab", 3)
        val sb = StringBuilder()
        replicated.appendTo(sb)
        assertEquals("ababab", sb.toString())
    }

    @Test
    fun testReplicatedAppendToWithInlineString() {
        val inner = StringConcatenation.concat("x", "")
        val replicated = StringConcatenation.replicate(inner, 3)
        val sb = StringBuilder()
        replicated.appendTo(sb)
        assertEquals("xxx", sb.toString())
    }

    @Test
    fun testLazyLength() {
        val lazy = StringConcatenation.concat("hello", " world") + "!"
        assertEquals(12, lazy.length)
    }

    @Test
    fun testLazyPlusEmpty() {
        val lazy = StringConcatenation.concat("hello", " world") + "!"
        val result = lazy + ""
        assertEquals(lazy, result)
    }

    @Test
    fun testLazyPlusNonEmpty() {
        val lazy = StringConcatenation.concat("hello", " world") + "!"
        val result = lazy + "?"
        assertEquals("hello world!?", result.toString())
    }

    @Test
    fun testLazySubSequence() {
        val lazy = StringConcatenation.concat("hello", " world") + "!"
        val sub = lazy.subSequence(6, 11)
        assertEquals("world", sub.toString())
    }

    @Test
    fun testLazyAppendToAppendable() {
        val lazy = StringConcatenation.concat("hello", " world") + "!"
        val sb = StringBuilder()
        lazy.appendTo(sb)
        assertEquals("hello world!", sb.toString())
    }

    @Test
    fun testLazyAppendToStringBuilder() {
        val lazy = StringConcatenation.concat("hello", " world") + "!"
        val sb = StringBuilder()
        lazy.appendTo(sb)
        assertEquals("hello world!", sb.toString())
    }

    @Test
    fun testLazyGet() {
        val lazy = StringConcatenation.concat("hello", " world") + "!"
        assertEquals('h', lazy[0])
        assertEquals(' ', lazy[5])
        assertEquals('!', lazy[11])
    }

    @Test
    fun testLazyToStringWithMemoization() {
        val lazy = StringConcatenation.concat("hello", " world") + "!"
        val str1 = lazy.toString()
        val str2 = lazy.toString()
        assertEquals("hello world!", str1)
        assertEquals("hello world!", str2)
    }

    @Test
    fun testLazyToStringMemoizesSingle() {
        val single = StringConcatenation.concat("test", "")
        val lazy = single + ""
        // First toString renders and memoizes
        val str1 = lazy.toString()
        assertEquals("test", str1)
        // Second toString should use memoized value
        val str2 = lazy.toString()
        assertEquals("test", str2)
    }

    @Test
    fun testEqualsIdentity() {
        val empty = StringConcatenation.Empty
        assertSame(empty, empty)
        assertEquals(empty, empty)
    }

    @Test
    fun testEqualsWithStringConcatenation() {
        val a = StringConcatenation.concat("hello", "")
        val b = StringConcatenation.concat("hello", "")
        assertEquals(a, b)
    }

    @Test
    fun testEqualsWithString() {
        val sc = StringConcatenation.concat("hello", "")
        assertEquals("hello", sc.toString())
    }

    @Test
    fun testEqualsWithDifferentString() {
        val sc = StringConcatenation.concat("hello", "")
        assertNotEquals("world", sc.toString())
    }

    @Test
    fun testHashCode() {
        val sc = StringConcatenation.concat("hello", "")
        assertEquals("hello".hashCode(), sc.hashCode())
    }

    @Test
    fun testComplexConcatenation() {
        val result = StringConcatenation.concat("a", "b") +
                StringConcatenation.concat("c", "d") + "e"
        assertEquals("abcde", result.toString())
    }

    @Test
    fun testNestedReplication() {
        val inner = StringConcatenation.replicate("x", 2)
        val result = inner + StringConcatenation.replicate("y", 3)
        assertEquals("xxyyy", result.toString())
    }

    @Test
    fun testMultipleChainedPlus() {
        val result = StringConcatenation.Empty + "a" + "b" + "c" + "d"
        assertEquals("abcd", result.toString())
    }

    @Test
    fun testEmptyConcatReturnsEmpty() {
        val result = StringConcatenation.concat("", "")
        assertEquals(StringConcatenation.Empty, result)
    }

    @Test
    fun testAppendToReturnsTarget() {
        val sb = StringBuilder()
        val result = StringConcatenation.concat("test", "").appendTo(sb)
        assertEquals(sb, result)
    }

    @Test
    fun testConcatWithRightEmpty() {
        val result = StringConcatenation.concat("hello", "")
        assertEquals("hello", result.toString())
    }

    @Test
    fun testReplicatedWithSingleChar() {
        val result = StringConcatenation.replicate("x", 5)
        assertEquals("xxxxx", result.toString())
        assertEquals(5, result.length)
    }
}
