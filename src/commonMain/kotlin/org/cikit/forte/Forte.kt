package org.cikit.forte

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import org.cikit.forte.core.Context
import org.cikit.forte.core.ResultBuilder
import org.cikit.forte.core.TemplateLoader
import org.cikit.forte.core.UPath
import org.cikit.forte.internal.TemplateLoaderImpl
import org.cikit.forte.lib.common.defineCommonExtensions
import org.cikit.forte.lib.core.defineCoreExtensions
import org.cikit.forte.lib.jinja.defineJinjaExtensions
import org.cikit.forte.lib.python.definePythonExtensions
import org.cikit.forte.lib.salt.defineSaltExtensions
import org.cikit.forte.parser.*

expect fun <R> Context.Builder<R>.definePlatformExtensions(): Context.Builder<R>

sealed class Forte private constructor(
    val declarations: List<Declarations>,
    val stringInterpolation: Boolean,
    context: Context.Builder<*>,
    templateLoader: TemplateLoader
) {
    companion object Default : Forte(
        declarations = defaultDeclarations,
        stringInterpolation = true,
        context = Context.builder()
            .defineCoreExtensions()
            .definePlatformExtensions()
            .defineCommonExtensions()
            .defineJinjaExtensions()
            .definePythonExtensions()
            .defineSaltExtensions(),
        templateLoader = TemplateLoader.Empty
    )

    constructor(builder: ForteBuilder) : this(
        declarations = builder.declarations.toList(),
        stringInterpolation = builder.stringInterpolation,
        context = builder.context,
        templateLoader = builder.templateLoader
    )

    val context = Context.Builder.from(
        context,
        when (templateLoader) {
            is TemplateLoaderImpl -> when (templateLoader) {
                is TemplateLoaderImpl.Empty -> templateLoader
                is TemplateLoaderImpl.Caching -> TemplateLoaderImpl.Caching(
                    this,
                    templateLoader.templateLoader
                )
                is TemplateLoaderImpl.Static -> TemplateLoaderImpl.Static(
                    this,
                    templateLoader.templates
                )
            }
            else -> TemplateLoaderImpl.Caching(this, templateLoader)
        }
    )

    fun parser(input: String, path: UPath? = null) =
        parser(Tokenizer(input, path))

    fun parser(tokenizer: TemplateTokenizer) =
        TemplateParser(tokenizer, stringInterpolation, declarations)

    fun parseTemplate(input: String, path: UPath? = null) =
        parser(input, path).parseTemplate()

    fun parseTemplate(tokenizer: TemplateTokenizer) =
        parser(tokenizer).parseTemplate()

    fun parseExpression(input: String): Expression =
        parser(input).parseExpression()

    fun scope(): Context.Builder<Unit> = context.scope()

    fun captureTo(target: (Any?) -> Unit) =
        scope().captureTo(target)

    fun captureTo(flowCollector: FlowCollector<Any?>) =
        scope().captureTo(flowCollector)

    fun captureTo(resultBuilder: ResultBuilder) =
        scope().captureTo(resultBuilder)

    fun captureToList() = scope().captureToList()

    fun flow(block: suspend Context.Builder<Unit>.() -> Unit): Flow<Any?> =
        kotlinx.coroutines.flow.flow {
            captureTo(this).block()
        }

    fun renderTo(target: Appendable) =
        scope().renderTo(target)

    fun renderTo(target: FlowCollector<CharSequence>) =
        scope().renderTo(target)

    fun renderToString() = scope().renderToString()
}

interface ForteBuilder {
    var declarations: MutableList<Declarations>
    var stringInterpolation: Boolean
    var context: Context.Builder<Unit>
    val templateLoader: TemplateLoader
    fun templateLoader(templateLoader: TemplateLoader)
    fun templateLoader(vararg template: Pair<UPath, String>)
}

private class ForteBuilderImpl : ForteBuilder {
    override var declarations = defaultDeclarations.toMutableList()
    override var stringInterpolation: Boolean = true
    override var context: Context.Builder<Unit> = Forte.scope()
    override var templateLoader: TemplateLoader = TemplateLoaderImpl.Empty
        private set

    override fun templateLoader(templateLoader: TemplateLoader) {
        this.templateLoader = templateLoader
    }

    override fun templateLoader(vararg template: Pair<UPath, String>) {
        this.templateLoader = TemplateLoaderImpl.Static(
            Forte,
            template.asSequence()
        )
    }
}

private class ForteInstance(builder: ForteBuilder) : Forte(builder)

fun Forte(builder: ForteBuilder.() -> Unit): Forte {
    val result = ForteBuilderImpl()
    builder(result)
    return ForteInstance(result)
}
