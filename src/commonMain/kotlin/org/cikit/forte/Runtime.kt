package org.cikit.forte

interface Runtime {

    interface Context

    interface CompiledTemplate {
        val template: ParsedTemplate
        val textRepresentation: String? get() = null
    }

    class Command(
        val name: String,
        val args: () -> Any?,
        val body: () -> Unit
    )

    val context: Context

    fun compile(template: ParsedTemplate): CompiledTemplate
    fun exec(template: CompiledTemplate)
    fun exec(template: ParsedTemplate) = exec(compile(template))

    fun startCapture(target: (Any?) -> Unit)
    fun endCapture()

    fun emit(value: Any?)
    fun get(subject: Any?, keys: Array<Any?>): Any?

    fun makeString(nativeArray: Any): String
    fun makeList(nativeArray: Any): List<Any?>
    fun makeMap(nativeEvenArray: Any): Map<Any, Any?>
    fun makeCommand(nativeObject: Any): Command

    fun enterScope()
    fun exitScope(): Context

    fun setVar(name: String, value: Any?)
    fun getVar(name: String): Any? = get(context, arrayOf(name))

    fun setFunction(name: String, implementation: (Any?) -> Any?)
    fun getFunction(name: String): ((Any?) -> Any?)?

    fun setExtension(name: String, implementation: (Any?, () -> Any?) -> Any?)
    fun getExtension(name: String): ((Any?, () -> Any?) -> Any?)?

    fun readArgs(args: Any?, vararg names: String): List<Any?>
}
