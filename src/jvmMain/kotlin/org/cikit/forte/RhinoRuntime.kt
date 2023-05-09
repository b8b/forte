package org.cikit.forte

import org.mozilla.javascript.*

class RhinoRuntime : Runtime {

    class Context(
        val vars: MutableMap<String, Any?> = mutableMapOf(),
        val functions: MutableMap<String, Any> = mutableMapOf()
    ) : Runtime.Context

    class CompiledTemplate(
        override val template: ParsedTemplate,
        override val textRepresentation: String,
        val jsFunc: NativeFunction
    ) : Runtime.CompiledTemplate

    private val rhCtx = org.mozilla.javascript.Context.enter()
    private val rhScope = rhCtx.initStandardObjects()

    private val module = NativeObject()
    private val dynamicRt: NativeObject
    private val captureFunctions = ArrayDeque<(Any?) -> Unit>()
    private val scopes = ArrayDeque(listOf(Context()))

    init {
        rhScope.defineProperty("module", module, 0)
        rhCtx.evaluateString(
            rhScope,
            "module.${::dynamicRt.name} = {};",
            "${javaClass.simpleName}::init",
            1,
            null
        )
        dynamicRt = module[::dynamicRt.name] as NativeObject
        dynamicRt.defineProperty(::context.name, scopes.last(), 0)
        dynamicRt.defineProperty(
            ::emit.name,
            Callable { _, _, _, args ->
                val (arg0) = args
                emit(arg0)
            },
            0
        )
        dynamicRt.defineProperty(
            ::get.name,
            Callable { _, _, _, args ->
                val (arg0, arg1) = args
                arg1 as NativeArray
                get(arg0, arg1.toTypedArray())
            },
            0
        )
        dynamicRt.defineProperty(
            ::makeString.name,
            Callable { _, _, _, args ->
                val (arg0) = args
                makeString(arg0)
            },
            0
        )
        dynamicRt.defineProperty(
            ::makeList.name,
            Callable { _, _, _, args ->
                val (arg0) = args
                makeList(arg0)
            },
            0
        )
        dynamicRt.defineProperty(
            ::makeMap.name,
            Callable { _, _, _, args ->
                val (arg0) = args
                makeMap(arg0)
            },
            0
        )
        dynamicRt.defineProperty(
            ::makeCommand.name,
            Callable { _, _, _, args ->
                val (arg0) = args
                makeCommand(arg0)
            },
            0
        )
    }

    override val context: Context get() = scopes.last()

    override fun compile(template: ParsedTemplate): CompiledTemplate {
        val jsSource = template.transpileToJs()
        val templatePath = template.path?.toString() ?: "anonymous"
        rhCtx.evaluateString(
            rhScope,
            jsSource,
            "$templatePath.js",
            1,
            null
        )
        val jsFunc =
            (module["exports"] as NativeObject)["exec"] as NativeFunction

        return CompiledTemplate(
            template = template,
            textRepresentation = jsSource,
            jsFunc = jsFunc
        )
    }

    override fun exec(template: Runtime.CompiledTemplate) {
        exec(template as CompiledTemplate)
    }

    fun exec(template: CompiledTemplate) {
        template.jsFunc.call(
            rhCtx,
            rhScope,
            null,
            arrayOf(dynamicRt)
        )
    }

    override fun startCapture(target: (Any?) -> Unit) {
        captureFunctions += target
    }

    override fun endCapture() {
        captureFunctions.removeLast()
    }

    override fun emit(value: Any?) {
        captureFunctions.lastOrNull()?.invoke(value)
    }

    override fun get(subject: Any?, keys: Array<Any?>): Any? {
        var result = if (subject === context) context.vars else subject
        for (key in keys) {
            result = when (result) {
                is Map<*, *> -> result[key]
                is List<*> -> when (key) {
                    "size" -> result.size
                    is Int -> result[key]
                    is Double -> if (key.toInt().toDouble() == key) {
                        result[key.toInt()]
                    } else {
                        error("invalid key for array: $key")
                    }
                    else -> error("invalid key for array: $key")
                }
                else -> null
            }
            if (result == null) {
                break
            }
        }
        if (subject == context) {
            context.vars["__trace_get"]?.let { traceGet ->
                @Suppress("UNCHECKED_CAST") run {
                    traceGet as MutableMap<Any, Any?>
                }.put(keys.toList().toString(), result)
            }
        }
        return result
    }

    override fun makeString(nativeArray: Any): String {
        val result = StringBuilder()
        require(nativeArray is NativeArray)
        for (index in (0 until nativeArray.size)) {
            result.append(nativeArray[index].toString())
        }
        return result.toString()
    }

    override fun makeList(nativeArray: Any): List<Any?> {
        require(nativeArray is NativeArray)
        return (0 until nativeArray.size).map { index ->
            when (val v = nativeArray[index]) {
                0.0 -> 0
                1.0 -> 1
                else -> {
                    if (v is Double && v.toInt().toDouble() == v) {
                        v.toInt()
                    } else {
                        v
                    }
                }
            }
        }
    }

    override fun makeMap(nativeEvenArray: Any): Map<Any, Any?> {
        require(nativeEvenArray is NativeArray &&
                (nativeEvenArray.size and 1) == 0)
        return (0 until nativeEvenArray.size / 2).associate { index ->
            val key = nativeEvenArray[index * 2]
            val value = nativeEvenArray[index * 2 + 1]
            require(key != null)
            key to if (value == 1.0) 1 else value
        }
    }

    override fun makeCommand(nativeObject: Any): Runtime.Command {
        require(nativeObject is NativeObject)
        val command = nativeObject["command"] as String
        val args = nativeObject["args"] as ArrowFunction
        val body = nativeObject["body"] as ArrowFunction
        val argsCall = {
            args.call(
                rhCtx,
                rhScope,
                null,
                arrayOf()
            )
        }
        val bodyCall = {
            body.call(
                rhCtx,
                rhScope,
                null,
                arrayOf()
            )
            Unit
        }
        return Runtime.Command(command, argsCall, bodyCall)
    }

    override fun enterScope() {
        val currentContext = scopes.last()
        val newContext = Context(
            vars = currentContext.vars.toMutableMap(),
            functions = currentContext.functions.toMutableMap()
        )
        scopes.addLast(newContext)
        dynamicRt.defineProperty(::context.name, newContext, 0)
    }

    override fun exitScope(): Context {
        val removed = scopes.removeLast()
        // restore context
        dynamicRt.defineProperty(::context.name, scopes.last(), 0)
        // restore functions
        for ((k, v) in scopes.last().functions) {
            dynamicRt.defineProperty(k, (v as Pair<*, *>).second, 0)
        }
        for ((k, _) in removed.functions) {
            if (!scopes.last().functions.containsKey(k)) {
                dynamicRt.delete(k)
            }
        }
        return removed
    }

    override fun setVar(name: String, value: Any?) {
        context.vars[name] = value
    }

    override fun setFunction(name: String, implementation: (Any?) -> Any?) {
        require(name.startsWith("call_") ||
                name.startsWith("cmd_") ||
                name.startsWith("control_"))
        val callable = Callable { _, _, _, args ->
            require(args.size == 1) {
                "invalid number of arguments in function call: ${args.size}"
            }
            implementation(if (args[0] == 1.0) 1 else args[0])
        }
        dynamicRt.defineProperty(name, callable, 0)
        context.functions[name] = implementation to callable
    }

    override fun getFunction(name: String): ((Any?) -> Any?)? {
        require(name.startsWith("call_") ||
                name.startsWith("cmd_") ||
                name.startsWith("control_"))
        @Suppress("UNCHECKED_CAST") run {
            return (context.functions[name] as Pair<*, *>?)
                ?.first as ((Any?) -> Any?)?
        }
    }

    override fun setExtension(
        name: String,
        implementation: (Any?, () -> Any?) -> Any?
    ) {
        require(name.startsWith("apply_"))
        val callable = Callable { ctx, scope, thisObj, args ->
            require(args.size == 2) {
                "invalid number of arguments in extension call: ${args.size}"
            }
            val subject = when (val s = args[0]) {
                is NativeJavaObject -> s.unwrap()
                else -> s
            }
            val namedArgs = args[1] as ArrowFunction
            implementation(if (subject == 1.0) 1 else subject) {
                namedArgs.call(ctx, scope, thisObj, emptyArray())
            }
        }
        dynamicRt.defineProperty(name, callable, 0)
        context.functions[name] = implementation to callable
    }

    override fun getExtension(name: String): ((Any?, () -> Any?) -> Any?)? {
        require(name.startsWith("apply_"))
        val func = (context.functions[name] as? Pair<*, *>?)
            ?.first
            ?: return null
        @Suppress("UNCHECKED_CAST") run {
            return func as (Any?, () -> Any?) -> Any?
        }
    }

    override fun readArgs(args: Any?, vararg names: String): List<Any?> {
        val keys = (args as NativeObject).keys.map { it.toString() }.toSet()
        val result = mutableMapOf<String, Any?>()
        val positionalNames = mutableSetOf<String>()
        for (pos in names.indices) {
            val key = pos.toString()
            if (key !in keys) {
                break
            }
            val v: Any? = args[pos]
            val name = names[pos]
            positionalNames += name
            result[name] = v
        }
        for (n in names) {
            if (n in positionalNames) {
                continue
            }
            val key = n.trimEnd { ch -> !ch.isLetterOrDigit() }
            val type = n.substring(key.length)
            if (key in keys) {
                require(!result.containsKey(n))
                result[n] = args[key]
            } else if (!type.endsWith("?")) {
                error("missing required argument $key in $keys")
            }
        }
        return names.map { name ->
            val v = when (val s = result[name]) {
                is NativeJavaObject -> s.unwrap()
                else -> s
            }
            if (v == 1.0) 1 else v
        }
    }

}
