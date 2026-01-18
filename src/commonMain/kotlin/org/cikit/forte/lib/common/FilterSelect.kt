package org.cikit.forte.lib.common

import org.cikit.forte.core.*

class FilterSelect(val cond: Boolean) : FilterMethod {
    override fun invoke(subject: Any?, args: NamedArgs): Any {
        require(subject is Iterable<*>) {
            "invalid operand of type '${typeName(subject)}'"
        }
        return Suspended { ctx ->
            val test: Method
            val testArgs: NamedArgs
            args.use {
                val name = optional<CharSequence>("test") { "true" }
                    .concatToString()
                test = ctx.getMethod(Context.Key.Apply<TestMethod>(name, "is"))
                    ?: error("test '$name' is not defined")
                testArgs = remaining()
            }
            val result = mutableListOf<Any?>()
            for (item in subject) {
                try {
                    var selected = test(item, testArgs)
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
