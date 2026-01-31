import kotlinx.coroutines.test.runTest
import org.cikit.forte.Forte
import org.cikit.forte.core.UPath
import org.cikit.forte.parser.ParsedTemplate
import kotlin.test.Test
import kotlin.test.assertEquals

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

    @Test
    fun testImport() = runTest {
        val forte = Forte {
            templateLoader(
                UPath("dummy") to "{% set x = 1 %}{% macro superfun() %}hello from dummy{% endmacro %}"
            )
        }
        val result = forte.renderToString()
            .evalTemplate(forte.parseTemplate("{% import 'dummy' as dummy %}{{ dummy.x }}, {{ dummy.superfun() }}"))
            .result
        assertEquals("1, hello from dummy", result)
        println(result)

    }
}
