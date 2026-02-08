import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.cikit.forte.Forte
import org.cikit.forte.core.*
import java.security.MessageDigest
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals

class TestEvalSuspended {

    private val crypto = Dispatchers.IO.limitedParallelism(1, name = "crypto")

    private val forte = Forte {
        context.defineFilter("sha256") { subject, args ->
            args.requireEmpty()
            when (subject) {
                is String -> Suspended {
                    "${Thread.currentThread().id}: " + withContext(crypto) {
                        val md = MessageDigest.getInstance("SHA-256")
                        md.update(subject.encodeToByteArray())
                        val result = md.digest().joinToString("") { "%02x".format(it) }
                        result + " (calculated on ${Thread.currentThread().id})"
                    }
                }
                else -> error("invalid type for sha256: ${typeName(subject)}")
            }
        }
        context.defineTest("good") { subject, args ->
            args.requireEmpty()
            subject == 42
        }
    }

    @Test
    fun testSimple() {
        val ctx = forte.renderToString()
        val expression = forte.parseExpression("'abc'|sha256")
        (0 until 10).map {
            thread {
                runBlocking {
                    val result = ctx.evalExpression(expression)
                    println(result)
                }
            }
        }.forEach { it.join() }
    }

    @Test
    fun testSelect() {
        val input = (0 until 100)
        val ctx = forte.renderToString().setVar("input", input)
        val expression = forte.parseExpression(
            "[input|select('good'), input|reject('good')]"
        )
        val result = runBlocking {
            ctx.evalExpression(expression)
        }
        val all = buildList {
            for (list in result as List<*>) {
                for (item in list as Iterable<*>) {
                    add(item as Int)
                }
            }
        }
        assertEquals(input.toSet(), all.sorted().toSet())
    }
}
