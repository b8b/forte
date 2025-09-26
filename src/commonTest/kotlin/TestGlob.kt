import org.cikit.forte.core.DecodeUrlPath
import org.cikit.forte.core.Glob
import org.cikit.forte.core.UPath
import org.cikit.forte.core.asGLobMatcher
import kotlin.test.*


class TestGlob {

    @Test
    fun testBasic() {
        assertEquals("abc", Glob("abc").toRegex().pattern)
        assertEquals("abc", Glob("abc").pattern)
        assertTrue(Glob("Icon\r").toRegex().matches("Icon\r"))
        assertTrue(Glob("abc\ndef", ignoreCase = true).toRegex().matches("ABC\ndef"))
        assertEquals("Icon\\\$", Glob("Icon\$").regexPattern.getOrThrow())
        assertEquals("Icon\\^", Glob("Icon^").regexPattern.getOrThrow())
    }

    @Test
    fun testEmptyClass() {
        assertFails {
            Glob("[/]", Glob.Flavor.Build(matchPathName = true))
                .regexPattern
                .getOrThrow()
        }
    }

    private class AssertedGlob(
        val glob: Glob,
        val regex: Regex = glob.regex.getOrThrow()
    )

    private fun Glob.assertRegexPattern(
        pattern: String,
        requireAbsolutePath: Boolean = false,
        requireRelativePath: Boolean = false,
        requireDirectory: Boolean = false,
        requireNormalized: Boolean = true
    ): AssertedGlob {
        assertEquals(pattern, regexPattern.getOrThrow())
        assertEquals(requireAbsolutePath, matchingProperties.requireAbsolutePath)
        assertEquals(requireRelativePath, matchingProperties.requireRelativePath)
        assertEquals(requireDirectory, matchingProperties.requireDirectory)
        assertEquals(requireNormalized, matchingProperties.requireNormalized)
        return AssertedGlob(this)
    }

    private fun AssertedGlob.assertMatchesOne(
        sample: String,
        andNot: Iterable<String> = emptyList()
    ) {
        assertEquals(Glob.MatchingPower.ONE, glob.matchingPower)
        assertTrue(regex.matches(sample))
        assertFalse(regex.matches("${sample}x"))
        assertFalse(regex.matches("x$sample"))
        for (s in andNot) {
            assertFalse(regex.matches(s))
        }
    }

    private fun AssertedGlob.assertMatchesOne(
        sample: String,
        andNot: String
    ) = assertMatchesOne(sample, listOf(andNot))

    private fun AssertedGlob.assertMatchesAll() {
        assertEquals(Glob.MatchingPower.ALL, glob.matchingPower)
        assertTrue(regex.matches(""))
        assertTrue(regex.matches(" "))
        assertTrue(regex.matches("x"))
        assertTrue(regex.matches("xyz"))
    }

    private fun AssertedGlob.assertMatchesSome(
        vararg samples: String,
        andNot: Iterable<String> = emptyList()
    ) {
        assertEquals(Glob.MatchingPower.SOME, glob.matchingPower)
        for (s in samples) {
            assertTrue(regex.matches(s))
        }
        for (s in andNot) {
            assertFalse(regex.matches(s))
        }
    }

    private fun AssertedGlob.assertMatchesSome(
        vararg samples: String,
        andNot: String
    ) = assertMatchesSome(*samples, andNot = listOf(andNot))

    @Test fun matchNonEmpty() = Glob("*?*")
        .assertRegexPattern(".*..*")
        .assertMatchesSome("a", "abc", "xy", andNot = "")

    @Test fun matchAll() = Glob("***", Glob.Flavor.Git)
        .assertRegexPattern(".*")
        .assertMatchesAll()

    @Test fun matchNonEmptyGit() = Glob("*?*", Glob.Flavor.Git)
        .assertRegexPattern("[^/]*[^/][^/]*")
        .assertMatchesSome("a", "abc", "xy", andNot = "")

    @Test fun matchOneRelative() = Glob("foo")
        .assertRegexPattern("foo")
        .assertMatchesOne("foo")

    @Test fun matchPrefix() = Glob("foo*")
        .assertRegexPattern("foo.*")
        .assertMatchesSome("foo", "foo1", "foo1a", andNot = "1foo")

    @Test fun matchOneAbsolute() = Glob("/foo", Glob.Flavor.Git)
        .assertRegexPattern("/foo", requireAbsolutePath = true)
        .assertMatchesOne("/foo")

    @Test fun matchOneNotNormalized() = Glob("//foo", Glob.Flavor.Git)
        .assertRegexPattern(
            "//foo",
            requireAbsolutePath = true,
            requireNormalized = false
        )
        .assertMatchesOne("//foo", andNot = "/foo")

    @Test fun matchRelativeSubPath() = Glob("a/foo", Glob.Flavor.Git)
        .assertRegexPattern("a/foo", requireRelativePath = true)
        .assertMatchesOne("a/foo", andNot = "a//foo")

    @Test fun matchRelativeDirWithSingleCharSuffix() =
        Glob("a/foo?/", Glob.Flavor.Git)
            .assertRegexPattern(
                "a/foo[^/]/",
                requireRelativePath = true,
                requireDirectory = true
            )
            .assertMatchesSome("a/foo1/", "a/foo2/", andNot = "a/foo/")

    @Test
    fun matchNotNormalizedRelativeDirWithSingleCharSuffix() =
        Glob("a/foo?//", Glob.Flavor.Git)
            .assertRegexPattern(
                "a/foo[^/]//",
                requireRelativePath = true,
                requireDirectory = true,
                requireNormalized = false
            )
            .assertMatchesSome("a/foo1//", "a/foo2//")

    @Test
    fun testGitIgnore() {
        assertGitTranspiles("",                         "")
        assertGitTranspiles("foo",                      "foo")
        assertGitTranspiles("/(.*?/)*a/b/c",            "/**/a/b/c")
        assertGitTranspiles("/[^/]*/a/b/c",             "/*/a/b/c")
        assertGitTranspiles("a/b/c",                    "a/b/c")
        assertGitTranspiles("a/(.*?/)*c",               "a/**/c")
        assertGitTranspiles("a/(.*?/)*c",               "a/***/c")
        assertGitTranspiles("a/a[^/]*/c",               "a/a**/c")
        assertGitTranspiles("\\u0061/a[^/]*/c",         "\\a/a**/c")
        assertGitTranspiles("(.*?/)*bar/.*",            "**/bar/**")
        assertGitTranspiles("[\\u005b-\\u005d]",        "[[-\\]]")
        assertGitTranspiles("[^/]*/bar/.*",             "*/bar/**")
    }

    @Test
    fun testEscapedSeparator() {
        assertFails {
            Glob("\\/").toRegex()
        }
    }

    @Test
    fun testGitLeadingStar() {
        assertGitMatchesNot(input = "bar/baz/foo", pattern = "*/foo")
    }

    @Test
    fun testGitBackslashWithinClass() {
        assertGitMatches(input = "-", pattern = "[\\-_]")
    }

    @Test
    fun testGitTrailingStarStar1() {
        assertGitMatches(input = "deep/foo/bar/baz/", pattern = "**/bar/**")
    }

    @Test
    fun testGitTrailingStarStar2() {
        assertGitMatches(input = "foo/bar/baz/x", pattern = "*/bar/**")
    }

    @Test
    fun testGitCollapseDoubleStars() {
        assertGitMatches(input = "foo/baz/bar", pattern = "foo/**/**/bar")
    }

    @Test
    fun testGitExplicitAnyLevel() {
        assertGitMatches(input = "foo", pattern = "**/foo")
    }

    @Test
    fun testSingleBackslash() {
        // git expects no match
        assertGitFails(input = "\\", pattern = "\\")
    }

    @Test
    fun testUnclosedClass1() {
        // git expects no match
        assertGitFails(input = "ab", pattern = "a[]b")
    }

    @Test
    fun testUnclosedClass2() {
        // git expects no match
        assertGitFails(input = "-", pattern = "[a-")
    }

    @Test
    fun testUnclosedClass3() {
        // git expects no match
        assertGitFails(input = "\\", pattern = "[\\]")
    }

    @Test
    fun matchBinary() {
        val pattern = "abc%c0%af%c1.*"
        val sample = "abc%c0%af%c1.txt"
        val glob = UPath(pattern, DecodeUrlPath).asGLobMatcher()
        assertTrue(glob.matches(UPath(sample, DecodeUrlPath)))
    }

    private fun assertGitTranspiles(regex: String, glob: String) {
        assertEquals(regex, gitGlob(glob).toRegex().pattern)
    }

    private fun assertGitMatches(input: String, pattern: String) {
        val glob = gitGlob(pattern)
        val regex = glob.toRegex()
        assertTrue(regex.matches(input))
    }

    private fun assertGitMatchesNot(input: String, pattern: String) {
        val glob = gitGlob(pattern)
        val regex = glob.toRegex()
        assertFalse(regex.matches(input))
    }

    private fun assertGitFails(input: String, pattern: String) {
        val glob = gitGlob(pattern)
        assertFails {
            val regex = glob.toRegex()
            regex.matches(input)
        }
    }

    private fun gitGlob(pattern: String) = Glob(pattern, Glob.Flavor.Git)
}
