import kotlin.test.Test
import kotlin.test.assertEquals

class TestEncoding {

    @Test
    fun testIsoCharset() {
        val allBytes = buildString {
            for (uByte in 0 until 256) {
                append(Char(uByte))
            }
        }
        assertEquals(256, allBytes.length)
        for (i in 0 until allBytes.length) {
            assertEquals(i, allBytes[i].code)
        }
        val allByteArray = ByteArray(allBytes.length) { i ->
            i.toByte()
        }
        assertEquals(allBytes, String(allByteArray, Charsets.ISO_8859_1))
    }

}