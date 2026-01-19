package org.cikit.forte.internal

import org.cikit.forte.Forte
import org.cikit.forte.core.Context
import org.cikit.forte.core.Context.Builder
import org.cikit.forte.core.TemplateLoader
import org.cikit.forte.core.UPath
import org.cikit.forte.parser.ParsedTemplate

internal sealed class TemplateLoaderImpl : TemplateLoader {

    abstract val rootContext: Context<Unit>

    abstract suspend fun <T> loadTemplate(
        search: Iterable<UPath>,
        relativeTo: UPath? = null,
        block: suspend (ParsedTemplate) -> T
    ): T?

    abstract suspend fun <T> importTemplate(
        search: Iterable<UPath>,
        relativeTo: UPath? = null,
        block: suspend (ParsedTemplate, Context<List<Any?>>) -> T
    ): T?

    object Empty : TemplateLoaderImpl() {
        override val rootContext: Context<Unit>
            get() = Context

        override suspend fun loadTemplate(path: UPath): String? = null

        override suspend fun <T> loadTemplate(
            search: Iterable<UPath>,
            relativeTo: UPath?,
            block: suspend (ParsedTemplate) -> T
        ): T? {
            return null
        }

        override suspend fun <T> importTemplate(
            search: Iterable<UPath>,
            relativeTo: UPath?,
            block: suspend (ParsedTemplate, Context<List<Any?>>) -> T
        ): T? {
            return null
        }
    }

    sealed class Simple(
        private val forte: Forte,
        private val includedTemplates: LinkedHashSet<UPath> = LinkedHashSet(),
    ) : TemplateLoaderImpl() {
        protected val cachedTemplates: MutableMap<UPath, ParsedTemplate> =
            HashMap()

        protected val cachedImports: MutableMap<UPath, Context<List<Any?>>> =
            HashMap()

        override val rootContext: Context<Unit>
            get() = forte.context

        override suspend fun <T> loadTemplate(
            search: Iterable<UPath>,
            relativeTo: UPath?,
            block: suspend (ParsedTemplate) -> T
        ): T? {
            val result = search.firstNotNullOfOrNull { path ->
                val path = resolveTemplate(path, relativeTo)
                val cached = cachedTemplates[path]
                if (cached == null) {
                    val loadedTemplate = loadTemplate(path)
                    if (loadedTemplate == null) {
                        null
                    } else {
                        val parsedTemplate = forte
                            .parseTemplate(loadedTemplate, path)
                        cachedTemplates[path] = parsedTemplate
                        path to parsedTemplate
                    }
                } else {
                    path to cached
                }
            } ?: return null
            val (path, parsedTemplate) = result
            if (path in includedTemplates) {
                error("Error importing '$path': cyclic include detected")
            }
            includedTemplates.add(path)
            try {
                return block(parsedTemplate)
            } finally {
                includedTemplates.remove(path)
            }
        }

        override suspend fun <T> importTemplate(
            search: Iterable<UPath>,
            relativeTo: UPath?,
            block: suspend (ParsedTemplate, Context<List<Any?>>) -> T
        ): T? {
            return loadTemplate(
                search = search,
                relativeTo = relativeTo,
            ) { parsedTemplate ->
                val path = parsedTemplate.path
                    ?: error("Included template has no path")
                val ctx = cachedImports.getOrPut(path) {
                    Builder.from(rootContext, this)
                        .captureToList()
                        .evalTemplate(parsedTemplate)
                        .build()
                }
                block(parsedTemplate, ctx)
            }
        }
    }

    class Static(
        forte: Forte,
        val templates: Map<UPath, String> = emptyMap()
    ) : Simple(forte) {
        constructor(
            forte: Forte,
            templates: Sequence<Pair<UPath, String>>
        ) : this(
            forte,
            templates.associate { (path, input) ->
                UPath("/").appendSegments(path.normalize()) to input
            }
        )

        override suspend fun loadTemplate(path: UPath): String? {
            return templates[path]
        }
    }

    class Caching(
        forte: Forte,
        val templateLoader: TemplateLoader
    ) : Simple(forte), TemplateLoader by templateLoader {
        val cachedInputs: MutableMap<UPath, String> = HashMap()

        override suspend fun loadTemplate(path: UPath): String? {
            val cached = cachedInputs[path]
            if (cached != null) {
                return cached
            }
            val loaded = templateLoader.loadTemplate(path)
                ?: return null
            cachedInputs[path] = loaded
            return loaded
        }
    }

}
