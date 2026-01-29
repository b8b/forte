package org.cikit.forte.core

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentHashSetOf
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

    protected sealed class Value {
        open fun getVar(name: String): Any? {
            return Undefined("undefined variable: '$name'")
        }

        data object Null : Value() {
            override fun getVar(name: String): Any? = null
        }

        class Invokable private constructor(
            val value: Any,
            val dependants: PersistentSet<String>
        ) : Value() {
            constructor(value: Any, vararg dependant: String) : this(
                value,
                persistentHashSetOf(*dependant)
            )

            fun update(value: Any) = Invokable(value, dependants)

            fun withDependants(vararg dependant: String): Invokable {
                val builder = dependants.builder()
                for (k in dependant) {
                    builder.add(k)
                }
                val newDependants = builder.build()
                if (newDependants === dependants) {
                    return this
                }
                return Invokable(value, newDependants)
            }

            fun removeDependants(vararg dependant: String): Invokable {
                val builder = dependants.builder()
                for (k in dependant) {
                    builder.remove(k)
                }
                val newDependants = builder.build()
                if (newDependants === dependants) {
                    return this
                }
                return Invokable(value, newDependants)
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

    protected inline fun <reified T> Any.invokableValue(): T? =
        (this as? Value.Invokable)?.let { it.value as T }

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

        fun builder() = Builder.from(this, TemplateLoaderImpl.Empty)
    }

    sealed class Evaluator<R> : Context<R>() {
        abstract fun withRootScope(): Builder<R>
        abstract fun withScope(ctx: Context<*>): Builder<R>
        abstract fun scope(): Builder<R>
    }

    class Builder<R> private constructor(
        scope: PersistentMap<String, Any>,
        filterGet: FilterMethod? = null,
        filterSlice: FilterMethod? = null,
        filterString: FilterMethod? = null,
        private val resultGetter: () -> R,
        private val templateLoader: TemplateLoaderImpl,
        val resultBuilder: ResultBuilder,
    ) : Evaluator<R>() {
        companion object {
            internal fun from(
                ctx: Context<*>,
                templateLoader: TemplateLoaderImpl
            ): Builder<Unit> {
                return Builder(
                    scope = importContext(ctx),
                    filterGet = ctx.filterGet,
                    filterSlice = ctx.filterSlice,
                    filterString = ctx.filterString,
                    resultGetter = ::noop,
                    templateLoader = templateLoader,
                    resultBuilder = ResultBuilder.Discard
                )
            }

            private fun importContext(ctx: Context<*>) = when (ctx) {
                Context.Companion -> persistentHashMapOf()
                is ReadOnlyContext<*> -> ctx.scope
                is Builder -> ctx.scope.build()

                else -> throw IllegalArgumentException()
            }

            private fun noop() {}
        }

        override val result: R
            get() = resultGetter()

        private var scope: PersistentMap.Builder<String, Any> = scope.builder()

        override var filterGet: FilterMethod = filterGet
            ?: resolveFilter(FilterGet.KEY)

        override var filterSlice: FilterMethod = filterSlice
            ?: resolveFilter(FilterSlice.KEY)

        override var filterString: FilterMethod = filterString
            ?: resolveFilter(FilterString.KEY)

        private val evaluator = EvaluatorStateImpl()

        fun setVar(name: String, value: Any?): Builder<R> {
            scope[name] = value ?: Value.Null
            return this
        }

        fun setVars(vararg pairs: Pair<String, Any?>): Builder<R> {
            for ((name, value) in pairs) {
                setVar(name, value)
            }
            return this
        }

        fun setVars(pairs: Map<String, Any?>): Builder<R> {
            for ((name, value) in pairs) {
                setVar(name, value)
            }
            return this
        }

        override fun getVar(name: String): Any? {
            return when (val result = scope[name]) {
                null -> Undefined("undefined variable: '$name'")
                is Value -> result.getVar(name)

                else -> result
            }
        }

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
            scope[key.value] = Value.Invokable(implementation)
            return this
        }

        override fun getCommandTag(key: Key.Command): CommandTag? {
            return scope[key.value]?.invokableValue()
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
            scope[key.value] = Value.Invokable(implementation)
            return this
        }

        override fun getControlTag(key: Key.Control): ControlTag? {
            return scope[key.value]?.invokableValue()
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
            define(key.value, implementation)
            return this
        }

        override fun getOpFunction(key: Key.Unary): UnOpFunction? {
            return scope[key.value]?.invokableValue()
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
            define(key.value, implementation)
            return this
        }

        override fun getBinaryOpFunction(key: Key.Binary): BinOpFunction? {
            return scope[key.value]?.invokableValue()
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
            define(key.value, implementation)
            return this
        }

        override fun getFunction(key: Key.Call): Function? {
            return scope[key.value]?.invokableValue()
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
            return this
        }

        override fun <T: Method> getMethod(key: Key.Apply<T>): T? {
            @Suppress("UNCHECKED_CAST")
            return scope[key.value]
                ?.let { it.invokableValue<Method>() as T }
        }

        override fun withRootScope(): Builder<R> {
            val rootContext = templateLoader.rootContext
            return Builder(
                scope = importContext(rootContext),
                filterGet = rootContext.filterGet,
                filterSlice = rootContext.filterSlice,
                filterString = rootContext.filterString,
                resultGetter = resultGetter,
                templateLoader = templateLoader,
                resultBuilder = resultBuilder
            )
        }

        override fun withScope(ctx: Context<*>): Builder<R> = Builder(
            importContext(ctx),
            filterGet = ctx.filterGet,
            filterSlice = ctx.filterSlice,
            filterString = ctx.filterString,
            resultGetter = resultGetter,
            templateLoader = templateLoader,
            resultBuilder = resultBuilder
        )

        override fun scope(): Builder<R> = Builder(
            scope.build(),
            filterGet = filterGet,
            filterSlice = filterSlice,
            filterString = filterString,
            resultGetter = resultGetter,
            templateLoader = templateLoader,
            resultBuilder = resultBuilder
        )

        fun discard() = Builder(
            scope = scope.build(),
            filterGet = filterGet,
            filterSlice = filterSlice,
            filterString = filterString,
            resultGetter = ::noop,
            templateLoader = templateLoader,
            resultBuilder = ResultBuilder.Discard
        )

        fun captureTo(target: (Any?) -> Unit) = Builder(
            scope = scope.build(),
            filterGet = filterGet,
            filterSlice = filterSlice,
            filterString = filterString,
            resultGetter = ::noop,
            templateLoader = templateLoader,
            resultBuilder = ResultBuilder.Emit { value -> target(value) }
        )

        fun captureTo(flowCollector: FlowCollector<Any?>) = Builder(
            scope = scope.build(),
            filterGet = filterGet,
            filterSlice = filterSlice,
            filterString = filterString,
            resultGetter = ::noop,
            templateLoader = templateLoader,
            resultBuilder = ResultBuilder.Emit(flowCollector)
        )

        fun captureTo(resultBuilder: ResultBuilder) = Builder(
            scope = scope.build(),
            filterGet = filterGet,
            filterSlice = filterSlice,
            filterString = filterString,
            resultGetter = ::noop,
            templateLoader = templateLoader,
            resultBuilder = resultBuilder
        )

        fun captureToList(): Builder<List<Any?>> {
            val listBuilder = mutableListOf<Any?>()
            return Builder(
                scope = scope.build(),
                filterGet = filterGet,
                filterSlice = filterSlice,
                filterString = filterString,
                resultGetter = listBuilder::toList,
                templateLoader = templateLoader,
                resultBuilder = ResultBuilder.Emit { value ->
                    listBuilder.add(value)
                }
            )
        }

        fun renderTo(target: Appendable) = Builder(
            scope = scope.build(),
            filterGet = filterGet,
            filterSlice = filterSlice,
            filterString = filterString,
            resultGetter = ::noop,
            templateLoader = templateLoader,
            resultBuilder = ResultBuilder.Render { value ->
                if (value is InlineString) {
                    value.appendTo(target)
                } else {
                    target.append(value)
                }
            }
        )

        fun renderTo(flowCollector: FlowCollector<CharSequence>) = Builder(
            scope = scope.build(),
            filterGet = filterGet,
            filterSlice = filterSlice,
            filterString = filterString,
            resultGetter = ::noop,
            templateLoader = templateLoader,
            resultBuilder = ResultBuilder.Render(flowCollector)
        )

        fun renderToString(): Builder<String> {
            val stringBuilder = StringBuilder()
            return Builder(
                scope = scope.build(),
                filterGet = filterGet,
                filterSlice = filterSlice,
                filterString = filterString,
                resultGetter = stringBuilder::toString,
                templateLoader = templateLoader,
                resultBuilder = ResultBuilder.Render { value ->
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

        fun build(): Context<R> = ReadOnlyContext(
            scope = scope.build(),
            result = result,
            filterGet = filterGet,
            filterSlice = filterSlice,
            filterString = filterString
        )

        private fun define(key: String, implementation: Any) {
            val rollBack = scope.build()
            try {
                val current = scope[key] as? Value.Invokable
                if (current != null) {
                    redefine(key, current, implementation)
                } else {
                    if (implementation is DependencyAware) {
                        val dependencies = HashSet<String>()
                        val withDependencies = implementation.withDependencies(
                            DependencyTracker(dependencies)
                        )
                        scope[key] = Value.Invokable(withDependencies)
                        updateDependencies(key, null, dependencies)
                    } else {
                        scope[key] = Value.Invokable(implementation)
                    }
                }
            } catch (ex: Exception) {
                scope = rollBack.builder()
                throw ex
            }
        }

        private fun redefine(
            key: String,
            oldValue: Value.Invokable,
            newImplementation: Any
        ) {
            val oldDependencies: Set<String>?
            if (oldValue.value is DependencyAware) {
                val dependencies = HashSet<String>()
                oldValue.value.withDependencies(
                    DependencyTracker(dependencies)
                )
                oldDependencies = dependencies
            } else {
                oldDependencies = null
            }
            if (newImplementation is DependencyAware) {
                val dependencies = HashSet<String>()
                val withDependencies = newImplementation.withDependencies(
                    DependencyTracker(dependencies)
                )
                scope[key] = oldValue.update(withDependencies)
                updateDependencies(key, oldDependencies, dependencies)
            } else {
                scope[key] = oldValue.update(newImplementation)
            }
            if (oldValue.dependants.isNotEmpty()) {
                updateDependants(oldValue.dependants)
            }
        }

        private fun updateDependencies(
            key: String,
            oldDependencies: Set<String>?,
            newDependencies: Set<String>
        ) {
            if (oldDependencies != null) {
                for (d in oldDependencies) {
                    if (d == key) {
                        // not reached
                        continue
                    }
                    if (d !in newDependencies) {
                        val current = scope[d]
                        if (current is Value.Invokable) {
                            val newEntry = current.removeDependants(key)
                            if (newEntry !== current) {
                                scope[d] = newEntry
                            }
                        }
                    }
                }
            }
            for (d in newDependencies) {
                if (d == key) {
                    error("cyclic dependency: $key -> $key")
                }
                if (oldDependencies == null || d !in oldDependencies) {
                    val current = scope[d]
                    if (current is Value.Invokable) {
                        val newEntry = current.withDependants(key)
                        if (newEntry !== current) {
                            scope[d] = newEntry
                        }
                    }
                }
            }
        }

        private fun updateDependants(
            dependants: Iterable<String>,
            visited: HashSet<String> = HashSet()
        ) {
            for (key in dependants) {
                if (key in visited) {
                    error("cyclic dependency: $key -> ... -> $key")
                }
                visited.add(key)
                val existing = scope[key] as? Value.Invokable
                    //this dependant seems gone
                    ?: continue
                val dependencyAware = existing.value as? DependencyAware
                    //this dependant seems not dependant anymore
                    ?: continue
                val withDependencies = dependencyAware.withDependencies(this)
                if (withDependencies === dependencyAware) {
                    //this dependant seems not dependant anymore
                    continue
                }
                scope[key] = existing.update(withDependencies)
                if (existing.dependants.isNotEmpty()) {
                    updateDependants(existing.dependants, visited)
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
                return this@Builder.getCommandTag(key)
            }

            override fun getControlTag(key: Key.Control): ControlTag? {
                dependencies.add(key.value)
                return this@Builder.getControlTag(key)
            }

            override fun getOpFunction(key: Key.Unary): UnOpFunction? {
                dependencies.add(key.value)
                return this@Builder.getOpFunction(key)
            }

            override fun getBinaryOpFunction(key: Key.Binary): BinOpFunction? {
                dependencies.add(key.value)
                return this@Builder.getBinaryOpFunction(key)
            }

            override fun getFunction(key: Key.Call): Function? {
                dependencies.add(key.value)
                return this@Builder.getFunction(key)
            }

            override fun <T : Method> getMethod(key: Key.Apply<T>): T? {
                dependencies.add(key.value)
                return this@Builder.getMethod(key)
            }

            override suspend fun evalExpression(expression: Expression): Any? {
                throw IllegalStateException(
                    "cannot evaluate expressions on $this"
                )
            }
        }
    }

    private class ReadOnlyContext<R>(
        val scope: PersistentMap<String, Any>,
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
            return when (val result = scope[name]) {
                null -> Undefined("undefined variable: '$name'")
                is Value -> result.getVar(name)

                else -> result
            }
        }

        override fun getCommandTag(key: Key.Command): CommandTag? {
            return scope[key.value]?.invokableValue()
        }

        override fun getControlTag(key: Key.Control): ControlTag? {
            return scope[key.value]?.invokableValue()
        }

        override fun getOpFunction(key: Key.Unary): UnOpFunction? {
            return scope[key.value]?.invokableValue()
        }

        override fun getBinaryOpFunction(key: Key.Binary): BinOpFunction? {
            return scope[key.value]?.invokableValue()
        }

        override fun getFunction(key: Key.Call): Function? {
            return scope[key.value]?.invokableValue()
        }

        override fun <T: Method> getMethod(key: Key.Apply<T>): T? {
            @Suppress("UNCHECKED_CAST")
            return (scope[key.value] ?: return null)
                .invokableValue<Method>() as T
        }
    }
}
