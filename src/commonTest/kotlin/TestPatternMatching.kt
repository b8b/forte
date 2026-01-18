import kotlinx.coroutines.test.runTest
import org.cikit.forte.Forte
import org.cikit.forte.core.Method
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.typeName
import org.cikit.forte.parser.Declarations
import kotlin.test.Test
import kotlin.test.assertEquals

class TestPatternMatching {

    sealed class MatchSubject(message: String) {
        data object None : MatchSubject("no subject")
        data object Matched : MatchSubject("matched subject")
        class Unmatched(val subject: Any?) : MatchSubject("unmatched subject")
        class Result(val result: Any?) : MatchSubject("result")
    }

    class ThenReturn : Method {
        override val operator: String
            get() = "then"

        override val isRescue: Boolean
            get() = true

        override fun invoke(subject: Any?, args: NamedArgs): Any {
            val right: Any?
            args.use {
                right = requireAny("result")
            }
            return when (subject) {
                is MatchSubject -> when (subject) {
                    is MatchSubject.Matched -> MatchSubject.Result(right)
                    else -> subject
                }

                else -> error("invalid type for then: $subject")
            }
        }
    }

    @Test
    fun testWithSubject() = runTest {
        val forte = Forte {
            declarations += Declarations.TransformOp(5, "->", name = "then")
            declarations += Declarations.BinOp(6, "with", left = true)
            declarations += Declarations.BinOp(6, "else")

            val eq = context.getBinaryOpFunction("eq")
                ?: error("binary operator 'eq' is not defined")

            context.defineFunction("match") { args ->
                args.use {
                    optionalNullable(
                        "subject",
                        { MatchSubject.Unmatched(it) },
                        { MatchSubject.None }
                    )
                }
            }

            context.defineBinaryOpFunction("with") { left, right ->
                when (left) {
                    is MatchSubject -> when (left) {
                        is MatchSubject.None -> {
                            if (right as Boolean) {
                                MatchSubject.Matched
                            } else {
                                left
                            }
                        }

                        is MatchSubject.Unmatched -> {
                            if (eq(left.subject, right) as Boolean) {
                                MatchSubject.Matched
                            } else {
                                left
                            }
                        }

                        is MatchSubject.Matched, is MatchSubject.Result -> {
                            left
                        }
                    }

                    else -> error("invalid type for with: ${typeName(left)}")
                }
            }

            context.defineMethod("return", ThenReturn())

            context.defineBinaryOpFunction("else") { left, right ->
                when (left) {
                    is MatchSubject -> when (left) {
                        is MatchSubject.Result -> left.result
                        else -> right
                    }

                    else -> error("invalid type for else: $left")
                }
            }
        }

        val badUsage = """ match(x) with 1 """
        val badExpr = forte.parseExpression(badUsage)
        val badResult = forte.scope().setVars("x" to 1).evalExpression(badExpr)
        println(badResult)

        val src = """ match(x) with 1 -> return("a") with 2 -> return("b") else "c" """
        val expr = forte.parseExpression(src)
        println(expr)
        assertEquals("a", forte.scope().setVars("x" to 1).evalExpression(expr))
        assertEquals("b", forte.scope().setVars("x" to 2).evalExpression(expr))
        assertEquals("c", forte.scope().setVars("x" to 22).evalExpression(expr))
        val expr2 = forte.parseExpression(
            """ match() with a is defined -> return("BAD") else "GOOD" """
        )
        assertEquals("GOOD", forte.scope().evalExpression(expr2))
    }
}
