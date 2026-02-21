import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.encodeToMap
import org.cikit.forte.Forte
import org.cikit.forte.core.loadJson
import kotlin.test.Test
import kotlin.test.assertEquals

class TestSerializable {

    @Test
    fun testJson() = runTest {
        val input = Json.decodeFromString<JsonElement>(
            """{"a":1,"b":2,"c":NaN}"""
        )
        val result = Forte.scope()
            .loadJson("input", input)
            .evalExpression(
                Forte.parseExpression("input.a + input.b")
            )
        assertEquals(3, (result as Number).toInt())
        val result2 = Forte.scope()
            .loadJson("input", input)
            .evalExpression(
                Forte.parseExpression("input.c")
            )
        println(result2)
    }

    @Serializable
    data class Sample(val x: Int, val y: String?)

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testDataObject() = runTest {
        val input = Sample(1, null)
        val result = Forte.scope()
            .setVar("input", Properties.encodeToMap(input))
            .evalExpression(
                Forte.parseExpression("input.x + 1")
            )
        assertEquals(2, result)
    }

    @Test
    fun testDataObjectViaJson() = runTest {
        val input = Sample(1, null)
        val result = Forte.scope()
            .loadJson("input", Json.encodeToJsonElement(input))
            .evalExpression(
                Forte.parseExpression("input.x + 1")
            )
        assertEquals(2, (result as Number).toInt())
    }
}
