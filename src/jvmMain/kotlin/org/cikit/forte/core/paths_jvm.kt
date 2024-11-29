package org.cikit.forte.core

import java.net.URI
import java.nio.file.Path
import kotlin.io.path.relativeTo
import kotlin.io.path.toPath

private val rootUriPath = URI.create("file:///").toPath()

fun Path.toUPath(): UPath {
    val p1 = toUri().rawPath.trimEnd('/')
    if (isAbsolute) {
        return UPath(p1, DecodeUrlPath)
    }
    var nc = nameCount
    // nameCount of a relative path must be > 0
    assert(nc > 0) {
        "internal error: $this.nameCount == $nc"
    }
    for (i in p1.indices.reversed()) {
        if (p1[i] == '/') {
            nc--
            if (nc == 0) {
                return UPath(p1.substring(i + 1), DecodeUrlPath)
            }
        }
    }
    return UPath(p1, DecodeUrlPath)
}

fun Path.append(other: UPath): Path = append(other.toNioPath())

fun Path.append(other: Path): Path = if (other.isAbsolute) {
    resolve(other.relativeTo(other.root))
} else {
    resolve(other)
}

fun UPath.toNioPath(): Path {
    val absolute = isAbsolute
    val urlPath = toUrlPath()
    val trimmed = urlPath.trimStart(Separator)
    if (trimmed.isEmpty()) {
        return rootUriPath
    }
    val absoluteUrlPath = "file:///$trimmed"
    val nioPath = URI.create(absoluteUrlPath).toPath()
    return if (absolute) {
        nioPath
    } else {
        nioPath.relativeTo(rootUriPath)
    }
}
