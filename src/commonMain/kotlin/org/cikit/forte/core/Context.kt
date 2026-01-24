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

    protected sealed class WithDependants(
        protected var dependants: Array<String?>
    ) : Iterable<String?> {
        abstract val implementation: Any
        abstract fun update(implementation: Any): WithDependants
        abstract fun append(key: String): WithDependants
        abstract fun remove(key: String): WithDependants

        override fun iterator(): Iterator<String?> = dependants.iterator()

        companion object {
            fun Array<String?>.append(key: String): Array<String?> {
                return copyOf(size + 1).also { it[it.size - 1] = key }
            }

            fun Array<String?>.removeAt(i: Int): Array<String?> {
                if (i == size - 1) {
                    return copyOf(size - 1)
                }
                if (i == 0) {
                    return copyOfRange(1, size)
                }
                val result = arrayOfNulls<String>(size - 1)
                copyInto(result, 0, 0, i)
                copyInto(result, i, i + 1, size)
                return result
            }
        }

        class Mutable private constructor(
            override var implementation: Any,
            dependants: Array<String?>
        ): WithDependants(dependants) {
            constructor(implementation: Any, dependant: String) : this(
                implementation,
                arrayOf(dependant)
            )

            constructor(frozen: Frozen, implementation: Any) : this(
                implementation,
                frozen.dependants
            )

            constructor(
                frozen: Frozen,
                implementation: Any,
                key: String,
            ) : this(
                implementation,
                frozen.dependants.append(key)
            )

            constructor(
                frozen: Frozen,
                implementation: Any,
                removeAt: Int,
            ) : this(
                implementation,
                frozen.dependants.removeAt(removeAt)
            )

            override fun update(implementation: Any): WithDependants {
                this.implementation = implementation
                return this
            }

            override fun append(key: String): WithDependants {
                if (key !in dependants) {
                    dependants = dependants.append(key)
                }
                return this
            }

            override fun remove(key: String): WithDependants {
                val i = dependants.indexOf(key)
                if (i >= 0) {
                    dependants = dependants.removeAt(i)
                }
                return this
            }

            fun copy(): Mutable = Mutable(
                implementation,
                dependants
            )

            fun freeze(): Frozen = Frozen(this)
        }

        class Frozen private constructor(
            override val implementation: Any,
            dependants: Array<String?>
        ): WithDependants(dependants) {
            constructor(from: Mutable) : this(
                from.implementation,
                from.dependants
            )

            override fun update(implementation: Any): WithDependants {
                return Mutable(this, implementation)
            }

            override fun append(key: String): WithDependants {
                if (key in dependants) {
                    return this
                }
                return Mutable(this, implementation, key)
            }

            override fun remove(key: String): WithDependants {
                val i = dependants.indexOf(key)
                if (i < 0) {
                    return this
                }
                return Mutable(this, implementation, removeAt = i)
            }
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

    protected fun resolveFilter(key: Key.Apply<FilterMethod>): FilterMethod {
        return getMethod(key) ?: object : FilterMethod {
            override fun invoke(subject: Any?, args: NamedArgs): Any? {
                error("$key is not defined")
            }
        }
    }

    protected abstract fun getImplementation(key: String): Any?

    protected inline fun <reified T> Any.castImplementation(): T {
        return if (this is WithDependants) {
            implementation as T
        } else {
            this as T
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
        override fun getImplementation(key: String): Any? = null

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
                is StaticContext -> ctx.toMutableContext()

                else -> throw IllegalArgumentException()
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
            ResultBuilder.Render { value ->
                if (value is InlineString) {
                    value.appendTo(target)
                } else {
                    target.append(value)
                }
            }
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
                ResultBuilder.Render { value ->
                    if (value is InlineString) {
                        value.appendTo(stringBuilder)
                    } else {
                        stringBuilder.append(value)
                    }
                }
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

        override fun getImplementation(key: String): Any? {
            return scope.getImplementation(key)
        }
    }

    private class StaticContext(
        private val scope: Map<String, Any?>,
    ) : Context<Unit>() {
        override val result: Unit
            get() = Unit

        override fun getVar(name: String): Any? {
            if (isVarName(name) && scope.containsKey(name)) {
                return scope.getValue(name)
            }
            return Undefined("undefined variable: '$name'")
        }
        override fun getCommandTag(key: Key.Command): CommandTag? =
            scope[key.value]?.castImplementation()
        override fun getControlTag(key: Key.Control): ControlTag? =
            scope[key.value]?.castImplementation()
        override fun getOpFunction(key: Key.Unary): UnOpFunction? =
            scope[key.value]?.castImplementation()
        override fun getBinaryOpFunction(key: Key.Binary): BinOpFunction? =
            scope[key.value]?.castImplementation()
        override fun getFunction(key: Key.Call): Function? =
            scope[key.value]?.castImplementation()
        override fun <T: Method> getMethod(key: Key.Apply<T>): T? {
            @Suppress("UNCHECKED_CAST")
            return scope[key.value]
                ?.let { it.castImplementation<Method>() as T }
        }

        override fun getImplementation(key: String): Any? = scope[key]

        fun toMutableContext(): MutableContext = MutableContext(
            rootContext = this,
        )
    }

    private class MutableContext(
        val rootContext: Context<*>,
        scope: Map<String, Any?> = emptyMap(),
        filterGet: FilterMethod? = null,
        filterSlice: FilterMethod? = null,
        filterString: FilterMethod? = null,
    ) : Context<Unit>() {

        private val scope: MutableMap<String, Any?> = HashMap(scope.size)

        override var filterGet: FilterMethod
        override var filterSlice: FilterMethod
        override var filterString: FilterMethod

        init {
            for ((k, v) in scope) {
                if (v is WithDependants.Mutable) {
                    this.scope[k] = v.copy()
                } else {
                    this.scope[k] = v
                }
            }
            this.filterGet = filterGet ?: resolveFilter(FilterGet.KEY)
            this.filterSlice = filterSlice ?: resolveFilter(FilterSlice.KEY)
            this.filterString = filterString ?: resolveFilter(FilterString.KEY)
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
                ?.castImplementation()
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
                ?.castImplementation()
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
                ?.castImplementation()
                ?: rootContext.getOpFunction(key)
        }

        fun defineOpFunction(
            key: Key.Unary,
            implementation: UnOpFunction
        ) {
            define(key.value, implementation)
        }

        override fun getBinaryOpFunction(key: Key.Binary): BinOpFunction? {
            return scope[key.value]
                ?.castImplementation()
                ?: rootContext.getBinaryOpFunction(key)
        }

        fun defineBinaryOpFunction(
            key: Key.Binary,
            implementation: BinOpFunction
        ) {
            define(key.value, implementation)
        }

        override fun getFunction(key: Key.Call): Function? {
            return scope[key.value]
                ?.castImplementation()
                ?: rootContext.getFunction(key)
        }

        fun defineFunction(
            key: Key.Call,
            implementation: Function
        ) {
            define(key.value, implementation)
        }

        override fun <T: Method> getMethod(key: Key.Apply<T>): T? {
            @Suppress("UNCHECKED_CAST")
            return scope[key.value]
                ?.let { it.castImplementation<Method>() as T }
                ?: rootContext.getMethod(key)
        }

        fun defineMethod(
            key: Key.Apply<*>,
            implementation: Method
        ) {
            define(key.value, implementation)
            when (key.value) {
                FilterGet.KEY.value -> {
                    filterGet = resolveFilter(FilterGet.KEY)
                }
                FilterSlice.KEY.value -> {
                    filterSlice = resolveFilter(FilterSlice.KEY)
                }
                FilterString.KEY.value -> {
                    filterString = resolveFilter(FilterString.KEY)
                }
            }
        }

        fun copy(): MutableContext = MutableContext(
            rootContext = rootContext,
            scope = scope,
            filterGet = filterGet,
            filterSlice = filterSlice,
            filterString = filterString
        )

        fun <R> build(result: R): Context<R> {
            return if (rootContext === Companion) {
                ReadOnlyContext(
                    rootContext = if (scope.isEmpty()) {
                        Companion
                    } else {
                        StaticContext(buildScope())
                    },
                    scope = emptyMap(),
                    result = result,
                    filterGet = filterGet,
                    filterSlice = filterSlice,
                    filterString = filterString
                )
            } else {
                ReadOnlyContext(
                    rootContext = rootContext,
                    scope = buildScope(),
                    result = result,
                    filterGet = filterGet,
                    filterSlice = filterSlice,
                    filterString = filterString
                )
            }
        }

        private fun buildScope() = buildMap(scope.size) {
            for ((k, v) in scope) {
                if (v is WithDependants.Mutable) {
                    put(k, v.freeze())
                } else {
                    put(k, v)
                }
            }
        }

        public override fun getImplementation(key: String): Any? {
            return scope[key] ?: rootContext.getImplementation(key)
        }

        private fun define(key: String, implementation: Any) {
            val existing = getImplementation(key)
            val newDependencies: Set<String>
            val newImplementation: Any
            if (implementation is DependencyAware) {
                val dependencies = HashSet<String>()
                val withDependencies = implementation.withDependencies(
                    DependencyTracker(dependencies)
                )
                for (d in dependencies) {
                    if (d == key) {
                        error("cyclic dependency: $key")
                    }
                    val current = scope[d]
                        ?: continue // optional dependency has been tracked
                    if (current is WithDependants) {
                        val newEntry = current.append(key)
                        if (newEntry !== current) {
                            scope[d] = newEntry
                        }
                    } else {
                        scope[d] = WithDependants.Mutable(current, key)
                    }
                }
                newDependencies = dependencies
                newImplementation = withDependencies
            } else {
                newDependencies = emptySet()
                newImplementation = implementation
            }
            when (existing) {
                is WithDependants.Mutable -> {
                    updateDependencies(
                        key,
                        existing.implementation,
                        newDependencies
                    )
                    existing.implementation = newImplementation
                    updateDependants(existing)
                }
                is WithDependants -> {
                    updateDependencies(
                        key,
                        existing.implementation,
                        newDependencies
                    )
                    scope[key] = existing.update(newImplementation)
                    updateDependants(existing)
                }

                else -> {
                    scope[key] = newImplementation
                }
            }
        }

        private fun updateDependencies(
            key: String,
            oldImplementation: Any,
            newDependencies: Set<String>
        ) {
            if (oldImplementation is DependencyAware) {
                val oldDependencies = HashSet<String>()
                oldImplementation.withDependencies(
                    DependencyTracker(oldDependencies)
                )
                for (d in oldDependencies) {
                    if (d !in newDependencies) {
                        when (val current = getImplementation(d)) {
                            is WithDependants.Mutable -> {
                                current.remove(key)
                            }
                            is WithDependants -> {
                                scope[d] = current.remove(key)
                            }
                        }
                    }
                }
            }
        }

        private fun updateDependants(
            dependants: Iterable<String?>,
            visited: HashSet<String> = HashSet()
        ) {
            for (key in dependants) {
                if (key == null) {
                    continue
                }
                if (key in visited) {
                    error("cyclic dependency: $key")
                }
                visited.add(key)
                val existing = getImplementation(key)
                val dependencyAware: DependencyAware?
                val withDependants: WithDependants?
                if (existing is WithDependants) {
                    dependencyAware =
                        existing.implementation as? DependencyAware
                    withDependants = existing
                } else {
                    dependencyAware = existing as? DependencyAware
                    withDependants = null
                }
                if (dependencyAware == null) {
                    //this dependant seems not dependant anymore
                    continue
                }
                val withDependencies = dependencyAware
                    .withDependencies(this)
                if (withDependencies === dependencyAware) {
                    //this dependant seems not dependant anymore
                    continue
                }
                if (withDependants == null) {
                    scope[key] = withDependencies
                } else {
                    if (withDependants is WithDependants.Mutable) {
                        withDependants.implementation = withDependencies
                    } else {
                        scope[key] = withDependants.update(withDependants)
                    }
                    updateDependants(withDependants, visited)
                }
            }
        }

        inner class DependencyTracker(
            val dependencies: MutableSet<String> = HashSet()
        ) : Context<Unit>() {
            override val result: Unit
                get() = Unit

            override val filterGet: FilterMethod
                get() {
                    dependencies.add(FilterGet.KEY.value)
                    return super.filterGet
                }

            override val filterSlice: FilterMethod
                get() {
                    dependencies.add(FilterSlice.KEY.value)
                    return super.filterSlice
                }

            override val filterString: FilterMethod
                get() {
                    dependencies.add(FilterString.KEY.value)
                    return super.filterString
                }

            override fun getVar(name: String): Any? {
                throw IllegalStateException("cannot depend on variables")
            }

            override fun getCommandTag(key: Key.Command): CommandTag? {
                dependencies.add(key.value)
                return this@MutableContext.getCommandTag(key)
            }

            override fun getControlTag(key: Key.Control): ControlTag? {
                dependencies.add(key.value)
                return this@MutableContext.getControlTag(key)
            }

            override fun getOpFunction(key: Key.Unary): UnOpFunction? {
                dependencies.add(key.value)
                return this@MutableContext.getOpFunction(key)
            }

            override fun getBinaryOpFunction(key: Key.Binary): BinOpFunction? {
                dependencies.add(key.value)
                return this@MutableContext.getBinaryOpFunction(key)
            }

            override fun getFunction(key: Key.Call): Function? {
                dependencies.add(key.value)
                return this@MutableContext.getFunction(key)
            }

            override fun <T : Method> getMethod(key: Key.Apply<T>): T? {
                dependencies.add(key.value)
                return this@MutableContext.getMethod(key)
            }

            override suspend fun evalExpression(expression: Expression): Any? {
                throw IllegalStateException(
                    "cannot evaluate expressions on $this"
                )
            }

            override fun getImplementation(key: String): Any? {
                return this@MutableContext.getImplementation(key)
            }
        }
    }

    private class ReadOnlyContext<R>(
        val rootContext: Context<*>,
        private val scope: Map<String, Any?>,
        override val result: R,
        filterGet: FilterMethod? = null,
        filterSlice: FilterMethod? = null,
        filterString: FilterMethod? = null,
    ) : Context<R>() {
        override var filterGet: FilterMethod
            private set

        override var filterSlice: FilterMethod
            private set

        override var filterString: FilterMethod
            private set

        init {
            this.filterGet = filterGet ?: resolveFilter(FilterGet.KEY)
            this.filterSlice = filterSlice ?: resolveFilter(FilterSlice.KEY)
            this.filterString = filterString ?: resolveFilter(FilterString.KEY)
        }

        override fun getVar(name: String): Any? {
            if (isVarName(name) && scope.containsKey(name)) {
                return scope[name]
            }
            return rootContext.getVar(name)
        }

        override fun getCommandTag(key: Key.Command): CommandTag? {
            return scope[key.value]
                ?.castImplementation()
                ?: rootContext.getCommandTag(key)
        }

        override fun getControlTag(key: Key.Control): ControlTag? {
            return scope[key.value]
                ?.castImplementation()
                ?: rootContext.getControlTag(key)
        }

        override fun getOpFunction(key: Key.Unary): UnOpFunction? {
            return scope[key.value]
                ?.castImplementation()
                ?: rootContext.getOpFunction(key)
        }

        override fun getBinaryOpFunction(key: Key.Binary): BinOpFunction? {
            return scope[key.value]
                ?.castImplementation()
                ?: rootContext.getBinaryOpFunction(key)
        }

        override fun getFunction(key: Key.Call): Function? {
            return scope[key.value]
                ?.castImplementation()
                ?: rootContext.getFunction(key)
        }

        override fun <T: Method> getMethod(key: Key.Apply<T>): T? {
            @Suppress("UNCHECKED_CAST")
            return scope[key.value]
                ?.let { castImplementation<Method>() as T }
                ?: rootContext.getMethod(key)
        }

        override fun getImplementation(key: String): Any? {
            return if (scope.containsKey(key)) {
                scope[key]
            } else {
                rootContext.getImplementation(key)
            }
        }

        fun toMutableContext() = MutableContext(
            rootContext = rootContext,
            scope = scope,
            filterGet = filterGet,
            filterSlice = filterSlice,
            filterString = filterString,
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
