package org.cikit.forte.core

fun interface TemplateLoader {

    object Empty : TemplateLoader {
        override suspend fun loadTemplate(path: UPath): String? = null
    }

    suspend fun resolveTemplate(path: UPath, relativeTo: UPath?): UPath {
        if (path.isAbsolute) {
            return path.normalize()
        }
        return (relativeTo ?: UPath("/")).appendSegments(path).normalize()
    }

    suspend fun loadTemplate(path: UPath, relativeTo: UPath?): String? =
        loadTemplate(resolveTemplate(path, relativeTo))

    suspend fun loadTemplate(path: UPath): String?

}
