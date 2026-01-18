import kotlinx.coroutines.test.runTest
import org.cikit.forte.Forte
import kotlin.test.Test
import kotlin.test.assertEquals

class TestBigCompare {

    @Test
    fun testBigCompare() = runTest {
        val result = Forte.scope()
            .setVar("a", 1)
            .setVar("b", 65476534743654364L)
            .evalExpression(Forte.parseExpression("a < b"))
        assertEquals(true, result)
    }
}
