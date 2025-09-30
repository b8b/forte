package org.cikit.forte

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import org.cikit.forte.core.*
import org.cikit.forte.parser.*

sealed class Forte(
    private val declarations: List<Declarations> = defaultDeclarations,
    private val context: Context<*> = Core.context
) {
    companion object Default : Forte()

    fun parser(input: String, path: UPath? = null) =
        parser(Tokenizer(input, path))

    fun parser(tokenizer: TemplateTokenizer) =
        TemplateParser(tokenizer, declarations)

    fun parseTemplate(input: String, path: UPath? = null) =
        parser(input, path).parseTemplate()

    fun parseTemplate(tokenizer: TemplateTokenizer) =
        parser(tokenizer).parseTemplate()

    fun parseExpression(input: String): Expression =
        parser(input).parseExpression()

    @Suppress("DEPRECATION")
    @Deprecated("migrate to suspending api")
    fun evalExpression(
        expression: Expression,
        vars: Map<String, Any?>
    ): Any? {
        return if (vars.isEmpty()) {
            context.evalExpression(expression)
        } else {
            context.builder()
                .setVars(vars)
                .evalExpression(expression)
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated("migrate to suspending api")
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

    @Suppress("DEPRECATION")
    @Deprecated("migrate to suspending api")
    fun evalExpression(
        input: String,
        vars: Map<String, Any?>
    ): Any? {
        return evalExpression(parseExpression(input), vars)
    }

    @Suppress("DEPRECATION")
    @Deprecated("migrate to suspending api")
    fun evalExpression(
        input: String,
        vararg vars: Pair<String, Any?>
    ): Any? {
        return evalExpression(parseExpression(input), *vars)
    }

    @Suppress("DEPRECATION")
    @Deprecated("migrate to suspending api")
    fun evalTemplate(
        input: String,
        path: UPath? = null,
        vars: Map<String, Any?> = emptyMap()
    ): Context<Unit> {
        val parsedTemplate = parseTemplate(input, path)
        return scope().setVars(vars).evalTemplate(parsedTemplate)
    }

    @Suppress("DEPRECATION")
    @Deprecated("migrate to suspending api")
    fun evalTemplate(
        input: String,
        vararg vars: Pair<String, Any?>,
        path: UPath? = null
    ): Context<Unit> {
        val parsedTemplate = parseTemplate(input, path)
        return scope().setVars(*vars).evalTemplate(parsedTemplate)
    }

    @Suppress("DEPRECATION")
    @Deprecated("migrate to suspending api")
    fun evalTemplateToString(
        input: String,
        path: UPath? = null,
        vars: Map<String, Any?> = emptyMap()
    ): String {
        val parsedTemplate = parseTemplate(input, path)
        return captureToString()
            .setVars(vars)
            .evalTemplate(parsedTemplate)
            .result
    }

    @Suppress("DEPRECATION")
    @Deprecated("migrate to suspending api")
    fun evalTemplateToString(
        input: String,
        vararg vars: Pair<String, Any?>,
        path: UPath? = null
    ): String {
        val parsedTemplate = parseTemplate(input, path)
        return captureToString()
            .setVars(*vars)
            .evalTemplate(parsedTemplate)
            .result
    }

    fun scope(): Context.Builder<Unit> = context.builder()

    @Deprecated("replace with capture")
    fun scope(captureFunction: (Any?) -> Unit) =
        context.builder().capture(captureFunction)

    fun capture(captureFunction: (Any?) -> Unit) =
        context.builder().capture(captureFunction)

    fun captureToString() = context.builder().captureToString()

    fun captureToList() = context.builder().captureToList()

    fun captureToFlow(flowCollector: FlowCollector<Any?>) =
        context.builder().captureToFlow(flowCollector)

    fun flow(block: suspend Context.Builder<Unit>.() -> Unit): Flow<Any?> =
        kotlinx.coroutines.flow.flow {
            captureToFlow(this).block()
        }
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
