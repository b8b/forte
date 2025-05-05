import kotlinx.io.bytestring.ByteStringBuilder
import kotlinx.io.bytestring.append
import kotlinx.io.bytestring.isEmpty
import org.cikit.forte.core.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.io.path.*
import kotlin.test.assertFails
import kotlin.test.assertNull

class TestPath {

    @Test
    fun testBasic() {
        val empty = UPath()
        assertTrue(empty.encoded.isEmpty())
        assertEquals(1, empty.segments.count())
        assertTrue(empty.lastSegment!!.pathString.isEmpty())
        assertTrue(empty.isUtf8Only)

        assertFails {
            UPath("test\u0000.txt")
        }

        assertFails {
            UPath("test\\000.txt", decodeEscapes = true)
        }

        assertEquals(UPath("1\ta.txt"), UPath("1\\ta.txt", DecodeEscapes))
        assertEquals("1\\ta.txt", UPath("1\ta.txt").toVisualPath())

        assertFalse(UPath("test%201.txt", DecodeUrlPath).isUtf8Only)
        assertFalse(UPath("test\\t1.txt", DecodeEscapes).isUtf8Only)

        val absolutePath = UPath("/some/where/over/there")
        val nioPath = absolutePath.toNioPath()
        assertEquals(nioPath.isAbsolute, absolutePath.isAbsolute)

        assertEquals(nioPath.fileName.toUPath(), absolutePath.lastSegment)
        assertEquals(nioPath.pathString, absolutePath.pathString)
        assertEquals(nioPath.parent.pathString, absolutePath.parent!!.pathString)
        assertEquals(nioPath.nameCount, absolutePath.segments.count())
        assertEquals(
            nioPath.subpath(1, 2).pathString,
            absolutePath.segments.toList().let {
                it[1].append(*it.subList(2, 2).toTypedArray()).pathString
            }
        )
        assertEquals(
            nioPath.nameWithoutExtension,
            absolutePath.nameWithoutExtension
        )
        assertEquals(nioPath.extension, absolutePath.extension)
        assertEquals(
            nioPath.normalize().pathString,
            absolutePath.normalize().pathString
        )

        assertEquals(
            UPath("/bin/my_script"),
            UPath("/bin", "my_script.sh").withoutExtension()
        )
    }

    @Test
    fun testNio() {
        assertEquals("/", UPath("/").toNioPath().pathString)
        assertEquals(Path("foo"), UPath("foo").toNioPath())
        assertEquals(UPath("foo"), Path("foo").toUPath())
        assertEquals("foo/bar", Path("foo").append(Path("bar")).pathString)
        assertEquals("foo/bar", Path("foo").append(Path("/bar")).pathString)
        assertEquals("foo/bar", Path("foo").append(UPath("bar")).pathString)
        assertEquals("foo/bar", Path("foo").append(UPath("/bar")).pathString)
    }

    @Test
    fun testNameCount() {
        val tests = listOf(
            1 to "",
            0 to "/",
            0 to "///",
            1 to "a",
            1 to "a/",
            1 to "/a",
            1 to "//a",
        )
        for ((count, pathString) in tests) {
            assertEquals(count, Path(pathString).nameCount) {
                "unexpected count of segments for Path '$pathString'"
            }
            assertEquals(count, UPath(pathString).segments.count()) {
                "unexpected count of segments for UPath '$pathString'"
            }
            assertEquals(
                Path(pathString).lastOrNull()?.pathString,
                UPath(pathString).lastSegment?.pathString
            ) {
                "unexpected last segments for '$pathString'"
            }
            if (count == 0) {
                assertEquals("", Path(pathString).name)
                assertEquals("", UPath(pathString).name)
            } else {
                assertEquals(Path(pathString).name, UPath(pathString).name)
            }
        }
    }

    @Test
    fun testParent() {
        assertNull(UPath("").parent)
        assertNull(UPath("/").parent)
        assertNull(UPath("///").parent)
        assertEquals("/", UPath("/foo").parent?.pathString)
        assertEquals("/foo", UPath("/foo/bar").parent?.pathString)
    }

    @Test
    fun testAbsolute() {
        assertTrue(UPath("/").isAbsolute)
        assertTrue(UPath("//").isAbsolute)
        assertTrue(UPath("///").isAbsolute)
        assertTrue(UPath("/a/b/c//").isAbsolute)
        assertFalse(UPath("").isAbsolute)
        assertFalse(UPath("a").isAbsolute)
        assertFalse(UPath(".").isAbsolute)
        assertFalse(UPath("..").isAbsolute)
        assertFalse(UPath("a/b/c").isAbsolute)
    }

    @Test
    fun testHidden() {
        assertTrue(UPath(".hidden").isHidden)
        assertFalse(UPath("hidden").isHidden)
        assertFalse(UPath("hidden.").isHidden)
        assertFalse(UPath("hidden.txt").isHidden)
        assertTrue(UPath("/.hidden").isHidden)
        assertTrue(UPath("//.hidden").isHidden)
        assertTrue(UPath("/a/b/c/.hidden").isHidden)
    }

    @Test
    fun testResolve() {
        // sub paths are not resolved (segments are appended)
        assertEquals(
            "foo/some/where/else",
            Path("foo", "//some/", "//where//", "//else//").pathString
        )
        assertEquals(
            "foo/some/where/else",
            UPath("foo", "/some/where/else").pathString
        )
        assertEquals(
            "foo/some/where/else",
            UPath(UPath("foo"), "/some/where/else").pathString
        )

        // same with File
        assertEquals(
            "foo/some/where/else",
            File(File("foo"), "/some/where/else").path
        )

        // resolve depends on other.isAbsolutePath

        assertEquals(
            // Path normalizes separators
            "/some/where/../else",
            Path("foo").resolve("//some//where//..//else//").pathString
        )
        assertEquals(
            // Path normalizes separators
            "foo/some/where/../else",
            Path("foo").resolve("some//where//..//else//").pathString
        )

        assertEquals(
            // UPath only normalizes separators between 2 paths
            "//some//where//..//else//",
            UPath("foo").resolve("//some//where//..//else//").pathString
        )
        assertEquals(
            // UPath only normalizes separators between 2 paths
            "foo/some//where//..//else//",
            UPath("foo").resolve("some//where//..//else//").pathString
        )
        assertEquals(
            // UPath only normalizes separators between 2 paths
            "foo/some//where//..//else//",
            UPath("foo").resolve(UPath("some//where//..//else//")).pathString
        )

        // The following is common on unix
        // x=foo/bar
        // y=/some/where/else
        // z="$x/$y"

        val x = UPath("foo/bar")
        val y = UPath("/some/where/else")
        assertEquals("foo/bar/some/where/else", UPath("$x/$y").normalize().pathString)
        assertEquals("foo/bar/some/where/else", UPath(x, y.pathString).pathString)

        // the binary safe variant with Path could be
        assertEquals(
            Path("foo/bar/some/where/else"),
            Path("foo/bar") /
                    Path("/some/where/else").let {
                        if (it.isAbsolute) {
                            it.relativeTo(it.root)
                        } else {
                            it
                        }
                    }
        )

        // to avoid confusion, UPath doesn't provide operator functions
        // resolve() handles absolute paths specially
        // append() simply appends all path segments
        assertEquals(
            UPath("foo/bar/some/where/else"),
            UPath("foo/bar").append(UPath("/some/where/else"))
        )
        assertEquals(
            UPath("foo/bar/some/where/else"),
            UPath("foo/bar").append(
                UPath("/some"),
                UPath("/where"),
                UPath("/else")
            )
        )
        assertEquals(
            UPath("foo/bar/some/where/else"),
            UPath("foo/bar").append("/some/where/else")
        )
        assertEquals(
            UPath("foo/bar/some/where/else"),
            UPath("foo/bar").append("/some/", "where", "/else")
        )
    }

    @Test
    fun testNormalize() {
        assertEquals(UPath("../.."), UPath("..//..").normalize(dots = false))
        assertEquals(UPath("/foo/bar"), UPath("/foo//bar/..//bar//").normalize())
    }

    @Test
    fun testRelativize() {
        assertFails {
            Path("/foo").relativeTo(Path("bar"))
        }
        assertNull(Path("/foo").relativeToOrNull(Path("bar")))
        assertEquals(Path("/foo"), Path("/foo").relativeToOrSelf(Path("bar")))

        assertFails {
            UPath("/foo").relativeTo(UPath("bar"))
        }
        assertNull(UPath("/foo").relativeToOrNull(UPath("bar")))
        assertEquals(UPath("/foo"), UPath("/foo").relativeToOrSelf(UPath("bar")))

        assertFails {
            Path("foo").relativeTo(Path("/bar"))
        }
        assertNull(Path("foo").relativeToOrNull(Path("/bar")))
        assertEquals(Path("foo"), Path("foo").relativeToOrSelf(Path("/bar")))

        assertFails {
            UPath("foo").relativeTo(UPath("/bar"))
        }
        assertNull(UPath("foo").relativeToOrNull(UPath("/bar")))
        assertEquals(UPath("foo"), UPath("foo").relativeToOrSelf(UPath("/bar")))

        assertEquals("", Path("/").relativeTo(Path("/")).pathString)
        assertEquals("", Path("").relativeTo(Path("")).pathString)
        assertEquals("", Path("/any").relativeTo(Path("/any")).pathString)
        assertEquals("../../../1/2/3", Path("/foo/bar/1/2/3").relativeTo(Path("/foo/bar/4/5/6")).pathString)
        assertEquals("x", Path("/x").relativeTo(Path("/")).pathString)
        assertEquals("..", Path("/").relativeTo(Path("/x")).pathString)

        assertEquals("", UPath("/").relativeTo(UPath("/")).pathString)
        assertEquals("", UPath("").relativeTo(UPath("")).pathString)
        assertEquals("", UPath("/any").relativeTo(UPath("/any")).pathString)
        assertEquals("../../../1/2/3", UPath("/foo/bar/1/2/3").relativeTo(UPath("/foo/bar/4/5/6")).pathString)
        assertEquals("x", UPath("/x").relativeTo(UPath("/")).pathString)
        assertEquals("..", UPath("/").relativeTo(UPath("/x")).pathString)

        assertEquals("../foo/bar", Path("foo/bar").relativeTo(Path("bar")).pathString)
        assertEquals("../foo/bar", UPath("foo/bar").relativeTo(UPath("bar")).pathString)
        assertEquals("../foo/bar", Path("/foo/bar").relativeTo(Path("/bar")).pathString)
        assertEquals("../foo/bar", UPath("/foo/bar").relativeTo(UPath("/bar")).pathString)

        // Path#relativeTo always normalizes the path
        assertEquals("../a/b/c", Path("/a//b/c/../c").relativeTo(Path("/d")).pathString)
        assertEquals("../a/b/c", UPath("/a//b/c/../c").relativeTo(UPath("/d")).pathString)
        assertEquals("../c", Path("/a/../../c").relativeTo(Path("/d")).pathString)
        assertEquals("../c", UPath("/a/../../c").relativeTo(UPath("/d")).pathString)
    }

    @Test
    fun testSpecial() {
        assertEquals("/usr/bin/%5B", UPath("/usr/bin/[").toUrlPath())
        if ("unix" in Path("test").javaClass.simpleName.lowercase()) {
            println("testing unix path")
            assertEquals(Path("/usr/bin/["), UPath("/usr/bin/[").toNioPath())
        }
    }

    @Test
    fun testTrailingSlash() {
        val name = UPath("1/2")
        val dirName = UPath(
            ByteStringBuilder(name.encoded.size + 1).apply {
                append(name.encoded)
                append('/'.code.toByte())
            }.toByteString()
        )
        assertEquals("1/2/", dirName.pathString)
        assertEquals("1/2", dirName.normalize().pathString)
    }

    @Test
    fun testAllChars() {
        for (ch in 1 .. 255) {
            val p1 = UPath("%%%02x".format(ch), DecodeUrlPath)
            val p2 = p1.toNioPath()
            val p3 = p2.toUPath()
            println(p2.pathString)
            println(p3.toUrlPath())
            assertEquals(p1.encoded, p3.encoded)
        }
    }

    @Test
    fun testRoot() {
        val p1 = UPath("/")
        assertTrue(p1.isAbsolute)
        assertNull(p1.parent)
        assertTrue(p1.segments.count() == 0)
        assertEquals("", p1.extension)
        assertEquals("", p1.name)
        assertEquals("", p1.nameWithoutExtension)
        assertEquals("/", p1.pathString)
        val p2 = p1.toNioPath()
        assertTrue(p2.isAbsolute)
        assertNull(p2.parent)
        assertTrue(p2.nameCount == 0)
        assertEquals("", p2.extension)
        assertEquals("", p2.name)
        assertEquals("", p2.nameWithoutExtension)
        assertEquals("/", p2.pathString)
        println(Path("/bin").toUri().rawPath)
        assertEquals(p1, p2.toUPath())
    }

    @Test
    fun testSingleDot() {
        val p1 = UPath(".")
        assertEquals(".", p1.pathString)
        assertEquals(".", p1.toUrlPath())
        assertNull(p1.parent)
        assertEquals(listOf(p1), p1.segments.toList())
        assertEquals("", p1.extension)
        assertEquals("", p1.nameWithoutExtension)
        assertEquals(".", p1.name)
        assertFalse(p1.isAbsolute)
        val p2 = p1.toNioPath()
        assertEquals(".", p2.pathString)
        assertNull(p2.parent)
        assertEquals(listOf(p2),
            (0 until p2.nameCount).map { p2.getName(it) })
        assertEquals("", p2.extension)
        assertEquals("", p2.nameWithoutExtension)
        assertEquals(".", p2.name)
        assertFalse(p2.isAbsolute)
        assertEquals(p1, p2.toUPath())
    }

    @Test
    fun testEmpty() {
        val p1 = UPath("")
        assertEquals("", p1.pathString)
        assertEquals("", p1.toUrlPath())
        assertNull(p1.parent)
        assertEquals(listOf(p1), p1.segments.toList())
        assertEquals("", p1.extension)
        assertEquals("", p1.nameWithoutExtension)
        assertEquals("", p1.name)
        assertFalse(p1.isAbsolute)
        val p2 = p1.toNioPath()
        assertEquals("", p2.pathString)
        assertNull(p2.parent)
        assertEquals(listOf(p2),
            (0 until p2.nameCount).map { p2.getName(it) })
        assertEquals("", p2.extension)
        assertEquals("", p2.nameWithoutExtension)
        assertEquals("", p2.name)
        assertFalse(p2.isAbsolute)
        println(Path(".").toUPath())
        assertEquals(p1, p2.toUPath())
    }

    @Test
    fun testDotDot() {
        val p1 = UPath("..")
        assertEquals("..", p1.pathString)
        assertEquals("..", p1.toUrlPath())
        assertNull(p1.parent)
        assertEquals(listOf(p1), p1.segments.toList())
        assertEquals("", p1.extension)
        assertEquals(".", p1.nameWithoutExtension)
        assertEquals("..", p1.name)
        assertFalse(p1.isAbsolute)
        val p2 = p1.toNioPath()
        assertEquals("..", p2.pathString)
        assertNull(p2.parent)
        assertEquals(listOf(p2),
            (0 until p2.nameCount).map { p2.getName(it) })
        assertEquals("", p2.extension)
        assertEquals(".", p2.nameWithoutExtension)
        assertEquals("..", p2.name)
        assertFalse(p2.isAbsolute)
        assertEquals(p1, p2.toUPath())
    }
}
