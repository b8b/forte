import kotlinx.coroutines.test.runTest
import org.cikit.forte.Forte
import org.cikit.forte.core.UPath
import org.cikit.forte.parser.ParsedTemplate
import kotlin.test.Test

class TestInclude {

    @Test
    fun testDummy() = runTest {
        val forte = Forte {
            templateLoader(
                UPath("dummy") to "hello from dummy"
            )
        }
        val result = forte.renderToString()
            .evalTemplate(forte.parseTemplate("{% include ['dummy'] %}"))
            .result
        println(result)
    }
}
