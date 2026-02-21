import kotlinx.coroutines.test.runTest
import org.cikit.forte.Forte
import org.cikit.forte.lib.js.BigNumericValue
import kotlin.test.Test
import kotlin.test.assertEquals

class TestBigCompare {

    @Test
    fun testPow() = runTest {
        val expr = Forte.parseExpression("""(2|int) ** (40|int) + 1""")
        val result = Forte.scope().evalExpression(expr)
        val expect = js("BigInt('1099511627777')")
        assertEquals(expect, result)
    }

    @Test
    fun testBigCompare() = runTest {
        val result = Forte.scope()
            .setVar("a", 1)
            .setVar("b", 65476534743654364L)
            .evalExpression(Forte.parseExpression("a < b"))
        assertEquals(true, result)
    }
}
