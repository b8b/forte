import kotlinx.coroutines.test.runTest
import org.cikit.forte.Forte
import org.junit.jupiter.api.Test
import java.math.BigInteger
import kotlin.test.assertEquals

class TestBigCompare {

    @Test
    fun testBigCompare() = runTest{
        val result = Forte.scope()
            .setVar("a", 1)
            .setVar("b", BigInteger("412837658761287364876287638476871324786"))
            .evalExpression(Forte.parseExpression("a < b"))
        assertEquals(true, result)
    }

}
