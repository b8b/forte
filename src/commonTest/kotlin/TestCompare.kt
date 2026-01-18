import kotlin.test.Test
import kotlin.test.assertEquals

class TestCompare {

    @Test
    fun testIntDouble() {
        val a = 10
        val b = 10.0
        assertEquals(0, a.toDouble().compareTo(b))
        assertEquals(0, a.compareTo(b))
    }
}
