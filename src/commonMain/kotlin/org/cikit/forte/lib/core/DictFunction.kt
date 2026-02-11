package org.cikit.forte.lib.core

import org.cikit.forte.core.Context
import org.cikit.forte.core.Function
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.typeName

class DictFunction : Function {
    companion object {
        val KEY = Context.Key.Call("dict")
    }

    override fun invoke(args: NamedArgs): Any? {
        val positionalCount = args.values.size - args.names.size
        val dict: Any?
        val pairs: NamedArgs
        args.use {
            when (positionalCount) {
                0 -> {
                    dict = emptyMap<String, Any?>()
                    pairs = remaining()
                }
                1 -> {
                    dict = requireAny("")
                    pairs = remaining()
                }
                else -> {
                    dict = requireAny("")
                    pairs = NamedArgs.Empty
                    // will fail for positionalCount > 1
                }
            }
        }
        if (dict is Map<*, *>) {
            if (pairs.values.isEmpty()) {
                return dict
            }
            // should be 0
            val offset = pairs.values.size - pairs.names.size
            return buildMap {
                for ((k, v) in dict) {
                    put(k, v)
                }
                for (i in pairs.names.indices) {
                    val k = pairs.names[i]
                    val v = pairs.values[i + offset]
                    put(k, v)
                }
            }
        }
        if (dict !is Iterable<*>) {
            throw IllegalArgumentException(
                "invalid operand of type '${typeName(dict)}'"
            )
        }
        val varNames = listOf("k", "v")
        return buildMap {
            for (item in dict) {
                val unpacked = unpackList(varNames, item)
                put(unpacked[0].second, unpacked[1].second)
            }
            for (i in pairs.names.indices) {
                val k = pairs.names[i]
                val v = pairs.values[i - positionalCount]
                put(k, v)
            }
        }
    }
}
