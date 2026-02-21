import kotlinx.coroutines.test.runTest
import org.cikit.forte.Forte
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

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
    }

    @Test
    fun testBigIntIsNumber() {
        // only works with -Xes-long-as-bigint
        val x: Any? = js("BigInt('109951167658564856476547547627777')")
        assertTrue(checkIsNumber(x))
    }

    private fun checkIsNumber(v: Any?): Boolean {
        return v is Number
    }

    @Test
    fun testNegativeZero() {
        val x = -0.0
        val y = 0.0
        assertNotEquals(x, y)
        assertEquals("0", callNumberToString(x))
        assertEquals("0", callNumberToString(y))
    }

    private fun callNumberToString(value: Number) = value.toString()

    @Test
    fun testSlice() = runTest {

        val expr = Forte.parseExpression("-1")
        println(expr)
        val result = Forte.scope().evalExpression(expr)
        println(result)
    }
}
