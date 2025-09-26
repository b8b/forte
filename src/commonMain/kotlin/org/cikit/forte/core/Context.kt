package org.cikit.forte.core

import kotlinx.coroutines.flow.FlowCollector

sealed class Context<R> {
    abstract val result: R
    abstract fun getVar(name: String): Any?
    @Suppress("DEPRECATION")
    @Deprecated("migrate to suspending api")
    abstract fun getCommand(name: String): CommandFunction?
    @Suppress("DEPRECATION")
    @Deprecated("migrate to suspending api")
    abstract fun getControl(name: String): ControlFunction?
    abstract fun getCommandTag(name: String): CommandTag?
    abstract fun getControlTag(name: String): ControlTag?
    abstract fun getOpFunction(name: String): UnOpFunction?
    abstract fun getBinaryOpFunction(name: String): BinOpFunction?
    abstract fun getFunction(name: String): Function?
    abstract fun getMethod(name: String, operator: String = "invoke"): Method?
    abstract fun getRescueMethod(name: String, operator: String = "invoke"): Method?

    abstract fun builder(): Builder<Unit>

    fun get(subject: Any?, key: Any?): Any? {
        val getMethod = getMethod("get", "pipe")
        return if (getMethod == null) {
            Undefined("get filter function not defined")
        } else {
            getMethod.invoke(this, subject, NamedArgs(listOf(key), listOf("key")))
        }
    }

    @Deprecated("migrate to flow api")
    interface ResultBuilder<R> {
        fun append(value: Any?)
        fun build(): R
    }

    @Suppress("DEPRECATION")
    @Deprecated("migrate to flow api")
    object UnitResultBuilder : ResultBuilder<Unit> {
        override fun append(value: Any?) {
        }

        override fun build() {
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated("migrate to flow api")
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

    @Suppress("DEPRECATION")
    @Deprecated("migrate to flow api")
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
            Undefined("undefined variable: '$name'")
        @Suppress("DEPRECATION")
        @Deprecated("migrate to suspending api")
        override fun getCommand(name: String): CommandFunction? = null
        @Suppress("DEPRECATION")
        @Deprecated("migrate to suspending api")
        override fun getControl(name: String): ControlFunction? = null
        override fun getCommandTag(name: String): CommandTag? = null
        override fun getControlTag(name: String): ControlTag? = null
        override fun getOpFunction(name: String): UnOpFunction? = null
        override fun getBinaryOpFunction(name: String): BinOpFunction? = null
        override fun getFunction(name: String): Function? = null
        override fun getMethod(name: String, operator: String): Method? = null
        override fun getRescueMethod(name: String, operator: String): Method? = null
        override fun builder(): Builder<Unit> = Builder.create(rootContext = this)
    }

    private interface CaptureFunction<R> : FlowCollector<Any?> {
        fun result(): R
    }

    private interface SyncCaptureFunction<R> : CaptureFunction<R> {
        fun emitSync(value: Any?)
    }

    private object NoOpCaptureFunction : SyncCaptureFunction<Unit> {
        override suspend fun emit(value: Any?) = Unit
        override fun emitSync(value: Any?) = Unit
        override fun result() = Unit
    }

    @Suppress("DEPRECATION")
    private class RbCaptureFunction<R>(
        val rb: ResultBuilder<R>
    ) : SyncCaptureFunction<R> {
        override suspend fun emit(value: Any?) = rb.append(value)
        override fun emitSync(value: Any?) = rb.append(value)
        override fun result(): R = rb.build()
    }

    private class CbCaptureFunction(
        val f: (Any?) -> Unit
    ) : SyncCaptureFunction<Unit> {
        override suspend fun emit(value: Any?) = f(value)
        override fun emitSync(value: Any?) = f(value)
        override fun result() = Unit
    }

    private class StringCaptureFunction : SyncCaptureFunction<String> {
        private val builder = StringBuilder()

        override suspend fun emit(value: Any?) {
            builder.append(value)
        }

        override fun emitSync(value: Any?) {
            builder.append(value)
        }

        override fun result() = builder.toString()
    }

    private class ListCaptureFunction : SyncCaptureFunction<List<Any?>> {
        private val builder = mutableListOf<Any?>()

        override suspend fun emit(value: Any?) {
            builder.add(value)
        }

        override fun emitSync(value: Any?) {
            builder.add(value)
        }

        override fun result(): List<Any?> = builder.toList()
    }

    private fun interface FlowCaptureFunction : CaptureFunction<Unit> {
        override fun result() = Unit
    }

    class Builder<R> private constructor(
        private val rootContext: Context<*>,
        private val scope: MutableMap<String, Any?> = mutableMapOf(),
        private val captureFunction: CaptureFunction<R>
    ) : Context<R>() {

        @Suppress("DEPRECATION")
        @Deprecated(
            "ResultBuilder is deprecated",
            ReplaceWith("Context.builder()")
        )
        constructor(
            rootContext: Context<*>,
            scope: MutableMap<String, Any?> = mutableMapOf(),
            resultBuilder: ResultBuilder<R>
        ) : this(
            rootContext = rootContext,
            scope = scope,
            captureFunction = RbCaptureFunction(resultBuilder)
        )

        companion object {
            internal fun create(
                rootContext: Context<*>,
                scope: MutableMap<String, Any?> = mutableMapOf()
            ) = Builder(rootContext, scope, NoOpCaptureFunction)
        }

        override val result: R
            get() = captureFunction.result()

        internal val flowCollector: FlowCollector<Any?>
            get() = captureFunction

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

        @Suppress("DEPRECATION")
        @Deprecated("migrate to suspending api")
        fun defineCommand(
            name: String,
            implementation: CommandFunction
        ): Builder<R> {
            scope["cmd_$name"] = implementation
            scope["%cmd_$name"] = CommandTag { ctx, args ->
                implementation.invoke(ctx, args)
            }
            return this
        }

        @Suppress("DEPRECATION")
        @Deprecated("migrate to suspending api")
        override fun getCommand(name: String): CommandFunction? {
            return scope["cmd_$name"]
                ?.let { it as CommandFunction }
                ?: rootContext.getCommand(name)
        }

        @Suppress("DEPRECATION")
        @Deprecated("migrate to suspending api")
        fun defineControl(
            name: String,
            implementation: ControlFunction
        ): Builder<R> {
            scope["control_$name"] = implementation
            scope["%control_$name"] = ControlTag { ctx, args ->
                implementation.invoke(ctx, args)
            }
            return this
        }

        @Suppress("DEPRECATION")
        @Deprecated("migrate to suspending api")
        override fun getControl(name: String): ControlFunction? {
            return scope["control_$name"]
                ?.let { it as ControlFunction }
                ?: rootContext.getControl(name)
        }

        fun defineCommandTag(
            name: String,
            implementation: CommandTag
        ): Builder<R> {
            scope["%cmd_$name"] = implementation
            return this
        }

        override fun getCommandTag(name: String): CommandTag? {
            return scope["%cmd_$name"]
                ?.let { it as CommandTag }
                ?: rootContext.getCommandTag(name)
        }

        fun defineControlTag(
            name: String,
            implementation: ControlTag
        ): Builder<R> {
            scope["%control_$name"] = implementation
            return this
        }

        override fun getControlTag(name: String): ControlTag? {
            return scope["%control_$name"]
                ?.let { it as ControlTag }
                ?: rootContext.getControlTag(name)
        }

        fun defineOpFunction(
            name: String,
            implementation: UnOpFunction
        ): Builder<R> {
            scope["unary_$name"] = implementation
            return this
        }

        override fun getOpFunction(name: String): UnOpFunction? {
            return scope["unary_$name"]
                ?.let { it as UnOpFunction }
                ?: rootContext.getOpFunction(name)
        }

        fun defineBinaryOpFunction(
            name: String,
            implementation: BinOpFunction
        ): Builder<R> {
            scope["binary_$name"] = implementation
            return this
        }

        fun defineBinaryOpFunction(
            name: String,
            condition: Any?,
            implementation: UnOpFunction
        ): Builder<R> {
            scope["binary_$name"] = object : ConditionalBinOpFunction {
                override fun condition(ctx: Context<*>, arg: Any?) = arg == condition
                override fun invoke(ctx: Context<*>, arg: Any?): Any? {
                    return implementation.invoke(ctx, arg)
                }
            }
            return this
        }

        override fun getBinaryOpFunction(name: String): BinOpFunction? {
            return scope["binary_$name"]
                ?.let { it as BinOpFunction }
                ?: rootContext.getBinaryOpFunction(name)
        }

        fun defineFunction(
            name: String,
            implementation: Function
        ): Builder<R> {
            scope["call_$name"] = implementation
            return this
        }

        override fun getFunction(name: String): Function? {
            return scope["call_$name"]
                ?.let { it as Function }
                ?: rootContext.getFunction(name)
        }

        fun defineMethod(
            name: String,
            implementation: Method
        ): Builder<R> {
            scope["apply_invoke_$name"] = implementation
            return this
        }

        fun defineMethod(
            name: String,
            operator: String,
            implementation: Method
        ): Builder<R> {
            scope["apply_${operator}_$name"] = implementation
            return this
        }

        override fun getMethod(name: String, operator: String): Method? {
            return scope["apply_${operator}_$name"]
                ?.let { it as Method }
                ?: rootContext.getMethod(name, operator)
        }

        fun defineRescueMethod(
            name: String,
            implementation: Method
        ): Builder<R> {
            scope["rescue_invoke_$name"] = implementation
            return this
        }

        fun defineRescueMethod(
            name: String,
            operator: String,
            implementation: Method
        ): Builder<R> {
            scope["rescue_${operator}_$name"] = implementation
            return this
        }

        override fun getRescueMethod(name: String, operator: String): Method? {
            return scope["rescue_${operator}_$name"]
                ?.let { it as Method }
                ?: rootContext.getRescueMethod(name, operator)
        }

        override fun builder(): Builder<Unit> = Builder(
            rootContext,
            scope.toMutableMap(),
            NoOpCaptureFunction
        )

        fun scope(): Builder<R> = Builder(
            rootContext,
            scope.toMutableMap(),
            captureFunction,
        )

        @Deprecated("replace with chained call to capture")
        fun scope(captureFunction: (Any?) -> Unit) = Builder(
            rootContext,
            scope.toMutableMap(),
            CbCaptureFunction(captureFunction)
        )

        @Suppress("DEPRECATION")
        @Deprecated("ResultBuilder is deprecated")
        fun <R> scope(resultBuilder: ResultBuilder<R>) = Builder(
            rootContext,
            scope.toMutableMap(),
            RbCaptureFunction(resultBuilder)
        )

        fun capture(captureFunction: (Any?) -> Unit) = Builder(
            rootContext,
            scope,
            CbCaptureFunction(captureFunction)
        )

        fun captureToString() = Builder(
            rootContext,
            scope,
            StringCaptureFunction()
        )

        fun captureToList() = Builder(
            rootContext,
            scope,
            ListCaptureFunction()
        )

        fun captureToFlow(flowCollector: FlowCollector<Any?>) = Builder(
            rootContext,
            scope,
            FlowCaptureFunction(flowCollector::emit)
        )

        @Deprecated("evaluator is using flowCollector now")
        fun emit(value: Any?) {
            (captureFunction as SyncCaptureFunction).emitSync(value)
        }

        fun build(): Context<R> {
            return if (rootContext === Context.Companion) {
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
            scope["var_$name"] ?: Undefined("undefined variable: '$name'")
        @Suppress("DEPRECATION")
        @Deprecated("migrate to suspending api")
        override fun getCommand(name: String): CommandFunction? =
            scope["cmd_$name"] as CommandFunction?
        @Suppress("DEPRECATION")
        @Deprecated("migrate to suspending api")
        override fun getControl(name: String): ControlFunction? =
            scope["control_$name"] as ControlFunction?
        override fun getCommandTag(name: String): CommandTag? =
            scope["%cmd_$name"] as? CommandTag?
        override fun getControlTag(name: String): ControlTag? =
            scope["%control_$name"] as? ControlTag?
        override fun getOpFunction(name: String): UnOpFunction? =
            scope["unary_$name"]?.let { it as UnOpFunction }
        override fun getBinaryOpFunction(name: String): BinOpFunction? =
            scope["binary_$name"]?.let { it as BinOpFunction }
        override fun getFunction(name: String): Function? =
            scope["call_$name"]?.let { it as Function }
        override fun getMethod(name: String, operator: String): Method? =
            scope["apply_${operator}_$name"]?.let { it as Method }
        override fun getRescueMethod(name: String, operator: String): Method? =
            scope["rescue_${operator}_$name"]?.let { it as Method }
        override fun builder(): Builder<Unit> = Builder.create(rootContext = this)
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

        @Suppress("DEPRECATION")
        @Deprecated("migrate to suspending api")
        override fun getCommand(name: String): CommandFunction? {
            return scope["cmd_$name"]
                ?.let { it as CommandFunction }
                ?: rootContext.getCommand(name)
        }

        @Suppress("DEPRECATION")
        @Deprecated("migrate to suspending api")
        override fun getControl(name: String): ControlFunction? {
            return scope["control_$name"]
                ?.let { it as ControlFunction }
                ?: rootContext.getControl(name)
        }

        override fun getCommandTag(name: String): CommandTag? {
            return scope["%cmd_$name"]
                ?.let { it as CommandTag }
                ?: rootContext.getCommandTag(name)
        }

        override fun getControlTag(name: String): ControlTag? {
            return scope["%control_$name"]
                ?.let { it as ControlTag }
                ?: rootContext.getControlTag(name)
        }

        override fun getOpFunction(name: String): UnOpFunction? {
            return scope["unary_$name"]
                ?.let { it as UnOpFunction }
                ?: rootContext.getOpFunction(name)
        }

        override fun getBinaryOpFunction(name: String): BinOpFunction? {
            return scope["binary_$name"]
                ?.let { it as BinOpFunction }
                ?: rootContext.getBinaryOpFunction(name)
        }

        override fun getFunction(name: String): Function? {
            return scope["call_$name"]
                ?.let { it as Function }
                ?: rootContext.getFunction(name)
        }

        override fun getMethod(name: String, operator: String): Method? {
            return scope["apply_${operator}_$name"]
                ?.let { it as Method }
                ?: rootContext.getMethod(name, operator)
        }

        override fun getRescueMethod(name: String, operator: String): Method? {
            return scope["rescue_${operator}_$name"]
                ?.let { it as Method }
                ?: rootContext.getRescueMethod(name, operator)
        }

        override fun builder(): Builder<Unit> = Builder.create(rootContext = this)
    }
}
