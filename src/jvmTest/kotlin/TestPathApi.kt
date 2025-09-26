import org.cikit.forte.core.DecodeEscapes
import org.cikit.forte.core.UPath
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.net.URI
import kotlin.io.path.*
import kotlin.test.assertFails
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * compare UPath API to java.nio.file.Path API.
 *
 * main differences:
 * * UPath is a platform independent "unix path" implementation
 * * Tests presented here might fail where the JVM is not using UnixPath
 * * UPath doesn't implement any functions requiring I/O
 * * UPath clearly isolates the "list of segments" nature of a path
 *     into the `segments` field / property
 */
class TestPathApi {

    @Test
    fun `test getFileSystem not supported`() {
        val nioPath = Path("")
        assertNotNull(nioPath.fileSystem)
    }

    @Test
    fun `test isAbsolute`() {
        assertEquals(Path("abc").isAbsolute, UPath("abc").isAbsolute)
        assertEquals(Path("/abc").isAbsolute, UPath("/abc").isAbsolute)
    }

    @Test
    fun `test getRoot not supported`() {
        // probably fails on win?
        assertEquals(Path("/"), Path("/abc").root)
        assertNull(Path("abc").root)
        // For UPath, the root is always `/`
    }

    @Test
    fun `test lastSegment replaces getFileName`() {
        assertNull(Path("/").fileName)
        assertNull(UPath("/").lastSegment)
        assertEquals(
            Path("").fileName.pathString,
            UPath("").lastSegment?.pathString
        )
    }

    @Test
    fun `test getParent`() {
        assertNull(Path("").parent)
        assertNull(UPath("").parent)
        assertNull(Path("abc").parent)
        assertNull(UPath("abc").parent)
        assertNull(Path("/").parent)
        assertNull(UPath("/").parent)
        assertEquals(
            Path("/abc").parent.pathString,
            UPath("/abc").parent?.pathString
        )
        assertEquals(
            Path("abc/def").parent.pathString,
            UPath("abc/def").parent?.pathString
        )
    }

    @Test
    fun `test getNameCount and getName not supported`() {
        val nioPath = Path("/")

        // there is absolutely no name
        assertEquals(0, nioPath.nameCount)
        assertFails { nioPath.getName(0) }
        assertNull(nioPath.fileName)

        // kotlin extension comes up with a name
        assertEquals("", nioPath.name)

        // UPath only has segments instead of iterable "names"

        val path = UPath("/")
        // there are no path segments
        assertEquals(0, path.segments.count())
        // however, there is a name (returning empty string for compatibility)
        assertEquals("", path.name)
    }

    @Test
    fun `test subpath not implemented`() {
        assertEquals(Path("a/b/c"), Path("/a/b/c").subpath(0, 3))
    }

    @Test
    fun `test startsWith not supported`() {
        // not clear if it applies to "path string" or "list of segments"
        assertFalse(Path("aal/b/c").startsWith("a"))
    }

    @Test
    fun `test endWith not supported`() {
        // not clear if it applies to "path string" or "list of segments"
        assertFalse(Path("a/b/candy").endsWith("andy"))
    }

    @Test
    fun `test normalize`() {
        assertEquals(Path("a"), Path("a//b/..").normalize())
        assertEquals(UPath("a"), UPath("a//b/..").normalize())
    }

    @Test
    fun `test resolve`() {
        assertEquals(Path("a/other"), Path("a").resolve("other"))
        assertEquals(Path("/other"), Path("a").resolve("/other"))
        assertEquals(UPath("a/other"), UPath("a").resolve("other"))
        assertEquals(UPath("/other"), UPath("a").resolve("/other"))
    }

    @Test
    fun `test resolveSibling not supported`() {
        assertEquals(Path("/other"), Path("/some").resolveSibling("other"))
        // as parent is nullable, this function has been omitted to
        // force the user into a decision
        assertEquals(
            UPath("/other"),
            UPath("/other").parent?.resolve("other") ?: UPath("what else?")
        )
    }

    @Test
    fun `test relativize not supported`() {
        // I still don't understand that function
        assertEquals(Path(".."), Path("/some/a").relativize(Path("/some")))
    }

    @Test
    fun `test toUri not supported`() {
        assertEquals("/some+where", Path("/some+where").toUri().rawPath)
        assertEquals("/some%2Bwhere", UPath("/some+where").toUrlPath())
    }

    @Test
    fun `test compareTo`() {
        assertEquals(0, UPath("").compareTo(UPath("")))
        assertTrue(UPath("abc") < UPath("def"))
        assertTrue(UPath("def") > UPath("abc"))
        assertEquals(0, Path("").compareTo(Path("")))
        assertTrue(Path("abc") < Path("def"))
        assertTrue(Path("def") > Path("abc"))
    }

    @Test
    fun `test equals`() {
        assertEquals(Path("a"), Path("a"))
        assertEquals(UPath("a"), UPath("a"))
        assertNotEquals(Path("a") as Any, UPath("a") as Any)
    }

    @Test
    fun `test hashCode`() {
        assertNotEquals(0, Path("a").hashCode())
        assertNotEquals(0, UPath("a").hashCode())
    }

    @Test
    fun `test toString`() {
        assertEquals("a", Path("a").toString())
        assertEquals("/\uFFFD", URI.create("file:///%ff").toPath().toString())
        assertEquals("a", UPath("a").toString())
        assertEquals("/\uFFFD", UPath("/\\777", DecodeEscapes).toString())
    }

    // kotlin extensions

    @Test
    fun `test name`() {
        assertEquals(Path("a").name, UPath("a").name)
    }

    @Test
    fun `test nameWithoutExtension`() {
        assertEquals(
            Path("a.txt").nameWithoutExtension,
            UPath("a.txt").nameWithoutExtension
        )
    }

    @Test
    fun `test extension`() {
        assertEquals(
            Path("a.txt").extension,
            UPath("a.txt").extension
        )
    }

    @Test
    fun `test pathString`() {
        assertEquals(
            Path("/some/where/a.txt").pathString,
            UPath("/some/where/a.txt").pathString
        )
    }

    @Test
    fun `test invariantSeparatorsPathString not supported`() {
        assertEquals(
            Path("/some/where/a.txt").invariantSeparatorsPathString,
            UPath("/some/where/a.txt").pathString
        )
    }

    @Test
    fun `test relativeTo`() {
        assertEquals(
            Path("../a/b/c"),
            Path("/a/b/c").relativeTo(Path("/work"))
        )
        assertEquals(
            UPath("../a/b/c"),
            UPath("/a/b/c").relativeTo(UPath("/work"))
        )
    }

    @Test
    fun `test relativeToOrSelf`() {
        assertEquals(
            Path("a/b/c"),
            Path("a/b/c").relativeToOrSelf(Path("/work"))
        )
        assertEquals(
            UPath("a/b/c"),
            UPath("a/b/c").relativeToOrSelf(UPath("/work"))
        )
    }

    @Test
    fun `test relativeToOrNull`() {
        assertNull(Path("a/b/c").relativeToOrNull(Path("/work")))
        assertNull(UPath("a/b/c").relativeToOrNull(UPath("/work")))
    }

}
