import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.sqrt
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class TestNumeric {

    @Test
    fun testParseDouble() {
        assertFailsWith<NumberFormatException> { "".toDouble() }
        assertFailsWith<NumberFormatException> { ".".toDouble() }
        assertFailsWith<NumberFormatException> { "1+".toDouble() }
        assertFailsWith<NumberFormatException> { "1e".toDouble() }
        assertFailsWith<NumberFormatException> { "NaNu".toDouble() }
        assertFailsWith<NumberFormatException> { "InfinityAndBeyond".toDouble() }
        assertEquals(Double.NaN, "NaN".toDouble())
        assertEquals(Double.NEGATIVE_INFINITY, "-Infinity".toDouble())
        assertEquals(Double.POSITIVE_INFINITY, "+Infinity".toDouble())
        assertEquals(Double.POSITIVE_INFINITY, "Infinity".toDouble())
    }

    @Test
    fun testNaN() {
        val n: Double = sqrt(-1.0)
        val i: Double = 1.0 / 0.0

        assertTrue(n.isNaN())
        assertEquals(Double.POSITIVE_INFINITY, i)

        // comparators are ordering NaN above everything
        assertEquals(0, n.compareTo(n), "NaN compares equally")
        assertEquals(1, n.compareTo(i), "NaN > +Infinity")

        // however, primitive operators implement ieee unordered for NaN
        assertFalse(n == n)
        assertFalse(n >= n)
        assertFalse(n > n)
        assertFalse(n <= n)
        assertFalse(n < n)

        assertEquals(n, n + 1.0)
    }
}
