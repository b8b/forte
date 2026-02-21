package org.cikit.forte.lib.salt

import org.cikit.forte.core.Context
import org.cikit.forte.lib.common.FilterToJson

// interesting salt filters
//
// * avg
// * human_to_bytes
// * mac_str_to_bytes
// * md5, sha1, sha256, sha512
// * path_join
// * regex_match
// * regex_replace
// * regex_search

fun <R> Context.Builder<R>.defineSaltExtensions(): Context.Builder<R> {
    defineMethod("base64_encode", FilterBase64Encode())
    defineMethod("base64_decode", FilterBase64Decode())

    // FIXME replace with what is actually used in salt
    defineMethod("matches_glob", FilterMatchesGlob())
    defineMethod("matches_regex", FilterMatchesRegex())
    defineMethod("regex_replace", FilterRegexReplace())

    defineMethod("json", FilterToJson(this))
    defineMethod("to_json", FilterToJson(this))

    defineMethod("yaml", FilterToYaml())
    defineMethod("to_yaml", FilterToYaml())
    defineMethod("toyaml", FilterToYaml())

    return this
}
