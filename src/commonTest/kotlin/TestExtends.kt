import kotlinx.coroutines.test.runTest
import org.cikit.forte.Forte
import org.cikit.forte.core.UPath
import kotlin.test.Test
import kotlin.test.assertEquals

class TestExtends {

    @Test
    fun testSimple() = runTest {
        val template = Forte.parseTemplate(
            "{% extends 'dummy' %}{% block b1 %}don't like {{ super() }}{% endblock %}"
        )
        val forte = Forte {
            templateLoader(
                UPath("dummy") to "{% block b1 %}hello from dummy{% endblock %}"
            )
        }
        val result = forte
            .renderToString()
            .evalTemplate(template)
            .result
        assertEquals("don't like hello from dummy", result)
    }

    @Test
    fun testBlocksWithoutExtends() = runTest {
        val template = Forte.parseTemplate(
            "Hello {% block block1 %}World{% endblock %}"
        )
        val result = Forte
            .renderToString()
            .evalTemplate(template)
            .result
        assertEquals("Hello World", result)
    }

    @Test
    fun testCached() = runTest {
        val forte = Forte {
            templateLoader(
                UPath("dummy") to "Hello {% block b1 %}hello from dummy{% endblock %}"
            )
        }
        val result1 = forte
            .renderToString()
            .evalTemplate(
                Forte.parseTemplate("{% extends 'dummy' %}{% block b1 %}1{% endblock %}")
            )
            .result
        assertEquals("Hello 1", result1)
        val result2 = forte
            .renderToString()
            .evalTemplate(
                Forte.parseTemplate("{% extends 'dummy' %}{% block b1 %}2{% endblock %}")
            )
            .result
        assertEquals("Hello 2", result2)
    }

    @Test
    fun testNested() = runTest {
        val forte = Forte {
            templateLoader(
                UPath("dummy1") to "{% extends '/dummy2' %}{% block b1 %}hello from dummy1{% endblock %}",
                UPath("dummy2") to "{% block b1 %}hello from dummy2{% endblock %} {% block b2 %}hello again{% endblock %}",
            )
        }
        val result = forte
            .renderToString()
            .evalTemplate(
                Forte.parseTemplate("{% extends 'dummy1' %}{% block b1 %}no hello{% endblock %}")
            )
            .result
        assertEquals("no hello hello again", result)
    }
}
