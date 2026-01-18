package org.cikit.forte.internal

import org.cikit.forte.core.Operation
import org.cikit.forte.core.Undefined
import org.cikit.forte.parser.Expression

internal val UNCOMPILED_EXPRESSION = listOf(
    Operation.Const(
        Expression.Malformed(emptyList()),
        Undefined("uncompiled expression")
    )
)
