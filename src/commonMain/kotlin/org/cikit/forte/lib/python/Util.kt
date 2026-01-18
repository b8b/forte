package org.cikit.forte.lib.python

import org.cikit.forte.core.Context

// planned methods
//
// split
//

fun <R> Context.Builder<R>.definePythonExtensions(): Context.Builder<R> {
    defineMethod("keys", ApplyKeys())

    defineMethod("startswith", ApplyStartsWith())
    defineMethod("endswith", ApplyEndsWith())

    return this
}
