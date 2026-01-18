package org.cikit.forte.core

import kotlinx.io.bytestring.isEmpty
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.relativeTo
import kotlin.io.path.toPath

private val rootUriPath = URI.create("file:///").toPath()

fun Path.toUPath(): UPath {
    return if (isAbsolute) {
        var p = toUri().rawPath
        if (p.length > 1) {
            p = p.removeSuffix("/")
        }
        UPath(p, DecodeUrlPath)
    } else {
        val p = rootUriPath
            .resolve(this)
            .toUri()
            .rawPath
            .removePrefix("/")
            .removeSuffix("/")
        UPath(p, DecodeUrlPath)
    }
}

@Deprecated("renamed to appendSegments", ReplaceWith("appendSegments"))
fun Path.append(other: UPath): Path = appendSegments(other.toNioPath())

fun Path.appendSegments(other: UPath): Path = appendSegments(other.toNioPath())


@Deprecated("renamed to appendSegments", ReplaceWith("appendSegments"))
fun Path.append(other: Path): Path = appendSegments(other)

fun Path.appendSegments(other: Path): Path = if (other.isAbsolute) {
    resolve(other.relativeTo(other.root))
} else {
    resolve(other)
}

fun UPath.toNioPath(): Path {
    if (encoded.isEmpty()) {
        return Path("")
    }
    val absolute = isAbsolute
    val urlPath = toUrlPath()
    val trimmed = urlPath.trimStart(UPath.SEPARATOR)
    if (trimmed.isEmpty()) {
        return rootUriPath
    }
    val absoluteUrlPath = "file:///$trimmed"
    val nioPath = URI.create(absoluteUrlPath).toPath()
    return if (absolute) {
        nioPath
    } else {
        nioPath.subpath(0, nioPath.nameCount)
    }
}
