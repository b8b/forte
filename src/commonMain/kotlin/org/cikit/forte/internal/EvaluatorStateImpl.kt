package org.cikit.forte.internal

import org.cikit.forte.core.Context
import org.cikit.forte.core.EvalException
import org.cikit.forte.core.EvaluatorState
import org.cikit.forte.core.FilterMethod
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.Operation
import org.cikit.forte.core.Suspended
import org.cikit.forte.core.Undefined
import org.cikit.forte.core.compile
import org.cikit.forte.parser.Expression

internal class EvaluatorStateImpl : EvaluatorState {

    private var stack = arrayOfNulls<Any?>(2)
    private var stackSize = 0
    private var ip = 0
    private var isSuspended = false

    override fun isEmpty(): Boolean = stackSize == 0

    suspend fun evalExpression(
        ctx: Context.Builder<*>,
        expression: Expression
    ): Any? {
        val finalExpression =
            if (expression.operations === UNCOMPILED_EXPRESSION) {
                expression.compile()
            } else {
                expression
            }
        eval(ctx, finalExpression.operations)
        val result = removeLast()
        require(isEmpty())
        return result
    }

    suspend fun renderExpression(
        ctx: Context.Builder<*>,
        expression: Expression
    ): CharSequence {
        val finalExpression =
            if (expression.operations === UNCOMPILED_EXPRESSION) {
                expression.compile()
            } else {
                expression
            }
        eval(ctx, finalExpression.operations)
        if (isSuspended || rescueLast() !is CharSequence) {
            applyFilter(expression, ctx.filterString, NamedArgs.Empty)
        }
        val result = removeLast()
        require(isEmpty())
        require(result is CharSequence)
        return result
    }

    suspend fun eval(
        ctx: Context.Builder<*>,
        operations: List<Operation>,
    ) {
        if (isSuspended) {
            throw IllegalStateException("evaluation is suspended")
        }
        this.ip = 0
        while (true) {
            while (!isSuspended && ip < operations.size) {
                val op = operations[ip++]
                op(ctx, this)
            }
            require(ip <= operations.size) {
                "invalid jump to $ip"
            }
            if (isSuspended) {
                val i = stackSize - 1
                val value = stack[i] as UndefinedResult
                var result = try {
                    (value.value as Suspended).eval(ctx)
                } catch (ex: EvalException) {
                    throw ex
                } catch (ex: Exception) {
                    throw EvalException(value.expression, ex.message, ex)
                }
                if (result is Undefined) {
                    result = UndefinedResult(value.expression, result)
                }
                stack[i] = result
                isSuspended = false
                continue
            }
            break
        }
    }

    override fun jump(relOffset: Int) {
        require(relOffset > 0) {
            "invalid non positive jump: $relOffset"
        }
        ip += relOffset
    }

    override fun addLast(expression: Expression, value: Any?) {
        if (isSuspended) {
            throw IllegalStateException("evaluation is suspended")
        }
        if (stack.size == stackSize) {
            stack = stack.copyOf(stackSize * 2)
        }
        if (value is Undefined) {
            if (value is Suspended) {
                isSuspended = true
            }
            stack[stackSize++] = UndefinedResult(expression, value)
        } else {
            stack[stackSize++] = value
        }
    }

    override fun setLast(expression: Expression, value: Any?) {
        if (isSuspended) {
            throw IllegalStateException("evaluation is suspended")
        }
        if (value is Undefined) {
            if (value is Suspended) {
                isSuspended = true
            }
            stack[stackSize - 1] = UndefinedResult(expression, value)
        } else {
            stack[stackSize - 1] = value
        }
    }

    override fun removeLast(): Any? {
        if (isSuspended) {
            throw IllegalStateException("evaluation is suspended")
        }
        val result = stack[--stackSize]
        stack[stackSize] = null
        if (result is UndefinedResult) {
            throw EvalException(result.expression, result.value.message)
        }
        return result
    }

    override fun last(): Any? {
        if (isSuspended) {
            throw IllegalStateException("evaluation is suspended")
        }
        val result = stack[stackSize - 1]
        if (result is UndefinedResult) {
            throw EvalException(result.expression, result.value.message)
        }
        return result
    }

    override fun rescueLast(): Any? {
        if (isSuspended) {
            throw IllegalStateException("evaluation is suspended")
        }
        val result = stack[stackSize - 1]
        if (result is UndefinedResult) {
            return result.value
        }
        return result
    }

    override fun rescueAndRemoveLast(): Any? {
        if (isSuspended) {
            throw IllegalStateException("evaluation is suspended")
        }
        val result = stack[--stackSize]
        stack[stackSize] = null
        if (result is UndefinedResult) {
            return result.value
        }
        return result
    }

    private fun applyFilter(
        expression: Expression,
        implementation: FilterMethod,
        args: NamedArgs
    ) {
        if (isSuspended) {
            throw IllegalStateException("evaluation is suspended")
        }
        var value = stack[stackSize - 1]
        if (value is UndefinedResult) {
            if (implementation.isRescue) {
                value = value.value
            } else {
                throw EvalException(value.expression, value.value.message)
            }
        }
        val result = implementation(value, args)
        if (result !== value) {
            if (result is Undefined) {
                if (result is Suspended) {
                    isSuspended = true
                }
                stack[stackSize - 1] = UndefinedResult(expression, result)
            } else {
                stack[stackSize - 1] = result
            }
        }
    }

    private class UndefinedResult(
        val expression: Expression,
        val value: Undefined
    ) {
        override fun toString(): String {
            return "UndefinedResult($expression => $value)"
        }
    }
}
