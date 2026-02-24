import org.cikit.forte.lib.js.BigInt
import org.cikit.forte.lib.js.JsFilterNumber
import org.cikit.forte.lib.js.numberToString
import kotlin.test.*

class TestIsNumber {

    class MyNumber : Number() {
        override fun toDouble(): Double {
            TODO("Not yet implemented")
        }

        override fun toFloat(): Float {
            TODO("Not yet implemented")
        }

        override fun toLong(): Long {
            TODO("Not yet implemented")
        }

        override fun toInt(): Int {
            TODO("Not yet implemented")
        }

        override fun toShort(): Short {
            TODO("Not yet implemented")
        }

        override fun toByte(): Byte {
            TODO("Not yet implemented")
        }
    }

    @Test
    fun testNumberIsNotNumber() {
        val x = MyNumber()
        // as of kotlin-2.3.0 Number subclasses seem impossible on js
        assertFalse(x is Number)
    }

    @Test
    fun testJsNumberIsNumber() {
        var x: Any? = 1
        assertTrue(checkIsNumber(x))
        x = 1.1
        assertTrue(checkIsNumber(x))
    }

    @Test
    fun testLongIsNumber() {
        // only works with -Xes-long-as-bigint
        val x: Any = 100L
        assertTrue(checkIsNumber(x))
        assertTrue(checkIsLong(x))
    }

    @Test
    fun testBigIntIsNumber() {
        // only works with -Xes-long-as-bigint
        val x: Any? = js("BigInt('109951167658564856476547547627777')")
        assertTrue(checkIsNumber(x))
        assertTrue(checkIsLong(x))
    }

    private fun checkIsNumber(v: Any?): Boolean {
        return v is Number
    }

    private fun checkIsLong(v: Any?): Boolean {
        return v is Long
    }

    @Test
    fun testNegativeZero() {
        val x = -0.0
        val y = 0.0
        assertNotEquals(x, y)
        assertEquals("0", callNumberToString(x))
        assertEquals("-0.0", numberToString(x))
        assertEquals("0", callNumberToString(y))
        assertEquals("0.0", numberToString(y))
    }

    private fun callNumberToString(value: Number) = value.toString()

    @Test
    fun testRequireInt() {
        val number = JsFilterNumber()
        assertEquals(18, number.requireInt(18))
        assertEquals(18, number.requireInt(BigInt(18)))
        assertFails { number.requireInt(18.8) }
    }

    @Test
    fun testParseDouble() {
        assertFailsWith<NumberFormatException> { "".toDouble() }
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
    fun testParser() {
        assertTrue(Regex("""(?:0|[1-9]\d*)(?:\.\d+)?(?:[Ee][+-]?\d+)?""").matches("1e300"))
    }
}
