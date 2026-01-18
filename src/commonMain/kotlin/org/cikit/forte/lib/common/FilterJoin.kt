package org.cikit.forte.lib.common

import org.cikit.forte.core.*
import org.cikit.forte.lib.core.FilterGet

class FilterJoin: FilterMethod {
    override fun invoke(subject: Any?, args: NamedArgs): Any {
        val getArgs: NamedArgs?
        val separator: CharSequence
        args.use {
            separator = optional("d") { "" }
            getArgs = optionalNullable(
                "attribute",
                { attribute ->
                    NamedArgs(listOf(attribute), FilterGet.singleArg)
                },
                { null }
            )
        }
        require(subject is Iterable<*>) {
            "invalid operand of type '${typeName(subject)}'"
        }
        return Suspended { ctx ->
            val result = StringBuilder()
            var first = true
            for (item in subject) {
                if (first) {
                    first = false
                } else {
                    result.append(separator)
                }
                var mapped = item
                if (getArgs != null) {
                    mapped = ctx.filterGet(mapped, getArgs)
                    if (mapped is Suspended) {
                        mapped = mapped.eval(ctx)
                    }
                    if (mapped is Undefined) {
                        return@Suspended mapped
                    }
                }
                if (mapped !is CharSequence) {
                    mapped = ctx.filterString(mapped, NamedArgs.Empty)
                    if (mapped is Suspended) {
                        mapped = mapped.eval(ctx)
                    }
                    if (mapped is Undefined) {
                        return@Suspended mapped
                    }
                }
                require(mapped is CharSequence) {
                    "invalid type '${typeName(mapped)}' returned " +
                            "from filter 'string': " +
                            "expected CharSequence"
                }
                result.append(mapped)
            }
            result.toString()
        }
    }
}
