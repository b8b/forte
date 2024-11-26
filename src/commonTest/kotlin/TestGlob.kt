import org.cikit.forte.core.Glob
import kotlin.test.*


class TestGlob {

    @Test
    fun testBasic() {
        assertEquals("abc", Glob("abc").toRegex().pattern)
    }

    @Test
    fun testEmptyClass() {
        assertFails {
            Glob("[/]", Glob.Flavor.Build(matchPathName = true))
                .regexPattern
                .getOrThrow()
        }
    }

    @Test
    fun testMatchingPower() {
        assertEquals(Glob.MatchingPower.ALL, Glob("*").matchingPower)
        assertEquals(Glob.MatchingPower.ONE, Glob("foo").matchingPower)
        assertEquals(Glob.MatchingPower.SOME, Glob("foo*").matchingPower)
    }

    @Test
    fun testGitIgnore() {
        assertGitTranspiles("(.*?/)*",                  "")
        assertGitTranspiles("(.*?/)*foo",               "foo")
        assertGitTranspiles("/(.*?/)*a/b/c",            "/**/a/b/c")
        assertGitTranspiles("/[^/]*/a/b/c",             "/*/a/b/c")
        assertGitTranspiles("a/b/c",                    "a/b/c")
        assertGitTranspiles("a/(.*?/)*c",               "a/**/c")
        assertGitTranspiles("a/(.*?/)*c",               "a/***/c")
        assertGitTranspiles("a/a[^/]*/c",               "a/a**/c")
        assertGitTranspiles("\\u0061/a[^/]*/c",         "\\a/a**/c")
        assertGitTranspiles("(.*?/)*\\u002f",           "\\/")
        assertGitTranspiles("(.*?/)*bar/.*",            "**/bar/**")
        assertGitTranspiles("(.*?/)*[\\u005b-\\u005d]", "[[-\\]]")
        assertGitTranspiles("[^/]*/bar/.*",             "*/bar/**")
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
