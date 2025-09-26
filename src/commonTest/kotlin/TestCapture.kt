import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.cikit.forte.Forte
import org.cikit.forte.eval.evalTemplate
import kotlin.test.Test
import kotlin.test.assertEquals

class TestCapture {

    val template = Forte.parseTemplate(
        """{{ "test #{x}" }}{{ "test #{y}" }}"""
    )

    @Test
    fun testCaptureToString() = runTest {
        val result = Forte.captureToString()
            .setVars("x" to 1, "y" to 2)
            .evalTemplate(template)
            .result
        assertEquals("test 1test 2", result)
    }

    @Test
    fun testCaptureToList() = runTest {
        val result = Forte.captureToList()
            .setVars("x" to 1, "y" to 2)
            .evalTemplate(template)
            .result
        assertEquals(listOf("test 1", "test 2"), result)
    }

    @Test
    fun testCapture() = runTest {
        val result = buildList {
            Forte.capture { value -> add(value) }
                .setVars("x" to 1, "y" to 2)
                .evalTemplate(template)
        }
        assertEquals(listOf("test 1", "test 2"), result)
    }

    @Test
    fun testCaptureToFlow() = runTest {
        val flow = flow {
            Forte.captureToFlow(this)
                .setVars("x" to 1, "y" to 2)
                .evalTemplate(template)
        }
        assertEquals(listOf("test 1", "test 2"), flow.toList())
    }
}
