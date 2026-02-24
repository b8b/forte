import kotlinx.coroutines.test.runTest
import org.cikit.forte.Forte
import org.cikit.forte.lib.core.filterNumber
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class TestBigCompare {

    @Test
    fun testPow() = runTest {
        val expr = Forte.parseExpression("""(2|int) ** (40|int) + 1""")
        val result = Forte.scope().evalExpression(expr)
        val expect = Forte.scope().filterNumber("1099511627777")
        assertEquals(expect, result)
        assertFails {
            Forte.scope().evalExpression(Forte.parseExpression("1000 ** 1000"))
        }
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
