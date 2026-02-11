package org.cikit.forte.lib.funit

import org.cikit.forte.core.Context
import org.cikit.forte.core.TestMethod
import org.cikit.forte.parser.Declarations
import org.cikit.forte.parser.Expression
import org.cikit.forte.parser.ParseException
import org.cikit.forte.parser.Token
import org.cikit.forte.parser.expect

val fUnitDeclarations = listOf(
    Declarations.Command("assert", setOf("endassert")) {
        if (name == "assert") {
            val assertion = parseExpression()
            if (assertion !is Expression.FunctionCall) {
                throw ParseException(
                    tokenizer,
                    assertion,
                    "expected test call"
                )
            }
            args["test"] = Expression.StringLiteral(
                assertion.first,
                assertion.first,
                assertion.name
            )
            args["argNames"] = Expression.ArrayLiteral(
                assertion.first,
                assertion.first,
                assertion.args.names.map { argName ->
                    Expression.StringLiteral(
                        assertion.first,
                        assertion.first,
                        argName
                    )
                }
            )
            args["argValues"] = Expression.ArrayLiteral(
                assertion.first,
                assertion.first,
                assertion.args.values
            )
        }
    },
    Declarations.Command("assert_that") {
        val assertion = parseExpression()
        if (assertion !is Expression.TransformOp ||
            assertion.decl.name != TestMethod.OPERATOR.value)
        {
            throw ParseException(
                tokenizer,
                assertion,
                "expected test expression"
            )
        }
        args["subject"] = assertion.left
        args["test"] = Expression.StringLiteral(
            assertion.tokens.first(),
            assertion.tokens.last(),
            assertion.name
        )
        args["argNames"] = Expression.ArrayLiteral(
            assertion.tokens.first(),
            assertion.tokens.last(),
            assertion.args.names.map { argName ->
                Expression.StringLiteral(
                    assertion.tokens.first(),
                    assertion.tokens.last(),
                    argName
                )
            }
        )
        args["argValues"] = Expression.ArrayLiteral(
            assertion.tokens.first(),
            assertion.tokens.last(),
            assertion.args.values
        )
    },
    Declarations.Command("assert_fails", setOf("endassert")) {
        val t = tokenizer.peek(true)
        if (t !is Token.EndCommand) {
            expect<Token.Identifier>("as")
            val variable = expect<Token.Identifier>()
            args["varName"] = Expression.StringLiteral(
                variable,
                variable,
                input.substring(variable.first..variable.last)
            )
        }
    },
    Declarations.Command("assert_true") {
        args["test"] = parseExpression()
    },
    Declarations.Command("assert_false") {
        args["test"] = parseExpression()
    },
)

fun <R> Context.Builder<R>.defineFUnitExtensions(): Context.Builder<R> {
    defineControlTag("assert", ControlAssert())
    defineControlTag("assert_fails", ControlAssertFails())
    defineCommandTag("assert_true", CommandAssertBoolean(true))
    defineCommandTag("assert_false", CommandAssertBoolean(false))
    defineCommandTag("assert_that", CommandAssertThat())
    return this
}
