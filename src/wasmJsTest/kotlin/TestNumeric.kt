import kotlinx.coroutines.test.runTest
import org.cikit.forte.Forte
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.lib.core.IsNumberTest
import org.cikit.forte.lib.wasmjs.FloatNumericValue
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TestNumeric {

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
    fun testPowWithInfinity() {
        val result = 2.0.pow(Double.POSITIVE_INFINITY)
        assertEquals(Double.POSITIVE_INFINITY, result)
    }

    @Test
    fun testNaNIsNumber() {
        val nan = FloatNumericValue(Double.NaN)
        assertTrue(nan.isFloat, "isFloat")
        assertTrue(nan.doubleOrNull().isNaN(), "isNaN")
        val result = IsNumberTest(Forte.scope()).invoke(nan, NamedArgs.Empty)
        println(result)
    }

    @Test
    fun testParseIntError() = runTest {
        val expr = Forte.parseExpression("(2 ** 8000) is lt(Infinity)")
        val result = Forte.scope().evalExpression(expr)
        println(result)
    }
}