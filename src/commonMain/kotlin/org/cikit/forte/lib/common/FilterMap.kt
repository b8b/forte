package org.cikit.forte.lib.common

import org.cikit.forte.core.*
import org.cikit.forte.lib.core.FilterGet

/**
 * jinja-filters.map(value: Iterable[Any], *args: Any, **kwargs: Any) → Iterable[Any]
 *
 *     Applies a filter on a sequence of objects or looks up an attribute.
 *     This is useful when dealing with lists of objects but you are really
 *     only interested in a certain value of it.
 *
 *     The basic usage is mapping on an attribute.
 *     Imagine you have a list of users but you are only interested in a list of usernames:
 *
 *     Users on this page: {{ users|map(attribute='username')|join(', ') }}
 *
 *     You can specify a default value to use if an object in the list does not have the given attribute.
 *
 *     {{ users|map(attribute="username", default="Anonymous")|join(", ") }}
 *
 *     Alternatively you can let it invoke a filter by passing the name of the
 *     filter and the arguments afterwards. A good example would be applying a
 *     text conversion filter on a sequence:
 *
 *     Users on this page: {{ titles|map('lower')|join(', ') }}
 *
 */
class FilterMap: FilterMethod {
    companion object {
        val NO_DEFAULT = Undefined("no default")
    }

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
            defaultValue = optional("default") { NO_DEFAULT }
        }
        require(subject is Iterable<*>) {
            "invalid operand of type '${typeName(subject)}'"
        }
        return Suspended { ctx ->
            val result = buildList {
                for (item in subject) {
                    try {
                        var mapped = ctx.filterGet(item, getArgs)
                        if (mapped is Suspended) {
                            mapped = mapped.eval(ctx)
                        }
                        if (mapped is Undefined) {
                            if (defaultValue === NO_DEFAULT) {
                                error(mapped.message)
                            }
                            add(defaultValue)
                        } else {
                            add(mapped)
                        }
                    } catch (ex: EvalException) {
                        throw RuntimeException(ex.errorMessage, ex)
                    }
                }
            }
            MaskedList(result)
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
                val key = Context.Key.Apply.create(
                    name,
                    FilterMethod.OPERATOR
                )
                filter = ctx.getMethod(key)
                    ?: error("filter '$name' is not defined")
                filterArgs = remaining()
            }
            val result = buildList {
                for (item in subject) {
                    try {
                        var mapped = filter(item, filterArgs)
                        if (mapped is Suspended) {
                            mapped = mapped.eval(ctx)
                        }
                        if (mapped is Undefined) {
                            error(mapped.message)
                        }
                        add(mapped)
                    } catch (ex: EvalException) {
                        throw RuntimeException(ex.errorMessage, ex)
                    }
                }
            }
            MaskedList(result)
        }
    }
}
