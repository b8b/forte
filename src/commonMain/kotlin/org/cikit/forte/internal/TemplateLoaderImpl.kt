package org.cikit.forte.internal

import org.cikit.forte.Forte
import org.cikit.forte.core.Context
import org.cikit.forte.core.TemplateLoader
import org.cikit.forte.core.UPath
import org.cikit.forte.parser.ParsedTemplate

internal sealed class TemplateLoaderImpl(
) : TemplateLoader {

    abstract suspend fun includeTemplate(
        search: Iterable<UPath>,
        relativeTo: UPath?,
        ignoreMissing: Boolean = false,
        block: suspend (ParsedTemplate) -> Unit
    )

    abstract suspend fun importTemplate(
        search: Iterable<UPath>,
        relativeTo: UPath?,
        ignoreMissing: Boolean = false,
        block: suspend (ParsedTemplate, Context<List<Any?>>) -> Unit
    )

    object Empty : TemplateLoaderImpl() {
        override fun loadTemplate(path: UPath): String? = null

        override suspend fun includeTemplate(
            search: Iterable<UPath>,
            relativeTo: UPath?,
            ignoreMissing: Boolean,
            block: suspend (ParsedTemplate) -> Unit
        ) {
            if (ignoreMissing) {
                return
            }
            error("Template not found: $search")
        }

        override suspend fun importTemplate(
            search: Iterable<UPath>,
            relativeTo: UPath?,
            ignoreMissing: Boolean,
            block: suspend (ParsedTemplate, Context<List<Any?>>) -> Unit
        ) {
            if (ignoreMissing) {
                return
            }
            error("Template not found: $search")
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

        override suspend fun includeTemplate(
            search: Iterable<UPath>,
            relativeTo: UPath?,
            ignoreMissing: Boolean,
            block: suspend (ParsedTemplate) -> Unit
        ) {
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
            }
            if (result == null) {
                if (ignoreMissing) {
                    return
                }
                error("Template not found: $search")
            }
            val (path, parsedTemplate) = result
            if (path in includedTemplates) {
                error("Error importing '$path': cyclic include detected")
            }
            includedTemplates.add(path)
            try {
                block(parsedTemplate)
            } finally {
                includedTemplates.remove(path)
            }
        }

        override suspend fun importTemplate(
            search: Iterable<UPath>,
            relativeTo: UPath?,
            ignoreMissing: Boolean,
            block: suspend (ParsedTemplate, Context<List<Any?>>) -> Unit
        ) {
            includeTemplate(
                search = search,
                relativeTo = relativeTo,
                ignoreMissing = ignoreMissing
            ) { parsedTemplate ->
                val path = parsedTemplate.path
                    ?: error("Included template has no path")
                val ctx = cachedImports.getOrPut(path) {
                    forte.scope()
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
        val templates: Map<UPath, String>
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

        override fun loadTemplate(path: UPath): String? {
            return templates[path]
        }
    }

    class Caching(
        forte: Forte,
        val templateLoader: TemplateLoader
    ) : Simple(forte), TemplateLoader by templateLoader {
        val cachedInputs: MutableMap<UPath, String> = HashMap()

        override fun loadTemplate(path: UPath): String? {
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
