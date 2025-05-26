import kotlinx.coroutines.test.runTest
import org.cikit.forte.Forte
import org.cikit.forte.core.UPath
import org.cikit.forte.eval.evalTemplate
import kotlin.test.assertEquals

expect fun readTests(fileName: String): String

private fun Forte.runTest(testName: String, testSource: String) {
    println("--- $testName ---")
    val expect = testSource.substringBefore("\n~\n")
    val templateSource = testSource.substringAfter("\n~\n")
    val result = evalTemplateToString(templateSource, UPath(testName))
    assertEquals(expect, result)
}

fun Forte.runTests(fileName: String) {
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
        runTest(testName, testSource.toString().trim())
    }
}

private suspend fun Forte.runSuspendingTest(
    testName: String,
    testSource: String
) {
    println("--- $testName ---")
    val expect = testSource.substringBefore("\n~\n")
    val templateSource = testSource.substringAfter("\n~\n")
    val template = parseTemplate(templateSource, UPath(testName))
    val result = captureToString().evalTemplate(template).result
    assertEquals(expect, result)
}

fun Forte.runSuspendingTests(fileName: String) = runTest {
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
        runSuspendingTest(testName, testSource.toString().trim())
    }
}
