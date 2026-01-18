package org.cikit.forte.lib.common

import org.cikit.forte.core.*
import org.cikit.forte.lib.core.FilterGet

class FilterMap: FilterMethod {
    override fun invoke(subject: Any?, args: NamedArgs): Any {
        return if ("attribute" in args.names) {
            mapAttr(subject, args)
        } else {
            mapFilter(subject, args)
        }
    }

    private fun mapAttr(subject: Any?, args: NamedArgs): Any {
        val getArgs: NamedArgs
        val defaultValue: Any?
        args.use {
            getArgs = NamedArgs(
                listOf(requireAny("attribute")),
                FilterGet.singleArg
            )
            defaultValue = optional("default") { Undefined("no default") }
        }
        require(subject is Iterable<*>) {
            "invalid operand of type '${typeName(subject)}'"
        }
        return Suspended { ctx ->
            val result = mutableListOf<Any?>()
            for (item in subject) {
                try {
                    var mapped = ctx.filterGet(item, getArgs)
                    if (mapped is Suspended) {
                        mapped = mapped.eval(ctx)
                    }
                    if (mapped is Undefined) {
                        if (defaultValue !is Undefined) {
                            result.add(defaultValue)
                        }
                    } else {
                        result.add(mapped)
                    }
                } catch (ex: EvalException) {
                    throw RuntimeException(ex.errorMessage, ex)
                }
            }
            result.toList()
        }
    }

    private fun mapFilter(subject: Any?, args: NamedArgs): Any {
        require(subject is Iterable<*>) {
            "invalid operand of type '${typeName(subject)}'"
        }
        return Suspended { ctx ->
            val filter: Method
            val filterArgs: NamedArgs
            args.use {
                val name = require<CharSequence>("filter").concatToString()
                filter = ctx.getMethod(Context.Key.Apply(name, "pipe"))
                    ?: error("filter '$name' is not defined")
                filterArgs = remaining()
            }
            val result = mutableListOf<Any?>()
            for (item in subject) {
                try {
                    var mapped = filter(item, filterArgs)
                    if (mapped is Suspended) {
                        mapped = mapped.eval(ctx)
                    }
                    if (mapped !is Undefined) {
                        result.add(mapped)
                    }
                } catch (ex: EvalException) {
                    throw RuntimeException(ex.errorMessage, ex)
                }
            }
            result.toList()
        }
    }
}
