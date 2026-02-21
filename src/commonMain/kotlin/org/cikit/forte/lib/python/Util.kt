package org.cikit.forte.lib.python

import org.cikit.forte.core.Context

// planned methods
//
// split
//

fun <R> Context.Builder<R>.definePythonExtensions(): Context.Builder<R> {
    defineMethod("keys", ApplyKeys())
    defineMethod("get", ApplyGet())

    defineMethod("startswith", ApplyStartsWith(this))
    defineMethod("endswith", ApplyEndsWith(this))

    return this
}
