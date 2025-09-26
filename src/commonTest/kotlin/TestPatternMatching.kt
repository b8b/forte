import kotlinx.coroutines.test.runTest
import org.cikit.forte.Forte
import org.cikit.forte.eval.evalExpression
import org.cikit.forte.parser.Declarations
import kotlin.test.Test
import kotlin.test.assertEquals

class TestPatternMatching {

    sealed class MatchSubject(message: String) {
        data object None : MatchSubject("no subject")
        class Unmatched(val subject: Any?) : MatchSubject("unmatched subject")
        class Matched(val subject: Any?) : MatchSubject("matched subject")
        class Result(val result: Any?) : MatchSubject("result")
    }

    @Test
    fun testWithSubject() = runTest {
        val forte = Forte {
            declarations += Declarations.TransformOp(5, "->", name = "then")
            declarations += Declarations.BinOp(6, "with", left = true)
            declarations += Declarations.BinOp(6, "else")

            val eq = context.getBinaryOpFunction("eq")!!

            context.defineFunction("match") { _, args ->
                args.use {
                    optionalNullable(
                        "subject",
                        { MatchSubject.Unmatched(it) },
                        { MatchSubject.None }
                    )
                }
            }

            context.defineBinaryOpFunction("with") { ctx, left, right ->
                when (left) {
                    is MatchSubject -> when (left) {
                        is MatchSubject.None -> {
                            if (right as Boolean) {
                                MatchSubject.Matched(left)
                            } else {
                                left
                            }
                        }

                        is MatchSubject.Unmatched -> {
                            if (eq(ctx, left.subject, right) as Boolean) {
                                MatchSubject.Matched(left.subject)
                            } else {
                                left
                            }
                        }

                        is MatchSubject.Matched, is MatchSubject.Result -> {
                            left
                        }
                    }

                    else -> error("invalid type for with: $left")
                }
            }

            context.defineMethod("return", "then") { _, subject, args ->
                args.use {
                    val right = requireAny("result")
                    when (subject) {
                        is MatchSubject -> when (subject) {
                            is MatchSubject.Matched -> MatchSubject.Result(right)
                            else -> subject
                        }

                        else -> error("invalid type for then: $subject")
                    }
                }
            }

            context.defineBinaryOpFunction("else") { _, left, right ->
                when (left) {
                    is MatchSubject -> when (left) {
                        is MatchSubject.Result -> left.result
                        else -> right
                    }

                    else -> error("invalid type for else: $left")
                }
            }
        }

        println(forte.parseExpression(""" match(x) with 1 -> return("a") with 2 -> return("b") else "c" """))
        val expr = forte.parseExpression(
            """ match(x) with 1 -> return("a") with 2 -> return("b") else "c" """
        )
        assertEquals("a", forte.scope().setVars("x" to 1).evalExpression(expr))
        assertEquals("b", forte.scope().setVars("x" to 2).evalExpression(expr))
        assertEquals("c", forte.scope().setVars("x" to 22).evalExpression(expr))
        val expr2 = forte.parseExpression(
            """ match() with a is defined -> return("BAD") else "GOOD" """
        )
        assertEquals("GOOD", forte.scope().evalExpression(expr2))
    }
}
