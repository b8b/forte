package org.cikit.forte.core

interface FilterMethod : Method {
    companion object {
        val OPERATOR = MethodOperator<FilterMethod>("pipe")
    }

    override val operator: String
        get() = OPERATOR.value
}

fun <R, T: FilterMethod> Context.Builder<R>.defineFilter(
    key: Context.Key.Apply<T>,
    implementation: T
): Context.Builder<R> {
    defineMethod(key, implementation)
    return this
}

fun <R> Context.Builder<R>.defineFilter(
    name: String,
    hidden: Boolean = false,
    rescue: Boolean = false,
    implementation: (subject: Any?, args: NamedArgs) -> Any?
): Context.Builder<R> = object : FilterMethod {
    override val isHidden: Boolean
        get() = hidden
    override val isRescue: Boolean
        get() = rescue
    override fun invoke(subject: Any?, args: NamedArgs): Any? {
        return implementation(subject, args)
    }
}.let { defineFilter(Context.Key.Apply(name, it.operator), it) }

fun <R, T: TestMethod> Context.Builder<R>.defineTest(
    key: Context.Key.Apply<T>,
    implementation: T
): Context.Builder<R> {
    defineMethod(key, implementation)
    return this
}

interface TestMethod : Method {
    companion object {
        val OPERATOR = MethodOperator<TestMethod>("is")
    }

    override val operator: String
        get() = OPERATOR.value

    override fun invoke(subject: Any?, args: NamedArgs): Any?
}

fun <R> Context.Builder<R>.defineTest(
    name: String,
    hidden: Boolean = false,
    rescue: Boolean = false,
    implementation: (subject: Any?, args: NamedArgs) -> Boolean
): Context.Builder<R> = object : TestMethod {
    override val isHidden: Boolean
        get() = hidden
    override val isRescue: Boolean
        get() = rescue
    override fun invoke(subject: Any?, args: NamedArgs): Boolean {
        return implementation(subject, args)
    }
}.let { defineTest(Context.Key.Apply(name, it.operator), it) }
