package org.cikit.forte.core

import kotlinx.coroutines.flow.FlowCollector
import org.cikit.forte.Forte
import org.cikit.forte.internal.EvaluatorStateImpl
import org.cikit.forte.internal.TemplateLoaderImpl
import org.cikit.forte.lib.core.FilterGet
import org.cikit.forte.lib.core.FilterSlice
import org.cikit.forte.lib.core.FilterString
import org.cikit.forte.parser.Expression
import org.cikit.forte.parser.Node
import org.cikit.forte.parser.ParsedTemplate

sealed class Context<R> {

    sealed class Key(val value: String) {
        class Command(name: String): Key("$name!") {
            val name: String
                get() = value.substring(0, value.length - 1)
        }

        class Control(name: String): Key("$name%") {
            val name: String
                get() = value.substring(0, value.length - 1)
        }

        class Unary(name: String): Key("`$name`()") {
            init {
                require('`' !in name) {
                    "illegal operator name: $name"
                }
            }

            val name: String
                get() = value.substring(1, value.length - 3)
        }

        class Binary(name: String): Key("`$name`.()") {
            init {
                require('`' !in name) {
                    "illegal operator name: $name"
                }
            }

            val name: String
                get() = value.substring(1, value.length - 4)
        }

        class Call(name: String): Key("$name()") {
            init {
                require('`' !in name) {
                    "illegal function name: $name"
                }
            }

            val name: String
                get() = value.substring(0, value.length - 2)
        }

        class Apply<T: Method>(name: String, operator: String): Key(
            "`${operator}`$name.()"
        ) {
            companion object {
                fun <T: O, O: Method> create(
                    name: String,
                    operator: MethodOperator<O>
                ): Apply<T> = Apply(name, operator.value)
            }

            constructor(name: String, implementation: T): this(
                name,
                implementation.operator
            )

            init {
                require('`' !in operator) {
                    "illegal operator name: $operator"
                }
            }

            val name: String
                get() = value.substring(
                    value.lastIndexOf('`') + 1,
                    value.length - 3
                )

            val operator: String
                get() = value.substring(1, value.indexOf('`', 1))
        }

        override fun toString(): String {
            return value
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Key

            return value == other.value
        }

        override fun hashCode(): Int {
            return value.hashCode()
        }
    }

    abstract val result: R

    open val filterGet: FilterMethod
        get() = resolveFilter(FilterGet.KEY)

    open val filterString: FilterMethod
        get() = resolveFilter(FilterString.KEY)

    open val filterSlice: FilterMethod
        get() = resolveFilter(FilterSlice.KEY)

    abstract fun getVar(name: String): Any?

    open fun getCommandTag(name: String) = getCommandTag(Key.Command(name))
    open fun getControlTag(name: String) = getControlTag(Key.Control(name))
    open fun getOpFunction(name: String) = getOpFunction(Key.Unary(name))
    open fun getBinaryOpFunction(name: String) =
        getBinaryOpFunction(Key.Binary(name))
    open fun getFunction(name: String) = getFunction(Key.Call(name))

    abstract fun getCommandTag(key: Key.Command): CommandTag?
    abstract fun getControlTag(key: Key.Control): ControlTag?
    abstract fun getOpFunction(key: Key.Unary): UnOpFunction?
    abstract fun getBinaryOpFunction(key: Key.Binary): BinOpFunction?
    abstract fun getFunction(key: Key.Call): Function?
    abstract fun <T: Method> getMethod(key: Key.Apply<T>): T?

    open suspend fun evalExpression(expression: Expression): Any? {
        return Forte.scope().withScope(this).evalExpression(expression)
    }

    internal abstract fun dependencyAware():
            Iterable<Pair<String, DependencyAware>>

    protected fun resolveFilter(key: Key.Apply<FilterMethod>): FilterMethod {
        return getMethod(key) ?: object : FilterMethod {
            override fun invoke(subject: Any?, args: NamedArgs): Any? {
                error("$key is not defined")
            }
        }
    }

    companion object : Context<Unit>() {
        override val result: Unit get() = Unit
        override fun getVar(name: String): Any =
            Undefined("undefined variable: '$name'")
        override fun getCommandTag(key: Key.Command): CommandTag? = null
        override fun getControlTag(key: Key.Control): ControlTag? = null
        override fun getOpFunction(key: Key.Unary): UnOpFunction? = null
        override fun getBinaryOpFunction(key: Key.Binary): BinOpFunction? = null
        override fun getFunction(key: Key.Call): Function? = null
        override fun <T: Method> getMethod(key: Key.Apply<T>): T? = null
        override fun dependencyAware(): Iterable<Pair<String, DependencyAware>> =
            emptyList()

        fun builder() = Builder.from(this, TemplateLoaderImpl.Empty)
    }

    sealed class Evaluator<R> : Context<R>() {
        abstract fun withRootScope(): Builder<R>
        abstract fun withScope(ctx: Context<*>): Builder<R>
        abstract fun scope(): Builder<R>
    }

    class Builder<R> private constructor(
        private val scope: MutableContext,
        private val resultGetter: () -> R,
        private val templateLoader: TemplateLoaderImpl,
        val resultBuilder: ResultBuilder
    ) : Evaluator<R>() {

        companion object {
            internal fun from(
                ctx: Context<*>,
                templateLoader: TemplateLoaderImpl
            ): Builder<Unit> {
                return Builder(
                    scope = importContext(ctx),
                    resultGetter = ::noop,
                    templateLoader = templateLoader,
                    resultBuilder = ResultBuilder.Discard
                )
            }

            private fun importContext(ctx: Context<*>) = when (ctx) {
                Context.Companion -> MutableContext(ctx)
                is ReadOnlyContext<*> -> ctx.toMutableContext()
                is MutableContext -> ctx.copy()
                is Builder -> ctx.scope.copy()
                is StaticContext -> MutableContext(ctx)
            }

            private fun noop() {}
        }

        override val result: R
            get() = resultGetter()

        override val filterGet: FilterMethod
            get() = scope.filterGet

        override val filterSlice: FilterMethod
            get() = scope.filterSlice

        override val filterString: FilterMethod
            get() = scope.filterString

        private val evaluator = EvaluatorStateImpl()

        fun setVar(name: String, value: Any?): Builder<R> {
            scope.setVar(name, value)
            return this
        }

        fun setVars(vararg pairs: Pair<String, Any?>): Builder<R> {
            for ((name, value) in pairs) {
                scope.setVar(name, value)
            }
            return this
        }

        fun setVars(pairs: Map<String, Any?>): Builder<R> {
            for ((name, value) in pairs) {
                scope.setVar(name, value)
            }
            return this
        }

        override fun getVar(name: String): Any? = scope.getVar(name)

        fun defineCommandTag(name: String, implementation: CommandTag) =
            defineCommandTag(Key.Command(name), implementation)

        fun defineCommandTag(
            name: String,
            hidden: Boolean = false,
            implementation:
            suspend (ctx: Builder<*>, ParsedTemplate, Map<String, Expression>) -> Unit
        ) = defineCommandTag(
            Key.Command(name),
            object: CommandTag {
                override val isHidden: Boolean
                    get() = hidden
                override suspend fun invoke(
                    ctx: Builder<*>,
                    template: ParsedTemplate,
                    args: Map<String, Expression>
                ) {
                    implementation(ctx, template, args)
                }
            }
        )

        fun defineCommandTag(
            key: Key.Command,
            implementation: CommandTag
        ): Builder<R> {
            scope.defineCommandTag(key, implementation)
            return this
        }

        override fun getCommandTag(key: Key.Command): CommandTag? {
            return scope.getCommandTag(key)
        }

        fun defineControlTag(name: String, implementation: ControlTag) =
            defineControlTag(Key.Control(name), implementation)

        fun defineControlTag(
            name: String,
            hidden: Boolean = false,
            implementation:
            suspend (ctx: Builder<*>, ParsedTemplate, List<Branch>) -> Unit
        ) = defineControlTag(
            Key.Control(name),
            object: ControlTag {
                override val isHidden: Boolean
                    get() = hidden
                override suspend fun invoke(
                    ctx: Builder<*>,
                    template: ParsedTemplate,
                    branches: List<Branch>
                ) {
                    implementation(ctx, template, branches)
                }
            }
        )

        fun defineControlTag(
            key: Key.Control,
            implementation: ControlTag
        ): Builder<R> {
            scope.defineControlTag(key, implementation)
            return this
        }

        override fun getControlTag(key: Key.Control): ControlTag? {
            return scope.getControlTag(key)
        }

        fun defineOpFunction(name: String, implementation: UnOpFunction) =
            defineOpFunction(Key.Unary(name), implementation)

        fun defineOpFunction(
            name: String,
            hidden: Boolean = false,
            implementation: (arg: Any?) -> Any?
        ) = defineOpFunction(
            name,
            object : UnOpFunction {
                override val isHidden: Boolean
                    get() = hidden
                override fun invoke(arg: Any?): Any? = implementation(arg)
            }
        )

        fun defineOpFunction(
            key: Key.Unary,
            implementation: UnOpFunction
        ): Builder<R> {
            scope.defineOpFunction(key, implementation)
            return this
        }

        override fun getOpFunction(key: Key.Unary): UnOpFunction? {
            return scope.getOpFunction(key)
        }

        fun defineBinaryOpFunction(
            name: String,
            implementation: BinOpFunction
        ) = defineBinaryOpFunction(Key.Binary(name), implementation)

        fun defineBinaryOpFunction(
            name: String,
            hidden: Boolean = false,
            implementation: (left: Any?, right: Any?) -> Any?
        ) = defineBinaryOpFunction(
            name,
            object : BinOpFunction {
                override val isHidden: Boolean
                    get() = hidden
                override fun invoke(left: Any?, right: Any?): Any? {
                    return implementation(left, right)
                }
            }
        )

        fun defineBinaryOpFunction(
            key: Key.Binary,
            implementation: BinOpFunction
        ): Builder<R> {
            scope.defineBinaryOpFunction(key, implementation)
            return this
        }

        override fun getBinaryOpFunction(key: Key.Binary): BinOpFunction? {
            return scope.getBinaryOpFunction(key)
        }

        fun defineFunction(name: String, implementation: Function) =
            defineFunction(Key.Call(name), implementation)

        fun defineFunction(
            name: String,
            hidden: Boolean = false,
            implementation: (args: NamedArgs) -> Any?
        ) = defineFunction(
            name,
            object : Function {
                override val isHidden: Boolean
                    get() = hidden
                override fun invoke(args: NamedArgs): Any? =
                    implementation(args)
            }
        )

        fun defineFunction(
            key: Key.Call,
            implementation: Function
        ): Builder<R> {
            scope.defineFunction(key, implementation)
            return this
        }

        override fun getFunction(key: Key.Call): Function? {
            return scope.getFunction(key)
        }

        fun defineMethod(name: String, implementation: Method) = defineMethod(
            Key.Apply(name, implementation.operator),
            implementation
        )

        fun defineMethod(
            name: String,
            hidden: Boolean = false,
            rescue: Boolean = false,
            implementation: (subject: Any?, args: NamedArgs) -> Any?
        ): Builder<R> = defineMethod(
            name,
            object : Method {
                override val isHidden: Boolean
                    get() = hidden
                override val isRescue: Boolean
                    get() = rescue
                override fun invoke(subject: Any?, args: NamedArgs): Any? {
                    return implementation(subject, args)
                }
            }
        )

        fun <T: Method> defineMethod(
            key: Key.Apply<T>,
            implementation: T
        ): Builder<R> {
            scope.defineMethod(key, implementation)
            return this
        }

        override fun <T: Method> getMethod(key: Key.Apply<T>): T? {
            return scope.getMethod(key)
        }

        override fun dependencyAware() = scope.dependencyAware()

        override fun withRootScope(): Builder<R> = Builder(
            importContext(templateLoader.rootContext),
            resultGetter,
            templateLoader,
            resultBuilder
        )

        override fun withScope(ctx: Context<*>): Builder<R> = Builder(
            importContext(ctx),
            resultGetter,
            templateLoader,
            resultBuilder
        )

        override fun scope(): Builder<R> = Builder(
            scope.copy(),
            resultGetter,
            templateLoader,
            resultBuilder
        )

        fun discard() = Builder(
            scope,
            ::noop,
            templateLoader,
            ResultBuilder.Discard
        )

        fun captureTo(target: (Any?) -> Unit) = Builder(
            scope,
            ::noop,
            templateLoader,
            ResultBuilder.Emit { value -> target(value) }
        )

        fun captureTo(flowCollector: FlowCollector<Any?>) = Builder(
            scope,
            ::noop,
            templateLoader,
            ResultBuilder.Emit(flowCollector)
        )

        fun captureTo(resultBuilder: ResultBuilder) = Builder(
            scope,
            ::noop,
            templateLoader,
            resultBuilder
        )

        fun captureToList(): Builder<List<Any?>> {
            val listBuilder = mutableListOf<Any?>()
            return Builder(
                scope,
                listBuilder::toList,
                templateLoader,
                ResultBuilder.Emit { value -> listBuilder.add(value) }
            )
        }

        fun renderTo(target: Appendable) = Builder(
            scope,
            ::noop,
            templateLoader,
            ResultBuilder.Render { value -> target.append(value) }
        )

        fun renderTo(flowCollector: FlowCollector<CharSequence>) = Builder(
            scope,
            ::noop,
            templateLoader,
            ResultBuilder.Render(flowCollector)
        )

        fun renderToString(): Builder<String> {
            val stringBuilder = StringBuilder()
            return Builder(
                scope,
                stringBuilder::toString,
                templateLoader,
                ResultBuilder.Render { value -> stringBuilder.append(value) }
            )
        }

        suspend fun emitValue(value: Any?): Builder<R> {
            when (resultBuilder) {
                is ResultBuilder.Discard -> {}
                is ResultBuilder.Render -> {
                    if (value is CharSequence) {
                        resultBuilder.emit(value)
                    } else {
                        var result = filterString(value, NamedArgs.Empty)
                        if (result is Suspended) {
                            result = result.eval(this)
                        }
                        if (result is CharSequence) {
                            resultBuilder.emit(result)
                        } else {
                            error("cannot convert value " +
                                    "of type '${typeName(value)}' to string")
                        }
                    }
                }
                is ResultBuilder.Emit -> resultBuilder.emit(value)
            }
            return this
        }

        suspend fun emitExpression(expression: Expression): Builder<R> {
            when (resultBuilder) {
                is ResultBuilder.Discard -> evalExpression(expression)
                is ResultBuilder.Render -> {
                    val value = evaluator.renderExpression(this, expression)
                    resultBuilder.emit(value)
                }
                is ResultBuilder.Emit -> {
                    val value = evaluator.evalExpression(this, expression)
                    resultBuilder.emit(value)
                }
            }
            return this
        }

        override suspend fun evalExpression(expression: Expression): Any? {
            return evaluator.evalExpression(this, expression)
        }

        suspend fun evalTemplate(path: UPath): Builder<R> {
            loadTemplate(listOf(path), null) { parsedTemplate ->
                evalTemplate(parsedTemplate)
            }
            return this
        }

        suspend fun evalTemplate(template: ParsedTemplate): Builder<R> {
            try {
                for (cmd in template.nodes) {
                    cmd.eval(this, template)
                }
                return this
            } catch (ex: EvalException) {
                ex.setTemplate(template)
                throw ex
            }
        }

        suspend fun evalNodes(
            template: ParsedTemplate,
            nodes: Iterable<Node>
        ): Builder<R> {
            try {
                for (node in nodes) {
                    node.eval(this, template)
                }
                return this
            } catch (ex: EvalException) {
                ex.setTemplate(template)
                throw ex
            }
        }

        @Deprecated("confusing name. replace with loadTemplate", level = DeprecationLevel.HIDDEN)
        suspend fun includeTemplate(
            search: Iterable<UPath>,
            relativeTo: UPath?,
            ignoreMissing: Boolean = false,
            block: suspend (ParsedTemplate) -> Unit
        ): Builder<R> {
            if (ignoreMissing) {
                loadTemplateOrNull(search, relativeTo, block)
            } else {
                loadTemplate(search, relativeTo, block)
            }
            return this
        }

        suspend fun <T> loadTemplate(
            path: UPath,
            relativeTo: UPath? = null,
            block: suspend (ParsedTemplate) -> T
        ): T = loadTemplateOrNull(listOf(path), relativeTo, block)
            ?: error("Template not found: $path")

        suspend fun <T> loadTemplate(
            search: Iterable<UPath>,
            relativeTo: UPath? = null,
            block: suspend (ParsedTemplate) -> T
        ): T = loadTemplateOrNull(search, relativeTo, block)
            ?: error("Template not found: $search")

        suspend fun <T> loadTemplateOrNull(
            path: UPath,
            relativeTo: UPath? = null,
            block: suspend (ParsedTemplate) -> T
        ): T? = loadTemplateOrNull(listOf(path), relativeTo, block)

        suspend fun <T> loadTemplateOrNull(
            search: Iterable<UPath>,
            relativeTo: UPath? = null,
            block: suspend (ParsedTemplate) -> T
        ): T? {
            return templateLoader.loadTemplate(
                search = search,
                relativeTo = relativeTo,
            ) { parsedTemplate ->
                block(parsedTemplate)
            }
        }

        @Deprecated("complicated API", level = DeprecationLevel.HIDDEN)
        suspend fun importTemplate(
            search: Iterable<UPath>,
            relativeTo: UPath?,
            ignoreMissing: Boolean = false,
            block: suspend (ParsedTemplate, Context<List<Any?>>) -> Unit
        ): Builder<R> {
            if (ignoreMissing) {
                importTemplate(search, relativeTo, block)
            } else {
                importTemplate(search, relativeTo, block)
            }
            return this
        }

        suspend fun <T> importTemplate(
            path: UPath,
            relativeTo: UPath? = null,
            block: suspend (ParsedTemplate, Context<List<Any?>>) -> T
        ): T = importTemplateOrNull(listOf(path), relativeTo, block)
            ?: error("Template not found: $path")

        suspend fun <T> importTemplate(
            search: Iterable<UPath>,
            relativeTo: UPath? = null,
            block: suspend (ParsedTemplate, Context<List<Any?>>) -> T
        ): T = importTemplateOrNull(search, relativeTo, block)
            ?: error("Template not found: $search")

        suspend fun <T> importTemplateOrNull(
            path: UPath,
            relativeTo: UPath? = null,
            block: suspend (ParsedTemplate, Context<List<Any?>>) -> T
        ): T? = importTemplateOrNull(listOf(path), relativeTo, block)

        suspend fun <T> importTemplateOrNull(
            search: Iterable<UPath>,
            relativeTo: UPath? = null,
            block: suspend (ParsedTemplate, Context<List<Any?>>) -> T
        ): T? {
            return templateLoader.importTemplate(
                search = search,
                relativeTo = relativeTo,
            ) { parsedTemplate, importedTemplate ->
                block(parsedTemplate, importedTemplate)
            }
        }

        fun build(): Context<R> {
            return scope.build(result)
        }
    }

    /**
     * import scope (entries must be in correct order) into an unordered
     * HashMap and a separate index for dependency awares
     */
    private class StaticContext(scope: Map<String, Any?>) : Context<Unit>() {
        private val scope: Map<String, Any?>
        private val dependencyAware: List<Pair<String, DependencyAware>>

        init {
            // copy all items into this.scope
            val mutableScope = HashMap(scope)
            val mutableList = mutableListOf<Pair<String, DependencyAware>>()
            this.scope = mutableScope
            // reimport and index dependency awares
            for ((k, currentValue) in scope) {
                if (currentValue is DependencyAware) {
                    val withDependencies = currentValue.withDependencies(this)
                    if (withDependencies !== currentValue) {
                        mutableScope[k] = withDependencies
                    }
                    mutableList.add(k to withDependencies)
                }
            }
            this.dependencyAware = mutableList.toList()
        }

        override val result: Unit
            get() = Unit

        override fun getVar(name: String): Any? {
            if (isVarName(name) && scope.containsKey(name)) {
                return scope.getValue(name)
            }
            return Undefined("undefined variable: '$name'")
        }
        override fun getCommandTag(key: Key.Command): CommandTag? =
            scope[key.value]?.let { it as CommandTag}
        override fun getControlTag(key: Key.Control): ControlTag? =
            scope[key.value]?.let { it as ControlTag}
        override fun getOpFunction(key: Key.Unary): UnOpFunction? =
            scope[key.value]?.let { it as UnOpFunction }
        override fun getBinaryOpFunction(key: Key.Binary): BinOpFunction? =
            scope[key.value]?.let { it as BinOpFunction }
        override fun getFunction(key: Key.Call): Function? =
            scope[key.value]?.let { it as Function }
        override fun <T: Method> getMethod(key: Key.Apply<T>): T? {
            @Suppress("UNCHECKED_CAST")
            return scope[key.value]?.let { it as T }
        }

        override fun dependencyAware() = dependencyAware
    }

    /**
     * import scope into a LinkedHashMap, optionally using an index of
     * dependency awares to retain the correct order.
     * in case the index is not provided (set to null), the entries of
     * scope must be in correct order.
     */
    private class MutableContext(
        val rootContext: Context<*>,
        scope: Map<String, Any?> = emptyMap(),
        dependencyAware: List<Pair<String, DependencyAware>>? = null,
        filterGet: FilterGet? = null,
        filterSlice: FilterSlice? = null,
        filterString: FilterString? = null,
    ) : Context<Unit>() {

        private val scope: MutableMap<String, Any?>

        override var filterGet: FilterMethod
        override var filterSlice: FilterMethod
        override var filterString: FilterMethod

        init {
            // copy all items into this.scope
            val mutableScope = LinkedHashMap(scope)
            this.scope = mutableScope
            this.filterGet = filterGet ?: resolveFilter(FilterGet.KEY)
            this.filterSlice = filterSlice ?: resolveFilter(FilterSlice.KEY)
            this.filterString = filterString ?: resolveFilter(FilterString.KEY)
            if (dependencyAware != null) {
                // reimport dependency awares
                for ((k, currentValue) in dependencyAware) {
                    val withDependencies = currentValue.withDependencies(this)
                    if (withDependencies !== currentValue) {
                        mutableScope[k] = withDependencies
                        updateMutableRef(k)
                    }
                }
            }
        }

        override val result: Unit
            get() = Unit

        override fun getVar(name: String): Any? {
            if (isVarName(name) && scope.containsKey(name)) {
                return scope[name]
            }
            return rootContext.getVar(name)
        }

        fun setVar(name: String, value: Any?) {
            requireVarName(name)
            scope[name] = value
        }

        override fun getCommandTag(key: Key.Command): CommandTag? {
            return scope[key.value]
                ?.let { it as CommandTag }
                ?: rootContext.getCommandTag(key)
        }

        fun defineCommandTag(
            key: Key.Command,
            implementation: CommandTag
        ) {
            scope[key.value] = implementation
        }

        override fun getControlTag(key: Key.Control): ControlTag? {
            return scope[key.value]
                ?.let { it as ControlTag }
                ?: rootContext.getControlTag(key)
        }

        fun defineControlTag(
            key: Key.Control,
            implementation: ControlTag
        ) {
            scope[key.value] = implementation
        }

        override fun getOpFunction(key: Key.Unary): UnOpFunction? {
            return scope[key.value]
                ?.let { it as UnOpFunction }
                ?: rootContext.getOpFunction(key)
        }

        fun defineOpFunction(
            key: Key.Unary,
            implementation: UnOpFunction
        ) {
            define(
                key.value,
                rootContext.getOpFunction(key),
                implementation
            )
        }

        override fun getBinaryOpFunction(key: Key.Binary): BinOpFunction? {
            return scope[key.value]
                ?.let { it as BinOpFunction}
                ?: rootContext.getBinaryOpFunction(key)
        }

        fun defineBinaryOpFunction(
            key: Key.Binary,
            implementation: BinOpFunction
        ) {
            define(
                key.value,
                rootContext.getBinaryOpFunction(key),
                implementation
            )
        }

        override fun getFunction(key: Key.Call): Function? {
            return scope[key.value]
                ?.let { it as Function }
                ?: rootContext.getFunction(key)
        }

        fun defineFunction(
            key: Key.Call,
            implementation: Function
        ) {
            define(
                key.value,
                rootContext.getFunction(key),
                implementation
            )
        }

        override fun <T: Method> getMethod(key: Key.Apply<T>): T? {
            @Suppress("UNCHECKED_CAST")
            return scope[key.value]
                ?.let { it as T }
                ?: rootContext.getMethod(key)
        }

        fun defineMethod(
            key: Key.Apply<*>,
            implementation: Method
        ) {
            define(
                key.value,
                rootContext.getMethod(key),
                implementation
            )
        }

        override fun dependencyAware() = sequence {
            for (pair in rootContext.dependencyAware()) {
                if (!scope.containsKey(pair.first)) {
                    yield(pair)
                }
            }
            for ((k, v) in scope) {
                if (v is DependencyAware) {
                    yield(k to v)
                }
            }
        }.asIterable()

        fun copy(): MutableContext = MutableContext(rootContext, scope)

        fun reset(): MutableContext = MutableContext(rootContext, emptyMap())

        fun <R> build(result: R): Context<R> {
            return if (rootContext === Companion) {
                ReadOnlyContext(
                    rootContext = if (scope.isEmpty()) {
                        Companion
                    } else {
                        StaticContext(scope)
                    },
                    scope = emptyMap(),
                    result = result,
                )
            } else {
                ReadOnlyContext(
                    rootContext = rootContext,
                    scope = scope,
                    result = result,
                )
            }
        }

        private fun define(
            key: String,
            initialImplementation: Any?,
            implementation: Any
        ) {
            if (initialImplementation != null) {
                // redefine item and reimport all dependency awares
                scope[key] = implementation
                val keys = scope.keys
                for ((k, v) in rootContext.dependencyAware()) {
                    if (k !in keys) {
                        import(k, v)
                    }
                }
                for (k in keys) {
                    val v = scope[k] as? DependencyAware
                    if (v != null) {
                        import(k, v)
                    }
                }
            } else if (scope.containsKey(key)) {
                // redefine item and reimport all dependency awares in scope
                scope[key] = implementation
                for ((k, v) in scope) {
                    if (v is DependencyAware) {
                        import(k, v)
                    }
                }
            } else if (implementation is DependencyAware) {
                // define new context aware
                scope[key] = implementation.withDependencies(this)
            } else {
                // define new item
                scope[key] = implementation
            }
            updateMutableRef(key)
        }

        private fun import(key: String, value: DependencyAware) {
            val withContext = value.withDependencies(this)
            if (withContext !== value) {
                scope[key] = withContext
            }
        }

        private fun updateMutableRef(key: String) = when (key) {
            FilterGet.KEY.value -> {
                filterGet = resolveFilter(FilterGet.KEY)
            }
            FilterSlice.KEY.value -> {
                filterSlice = resolveFilter(FilterSlice.KEY)
            }
            FilterString.KEY.value -> {
                filterString = resolveFilter(FilterString.KEY)
            }

            else -> {}
        }
    }

    /**
     * import scope (entries must be in correct order) into an unordered
     * HashMap and a separate index for dependency awares
     */
    private class ReadOnlyContext<R>(
        val rootContext: Context<*>,
        scope: Map<String, Any?>,
        override val result: R,
    ) : Context<R>() {
        override var filterGet: FilterMethod
            private set

        override var filterSlice: FilterMethod
            private set

        override var filterString: FilterMethod
            private set

        private val scope: Map<String, Any?>
        private val dependencyAware: List<Pair<String, DependencyAware>>

        init {
            // copy all items into this.scope
            val mutableScope = HashMap(scope)
            val mutableList = mutableListOf<Pair<String, DependencyAware>>()
            this.scope = mutableScope
            this.filterGet = resolveFilter(FilterGet.KEY)
            this.filterSlice = resolveFilter(FilterSlice.KEY)
            this.filterString = resolveFilter(FilterString.KEY)
            // reimport dependency awares
            val imported = HashSet<String>()
            for ((k, v) in rootContext.dependencyAware()) {
                val currentValue = if (scope.containsKey(k)) {
                    imported.add(k)
                    scope[k] as? DependencyAware ?: continue
                } else {
                    v
                }
                val withDependencies = currentValue.withDependencies(this)
                if (withDependencies !== currentValue) {
                    mutableScope[k] = withDependencies
                }
                mutableList.add(k to withDependencies)
            }
            for ((k, currentValue) in scope) {
                if (k !in imported && currentValue is DependencyAware) {
                    val withDependencies = currentValue.withDependencies(this)
                    if (withDependencies !== currentValue) {
                        mutableScope[k] = withDependencies
                    }
                    mutableList.add(k to withDependencies)
                }
            }
            this.filterGet = resolveFilter(FilterGet.KEY)
            this.filterSlice = resolveFilter(FilterSlice.KEY)
            this.filterString = resolveFilter(FilterString.KEY)
            this.dependencyAware = mutableList.toList()
        }

        override fun getVar(name: String): Any? {
            if (isVarName(name) && scope.containsKey(name)) {
                return scope[name]
            }
            return rootContext.getVar(name)
        }

        override fun getCommandTag(key: Key.Command): CommandTag? {
            return scope[key.value]
                ?.let { it as CommandTag }
                ?: rootContext.getCommandTag(key)
        }

        override fun getControlTag(key: Key.Control): ControlTag? {
            return scope[key.value]
                ?.let { it as ControlTag }
                ?: rootContext.getControlTag(key)
        }

        override fun getOpFunction(key: Key.Unary): UnOpFunction? {
            return scope[key.value]?.let { it as UnOpFunction}
                ?: rootContext.getOpFunction(key)
        }

        override fun getBinaryOpFunction(key: Key.Binary): BinOpFunction? {
            return scope[key.value]?.let { it as BinOpFunction }
                ?: rootContext.getBinaryOpFunction(key)
        }

        override fun getFunction(key: Key.Call): Function? {
            return scope[key.value]?.let { it as Function }
                ?: rootContext.getFunction(key)
        }

        override fun <T: Method> getMethod(key: Key.Apply<T>): T? {
            @Suppress("UNCHECKED_CAST")
            return scope[key.value]?.let { it as T }
                ?: rootContext.getMethod(key)
        }

        override fun dependencyAware() = dependencyAware

        fun toMutableContext() = MutableContext(
            rootContext = rootContext,
            scope = scope,
            dependencyAware = dependencyAware
        )
    }
}

private fun requireVarName(name: String) = require(
    name.isNotBlank() &&
            name[name.length - 1].code !in 0x20 .. 0x2F
) {
    "illegal variable name: $name"
}

private fun isVarName(name: String) = name.isNotEmpty() &&
        name[name.length - 1].code !in 0x20 .. 0x2F
