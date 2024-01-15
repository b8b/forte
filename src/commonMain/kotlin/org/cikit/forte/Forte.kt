package org.cikit.forte

import okio.Path
import org.cikit.forte.core.Context
import org.cikit.forte.core.Core
import org.cikit.forte.core.evalExpression
import org.cikit.forte.core.evalTemplate
import org.cikit.forte.parser.*

sealed class Forte(
    private val declarations: List<Declarations> = defaultDeclarations,
    private val context: Context<*> = Core.context
) {
    companion object Default : Forte()

    fun parser(input: String, path: Path? = null) =
        parser(Tokenizer(input, path))

    fun parser(tokenizer: TemplateTokenizer) =
        TemplateParser(tokenizer, declarations)

    fun parseTemplate(input: String, path: Path? = null) =
        parser(input, path).parseTemplate()

    fun parseTemplate(tokenizer: TemplateTokenizer) =
        parser(tokenizer).parseTemplate()

    fun parseExpression(input: String): Expression =
        parser(input).parseExpression()

    fun evalExpression(expression: Expression, vars: Map<String, Any?>): Any? {
        return if (vars.isEmpty()) {
            context.evalExpression(expression)
        } else {
            context.builder()
                .setVars(vars)
                .evalExpression(expression)
        }
    }

    fun evalExpression(
        expression: Expression,
        vararg vars: Pair<String, Any?>
    ): Any? {
        return if (vars.isEmpty()) {
            context.evalExpression(expression)
        } else {
            context.builder()
                .setVars(*vars)
                .evalExpression(expression)
        }
    }

    fun evalExpression(input: String, vars: Map<String, Any?>): Any? {
        return evalExpression(parseExpression(input), vars)
    }

    fun evalExpression(input: String, vararg vars: Pair<String, Any?>): Any? {
        return evalExpression(parseExpression(input), *vars)
    }

    fun evalTemplate(
        input: String,
        path: Path? = null,
        vars: Map<String, Any?> = emptyMap()
    ): Context<Unit> {
        val parsedTemplate = parseTemplate(input, path)
        return scope().setVars(vars).evalTemplate(parsedTemplate)
    }

    fun evalTemplate(
        input: String,
        vararg vars: Pair<String, Any?>,
        path: Path? = null
    ): Context<Unit> {
        val parsedTemplate = parseTemplate(input, path)
        return scope().setVars(*vars).evalTemplate(parsedTemplate)
    }

    fun evalTemplateToString(
        input: String,
        path: Path? = null,
        vars: Map<String, Any?> = emptyMap()
    ): String {
        val parsedTemplate = parseTemplate(input, path)
        return captureToString()
            .setVars(vars)
            .evalTemplate(parsedTemplate)
            .result
    }

    fun evalTemplateToString(
        input: String,
        vararg vars: Pair<String, Any?>,
        path: Path? = null
    ): String {
        val parsedTemplate = parseTemplate(input, path)
        return captureToString()
            .setVars(*vars)
            .evalTemplate(parsedTemplate)
            .result
    }

    fun scope(): Context.Builder<Unit> = context.builder()

    fun scope(captureFunction: (Any?) -> Unit) =
        context.builder().capture(captureFunction)

    fun capture(captureFunction: (Any?) -> Unit) =
        context.builder().capture(captureFunction)

    fun captureToString() = context.builder().captureToString()

    fun captureToList() = context.builder().captureToList()
}

class ForteBuilder {
    var declarations = defaultDeclarations.toMutableList()
    val context: Context.Builder<Unit> = Core.context.builder()
}

private class ForteInstance(
    builder: ForteBuilder
) : Forte(builder.declarations.toList(), builder.context.build())

fun Forte(builder: ForteBuilder.() -> Unit): Forte {
    val result = ForteBuilder()
    builder(result)
    return ForteInstance(result)
}
