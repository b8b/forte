package org.cikit.forte.eval

import org.cikit.forte.core.Context
import org.cikit.forte.core.EvalException
import org.cikit.forte.core.Suspended
import org.cikit.forte.core.Undefined
import org.cikit.forte.parser.Expression
import org.cikit.forte.parser.ParsedTemplate

sealed class EvaluationResult {
    abstract suspend fun get(): Any?
    abstract fun getOrThrow(): Any?
    abstract fun getOrNull(): Any?

    private class Succeeded(val value: Any?) : EvaluationResult() {
        override suspend fun get(): Any? = value
        override fun getOrThrow(): Any? = value
        override fun getOrNull(): Any? = value
    }

    companion object {
        fun succeeded(value: Any?): EvaluationResult = Succeeded(value)
    }
}

class UndefinedResult(
    val expression: Expression,
    val value: Undefined
) : EvaluationResult() {
    override fun toString(): String {
        return "UndefinedResult($expression => $value)"
    }

    override suspend fun get(): Any? {
        val undefinedValue: Undefined = if (value is Suspended) {
            val result = value.eval()
            result as? Undefined ?: return result
        } else {
            value
        }
        throw EvalException(expression, undefinedValue.message)
    }

    override fun getOrThrow(): Any? {
        throw EvalException(expression, value.message)
    }

    override fun getOrNull(): Any? = null
}

suspend fun <R> Context.Builder<R>.evalTemplate(
    template: ParsedTemplate
): Context.Builder<R> {
    try {
        for (cmd in template.nodes) {
            cmd.execute?.let { proc ->
                if (proc === UNCOMPILED_NODE) {
                    cmd.compile(template).execute?.invoke(this)
                } else {
                    proc.invoke(this)
                }
            }
        }
        return this
    } catch (ex: EvalException) {
        ex.setTemplate(template)
        throw ex
    }
}

suspend fun Context<*>.evalExpression(expression: Expression): Any? {
    val finalExpression = if (expression.operations === UNCOMPILED_EXPRESSION) {
        expression.compile()
    } else {
        expression
    }
    val state = EvaluatorState(finalExpression.operations)
    while (true) {
        val op = state.next() ?: break
        op(this, state)
        (state.lastOrNull() as? UndefinedResult)?.let { result ->
            if (result.value is Suspended) {
                state.setLast(result.value.eval())
            }
        }
    }
    val result = state.removeLast()
    if (result is UndefinedResult) {
        return result.get()
    }
    return result
}

fun Context<*>.tryEvalExpression(expression: Expression): EvaluationResult {
    val finalExpression = if (expression.operations === UNCOMPILED_EXPRESSION) {
        expression.compile()
    } else {
        expression
    }
    val state = EvaluatorState(finalExpression.operations)
    while (true) {
        val op = state.next() ?: break
        op(this, state)
        (state.lastOrNull() as? UndefinedResult)?.let { result ->
            if (result.value is Suspended) {
                val result = Suspended {
                    val value = result.value.eval()
                    state.setLast(value)
                    while (true) {
                        val op = state.next() ?: break
                        op(this, state)
                        (state.lastOrNull() as? UndefinedResult)?.let {
                            if (it.value is Suspended) {
                                state.setLast(it.value.eval())
                            }
                        }
                    }
                    val result = state.removeLast()
                    if (result is UndefinedResult) {
                        result.get()
                    } else {
                        result
                    }
                }
                return UndefinedResult(op.expression, result)
            }
        }
    }
    val result = state.removeLast()
    if (result is EvaluationResult) {
        return result
    }
    return EvaluationResult.succeeded(result)
}
