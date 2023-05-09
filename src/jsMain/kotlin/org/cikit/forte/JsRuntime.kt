package org.cikit.forte

import kotlin.js.json

class JsRuntime : Runtime {

    class Context(
        val vars: MutableMap<String, Any?> = mutableMapOf(),
        val functions: MutableMap<String, Any> = mutableMapOf(),
    ) : Runtime.Context {
        constructor(parent: Context) : this(
            parent.vars.toMutableMap(),
            parent.functions.toMutableMap()
        )
    }

    class CompiledTemplate internal constructor(
        override val template: ParsedTemplate,
        val jsSource: String,
        val jsFunc: dynamic
    ) : Runtime.CompiledTemplate

    private val dynamicRt: dynamic = json()
    private val captureFunctions = ArrayDeque<(Any?) -> Unit>()
    private val scopes = ArrayDeque(listOf(Context()))

    init {
        dynamicRt.context = scopes.last()
        dynamicRt.emit = ::emit
        dynamicRt.get = ::get
        dynamicRt.makeString = ::makeString
        dynamicRt.makeList = ::makeList
        dynamicRt.makeMap = ::makeMap
        dynamicRt.makeCommand = ::makeCommand
    }

    override val context: Context get() = scopes.last()

    override fun compile(template: ParsedTemplate): CompiledTemplate {
        val jsSource = template.transpileToJs()
        val jsFunc = eval(jsSource)
        return CompiledTemplate(template, jsSource, jsFunc)
    }

    override fun exec(template: Runtime.CompiledTemplate) {
        exec(template as CompiledTemplate)
    }

    fun exec(template: CompiledTemplate) {
        template.jsFunc.exec(dynamicRt)
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
                    else -> error("invalid key for array: $key")
                }
                else -> null
            }
            if (result == null) {
                break
            }
        }
        if (subject == context && context.vars.containsKey("__trace_get")) {
            val traceGet = context.vars["__trace_get"]
                .unsafeCast<MutableMap<String, Any?>>()
            traceGet[keys.toList().toString()] = result
        }
        return result
    }

    override fun makeString(nativeArray: Any): String {
        val result = StringBuilder()
        val length = nativeArray.asDynamic().length as Int
        for (index in (0 until length)) {
            result.append(nativeArray.asDynamic()[index].toString())
        }
        return result.toString()
    }

    override fun makeList(nativeArray: Any): List<Any?> {
        val result = mutableListOf<Any?>()
        val length = nativeArray.asDynamic().length as Int
        for (index in (0 until length)) {
            result.add(nativeArray.asDynamic()[index])
        }
        return result.toList()
    }

    override fun makeMap(nativeEvenArray: Any): Map<Any, Any?> {
        val result = mutableMapOf<Any, Any?>()
        val length = nativeEvenArray.asDynamic().length as Int
        require(length and 1 == 0)
        for (index in (0 until length / 2)) {
            val key = nativeEvenArray.asDynamic()[index * 2]
            val value = nativeEvenArray.asDynamic()[index * 2 + 1]
            result[key as Any] = value
        }
        return result.toMap()
    }

    override fun makeCommand(nativeObject: Any): Runtime.Command {
        val command = nativeObject.asDynamic().command as String
        val args = nativeObject.asDynamic().args
        val body = nativeObject.asDynamic().body
        return Runtime.Command(
            command,
            { args() },
            { body(); Unit }
        )
    }

    override fun enterScope() {
        val currentContext = scopes.last()
        val newContext = Context(currentContext)
        scopes.addLast(newContext)
        dynamicRt.context = newContext
    }

    override fun exitScope(): Context {
        val removed = scopes.removeLast()
        // restore context
        dynamicRt.context = scopes.last()
        // restore functions
        for ((k, v) in scopes.last().functions) {
            dynamicRt[k] = v
        }
        for ((k, _) in removed.functions) {
            if (!scopes.last().functions.containsKey(k)) {
                dynamicRt[k] = undefined
            }
        }
        return removed
    }

    override fun setVar(name: String, value: Any?) {
        context.vars[name] = value
    }

    override fun setFunction(
        name: String, implementation: (Any?) -> Any?
    ) {
        require(name.startsWith("call_") ||
                name.startsWith("cmd_") ||
                name.startsWith("control_"))
        context.functions[name] = implementation
        dynamicRt[name] = implementation
    }

    override fun getFunction(name: String): ((Any?) -> Any?)? {
        require(name.startsWith("call_") ||
                name.startsWith("cmd_") ||
                name.startsWith("control_"))
        val func = context.functions[name] ?: return null
        return func.unsafeCast<(Any?) -> Any?>()
    }

    override fun setExtension(
        name: String,
        implementation: (Any?, () -> Any?) -> Any?
    ) {
        require(name.startsWith("apply_"))
        context.functions[name] = implementation
        dynamicRt[name] = implementation
    }

    override fun getExtension(name: String): ((Any?, () -> Any?) -> Any?)? {
        require(name.startsWith("apply_"))
        val func = context.functions[name] ?: return null
        return func.unsafeCast<(Any?, () -> Any?) -> Any?>()
    }

    override fun readArgs(args: Any?, vararg names: String): List<Any?> {
        val keys = (js("Object.keys")(args) as Array<Any?>)
            .map { it.toString() }
            .toSet()
        val result = mutableMapOf<String, Any?>()
        val positionalNames = mutableSetOf<String>()
        for (pos in names.indices) {
            val key = pos.toString()
            if (key !in keys) {
                break
            }
            val v: Any? = args.asDynamic()[pos]
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
                result[n] = args.asDynamic()[key]
            } else if (!type.endsWith("?")) {
                error("missing required argument $key in $keys")
            }
        }
        return names.map { name -> result[name] }
    }

}
