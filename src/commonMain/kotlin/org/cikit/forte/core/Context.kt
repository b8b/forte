package org.cikit.forte.core

sealed class Context<R> {
    abstract val result: R
    abstract fun getVar(name: String): Any?
    abstract fun getCommand(name: String): CommandFunction?
    abstract fun getControl(name: String): ControlFunction?
    abstract fun getOpFunction(name: String): UnaryOpFunction?
    abstract fun getFunction(name: String): UnaryFunction?
    abstract fun getBinaryOpFunction(name: String): BinaryOpFunction?
    abstract fun getBinaryFunction(name: String): BinaryFunction?
    abstract fun getRescueFunction(name: String): BinaryFunction?

    abstract fun builder(): Builder<Unit>

    open fun get(subject: Any?, key: Any?): Any? {
        return when (subject) {
            null -> Undefined("cannot access property $key of null")
            is Map<*, *> -> subject[key] ?: Undefined(
                "$subject does not contain key '$key'"
            )
            is List<*> -> when (key) {
                "size" -> subject.size
                is Int -> subject.getOrElse(key) {
                    Undefined("index out of bounds: $subject[$key]")
                }
                else -> Undefined(
                    "cannot access property '$key' of list $subject"
                )
            }
            else -> Undefined("cannot access property '$key' of '$subject'")
        }
    }

    interface ResultBuilder<R> {
        fun append(value: Any?)
        fun build(): R
    }

    object UnitResultBuilder : ResultBuilder<Unit> {
        override fun append(value: Any?) {
        }

        override fun build() {
        }
    }

    class StringResultBuilder(
        private val target: Appendable = StringBuilder()
    ) : ResultBuilder<String> {
        override fun append(value: Any?) {
            target.append(value.toString())
        }

        override fun build(): String {
            return target.toString()
        }
    }

    class ListResultBuilder(
        private val target: MutableList<Any?> = mutableListOf()
    ) : ResultBuilder<List<Any?>> {
        override fun append(value: Any?) {
            target.add(value)
        }

        override fun build(): List<Any?> {
            return target.toList()
        }
    }

    companion object : Context<Unit>() {
        override val result: Unit get() = Unit
        override fun getVar(name: String): Any =
            Undefined("undefined variable: $name")
        override fun getCommand(name: String): CommandFunction? = null
        override fun getControl(name: String): ControlFunction? = null
        override fun getOpFunction(name: String): UnaryOpFunction? = null
        override fun getFunction(name: String): UnaryFunction? = null
        override fun getBinaryOpFunction(name: String): BinaryOpFunction? = null
        override fun getBinaryFunction(name: String): BinaryFunction? = null
        override fun getRescueFunction(name: String): BinaryFunction? = null
        override fun builder(): Builder<Unit> = Builder(
            rootContext = this,
            resultBuilder = UnitResultBuilder
        )
    }

    class Builder<R> private constructor(
        private val rootContext: Context<*>,
        private val scope: MutableMap<String, Any?> = mutableMapOf(),
        private val resultBuilder: ResultBuilder<R>,
        private val captureFunction: (Any?) -> Unit = resultBuilder::append
    ) : Context<R>() {

        constructor(
            rootContext: Context<*>,
            scope: MutableMap<String, Any?> = mutableMapOf(),
            resultBuilder: ResultBuilder<R>
        ) : this(rootContext, scope, resultBuilder, resultBuilder::append)

        override val result: R
            get() = resultBuilder.build()

        fun setVar(name: String, value: Any?): Builder<R> {
            scope["var_$name"] = value
            return this
        }

        fun setVars(vararg pairs: Pair<String, Any?>): Builder<R> {
            for ((name, value) in pairs) {
                scope["var_$name"] = value
            }
            return this
        }

        fun setVars(pairs: Map<String, Any?>): Builder<R> {
            for ((name, value) in pairs) {
                scope["var_$name"] = value
            }
            return this
        }

        override fun getVar(name: String): Any? {
            if (scope.containsKey("var_$name")) {
                return scope["var_$name"]
            }
            return rootContext.getVar(name)
        }

        fun setCommand(
            name: String,
            implementation: CommandFunction
        ): Builder<R> {
            scope["cmd_$name"] = implementation
            return this
        }

        override fun getCommand(name: String): CommandFunction? {
            return scope["cmd_$name"]
                ?.let { it as CommandFunction }
                ?: rootContext.getCommand(name)
        }

        fun setControl(
            name: String,
            implementation: ControlFunction
        ): Builder<R> {
            scope["control_$name"] = implementation
            return this
        }

        override fun getControl(name: String): ControlFunction? {
            return scope["control_$name"]
                ?.let { it as ControlFunction }
                ?: rootContext.getControl(name)
        }

        fun setOpFunction(
            name: String,
            implementation: UnaryOpFunction
        ): Builder<R> {
            scope["call_$name"] = implementation
            return this
        }

        override fun getOpFunction(name: String): UnaryOpFunction? {
            return scope["call_$name"]
                ?.let { it as UnaryOpFunction }
                ?: rootContext.getOpFunction(name)
        }

        fun setFunction(
            name: String,
            implementation: UnaryFunction
        ): Builder<R> {
            scope["call_$name"] = implementation
            return this
        }

        override fun getFunction(name: String): UnaryFunction? {
            return scope["call_$name"]
                ?.let { it as UnaryFunction }
                ?: rootContext.getFunction(name)
        }

        fun setBinaryOpFunction(
            name: String,
            implementation: BinaryOpFunction
        ): Builder<R> {
            scope["apply_$name"] = implementation
            return this
        }

        override fun getBinaryOpFunction(name: String): BinaryOpFunction? {
            return scope["apply_$name"]
                ?.let { it as BinaryOpFunction }
                ?: rootContext.getBinaryOpFunction(name)
        }

        fun setBinaryFunction(
            name: String,
            implementation: BinaryFunction
        ): Builder<R> {
            scope["apply_$name"] = implementation
            return this
        }

        override fun getBinaryFunction(name: String): BinaryFunction? {
            return scope["apply_$name"]
                ?.let { it as BinaryFunction }
                ?: rootContext.getBinaryFunction(name)
        }

        fun setRescueFunction(
            name: String,
            implementation: BinaryFunction
        ): Builder<R> {
            scope["rescue_$name"] = implementation
            return this
        }

        override fun getRescueFunction(name: String): BinaryFunction? {
            return scope["rescue_$name"]
                ?.let { it as BinaryFunction }
                ?: rootContext.getRescueFunction(name)
        }

        override fun builder(): Builder<Unit> = Builder(
            rootContext,
            scope.toMutableMap(),
            UnitResultBuilder,
        )

        fun scope(): Builder<R> = Builder(
            rootContext,
            scope.toMutableMap(),
            resultBuilder,
            captureFunction
        )

        fun scope(captureFunction: (Any?) -> Unit) = Builder(
            rootContext,
            scope.toMutableMap(),
            UnitResultBuilder,
            captureFunction
        )

        fun <R> scope(resultBuilder: ResultBuilder<R>) = Builder(
            rootContext,
            scope.toMutableMap(),
            resultBuilder
        )

        fun capture(captureFunction: (Any?) -> Unit) = Builder(
            rootContext,
            scope,
            UnitResultBuilder,
            captureFunction
        )

        fun captureToString() = Builder(
            rootContext,
            scope,
            StringResultBuilder()
        )

        fun captureToList() = Builder(
            rootContext,
            scope,
            ListResultBuilder()
        )

        fun emit(value: Any?) = captureFunction(value)

        fun build(): Context<R> {
            return if (rootContext === Companion) {
                MapContext(scope.toMap(), result)
            } else {
                NewContext(rootContext, scope.toMap(), result)
            }
        }
    }

    private class MapContext<R>(
        val scope: Map<String, Any?>,
        override val result: R
    ) : Context<R>() {
        override fun getVar(name: String): Any =
            scope["var_$name"] ?: Undefined("undefined variable: $name")
        override fun getCommand(name: String): CommandFunction? =
            scope["cmd_$name"] as CommandFunction?
        override fun getControl(name: String): ControlFunction? =
            scope["control_$name"] as ControlFunction?
        override fun getOpFunction(name: String): UnaryOpFunction? =
            scope["call_$name"] as UnaryOpFunction?
        override fun getFunction(name: String): UnaryFunction? =
            scope["call_$name"] as UnaryFunction?
        override fun getBinaryOpFunction(name: String): BinaryOpFunction? =
            scope["apply_$name"] as BinaryOpFunction?
        override fun getBinaryFunction(name: String): BinaryFunction? =
            scope["apply_$name"] as BinaryFunction?
        override fun getRescueFunction(name: String): BinaryFunction? =
            scope["rescue_$name"] as BinaryFunction?
        override fun builder(): Builder<Unit> = Builder(
            rootContext = this,
            resultBuilder = UnitResultBuilder
        )
    }

    private class NewContext<R>(
        val rootContext: Context<*>,
        val scope: Map<String, Any?>,
        override val result: R
    ) : Context<R>() {
        override fun getVar(name: String): Any? {
            if (scope.containsKey("var_$name")) {
                return scope["var_$name"]
            }
            return rootContext.getVar(name)
        }

        override fun getCommand(name: String): CommandFunction? {
            return scope["cmd_$name"]
                ?.let { it as CommandFunction }
                ?: rootContext.getCommand(name)
        }

        override fun getControl(name: String): ControlFunction? {
            return scope["control_$name"]
                ?.let { it as ControlFunction }
                ?: rootContext.getControl(name)
        }

        override fun getOpFunction(name: String): UnaryOpFunction? {
            return scope["call_$name"]
                ?.let { it as UnaryOpFunction }
                ?: rootContext.getOpFunction(name)
        }

        override fun getFunction(name: String): UnaryFunction? {
            return scope["call_$name"]
                ?.let { it as UnaryFunction }
                ?: rootContext.getFunction(name)
        }

        override fun getBinaryOpFunction(name: String): BinaryOpFunction? {
            return scope["apply_$name"]
                ?.let { it as BinaryOpFunction }
                ?: rootContext.getBinaryOpFunction(name)
        }

        override fun getBinaryFunction(name: String): BinaryFunction? {
            return scope["apply_$name"]
                ?.let { it as BinaryFunction }
                ?: rootContext.getBinaryFunction(name)
        }

        override fun getRescueFunction(name: String): BinaryFunction? {
            return scope["rescue_$name"]
                ?.let { it as BinaryFunction }
                ?: rootContext.getRescueFunction(name)
        }

        override fun builder(): Builder<Unit> = Builder(
            rootContext = this,
            resultBuilder = UnitResultBuilder
        )
    }
}
