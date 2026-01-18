package org.cikit.forte.lib.common

import org.cikit.forte.core.*
import org.cikit.forte.lib.core.FilterGet

class FilterSelectAttr private constructor(
    private val get: FilterMethod,
    val cond: Boolean
) : FilterMethod, DependencyAware {

    constructor(ctx: Context<*>, cond: Boolean) : this(ctx.filterGet, cond)

    override fun withDependencies(ctx: Context<*>): FilterSelectAttr {
        val get = ctx.filterGet
        return if (get === this.get) {
            this
        } else {
            FilterSelectAttr(get, cond)
        }
    }

    override fun invoke(subject: Any?, args: NamedArgs): Any {
        require(subject is Iterable<*>) {
            "invalid operand of type '${typeName(subject)}'"
        }
        return Suspended { ctx ->
            val getArgs: NamedArgs
            val test: Method
            val testArgs: NamedArgs
            args.use {
                getArgs = NamedArgs(
                    listOf(requireAny("attribute")),
                    FilterGet.singleArg
                )
                val name = optional<CharSequence>("test") { "defined" }
                    .concatToString()
                test = ctx.getMethod(Context.Key.Apply<TestMethod>(name, "is"))
                    ?: error("filter '$name' is not defined")
                testArgs = remaining()
            }
            val result = mutableListOf<Any?>()
            for (item in subject) {
                try {
                    var mapped = get(item, getArgs)
                    if (mapped is Suspended) {
                        mapped = mapped.eval(ctx)
                    }
                    var selected: Any? = test(mapped, testArgs)
                    if (selected is Suspended) {
                        selected = selected.eval(ctx)
                    }
                    if (selected == cond) {
                        result.add(item)
                    }
                } catch (ex: EvalException) {
                    throw RuntimeException(ex.errorMessage, ex)
                }
            }
            result.toList()
        }
    }
}
