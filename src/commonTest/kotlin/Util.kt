import kotlinx.coroutines.test.runTest
import org.cikit.forte.Forte
import org.cikit.forte.core.Method
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.UPath
import org.cikit.forte.core.typeName
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.collections.set
import kotlin.reflect.KClass
import kotlin.test.assertEquals

expect fun readTests(fileName: String): String

private suspend fun Forte.runTest(
    testName: String,
    testSource: String,
    preprocessSource: (String) -> String
) {
    println("--- $testName ---")
    val finalSource = preprocessSource(testSource)
    val expect = finalSource.substringBefore("\n~\n").trim()
    val templateSource = finalSource.substringAfter("\n~\n")
    val template = parseTemplate(templateSource, UPath(testName))
    val result = renderToString().evalTemplate(template).result.trim()
    assertEquals(expect, result)
}

fun Forte.runTests(
    fileName: String,
    preprocessSource: (String) -> String = { it }
) = runTest {
    val testsMd = readTests(fileName)
    val lines = testsMd.split("\n").iterator()
    while (lines.hasNext()) {
        val line = lines.next()
        if (!line.startsWith("##")) continue
        val testName = line.removePrefix("##").trim()
        val testSource = StringBuilder()
        while (lines.hasNext()) {
            val line2 = lines.next()
            if (!line2.startsWith("```")) continue
            while (lines.hasNext()) {
                val line3 = lines.next()
                if (line3.startsWith("```")) break
                testSource.appendLine(line3)
            }
            break
        }
        runTest(testName, testSource.toString().trim(), preprocessSource)
    }
}

class OverloadResolver private constructor(
    private val methods: Map<KClass<*>, Method>,
    private val fallback: Method?
) : Method {
    constructor(
        subjectType: KClass<*>,
        implementation: Method,
        fallback: Method? = null
    ) : this(
        mapOf(subjectType to implementation),
        fallback
    )

    override fun invoke(subject: Any?, args: NamedArgs): Any? {
        if (subject != null) {
            methods[subject::class]?.let {
                return it(subject, args)
            }
        }
        if (fallback != null) {
            return fallback(subject, args)
        }
        throw IllegalArgumentException(
            "invalid operand of type '${typeName(subject)}'"
        )
    }

    fun plus(
        subjectType: KClass<*>,
        implementation: Method
    ): OverloadResolver {
        val newMap: MutableMap<KClass<*>, Method> = when {
            methods.contains(subjectType) -> HashMap(methods.size)
            else -> HashMap(methods.size + 1)
        }
        for ((k, v) in methods) {
            if (k !== subjectType) {
                newMap[k] = v
            }
        }
        newMap[subjectType] = implementation
        return OverloadResolver(newMap, fallback)
    }
}
