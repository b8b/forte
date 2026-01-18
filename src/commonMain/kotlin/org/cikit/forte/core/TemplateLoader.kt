package org.cikit.forte.core

fun interface TemplateLoader {

    object Empty : TemplateLoader {
        override fun loadTemplate(path: UPath): String? = null
    }

    fun resolveTemplate(path: UPath, relativeTo: UPath?): UPath {
        if (path.isAbsolute) {
            return path.normalize()
        }
        return (relativeTo ?: UPath("/")).appendSegments(path).normalize()
    }

    fun loadTemplate(path: UPath, relativeTo: UPath?): String? =
        loadTemplate(resolveTemplate(path, relativeTo))

    fun loadTemplate(path: UPath): String?

}
