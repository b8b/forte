package org.cikit.forte.eval

import org.cikit.forte.core.BinOpFunction
import org.cikit.forte.core.ConditionalBinOpFunction
import org.cikit.forte.core.Context
import org.cikit.forte.core.EvalException
import org.cikit.forte.core.Method
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.Undefined
import org.cikit.forte.parser.Expression

sealed class Operation {

    abstract val expression: Expression
    abstract operator fun invoke(ctx: Context<*>, state: EvaluatorState)

    class Const(override val expression: Expression, val value: Any?) : Operation() {
        override fun toString(): String {
            return expression.toString()
        }

        override fun invoke(ctx: Context<*>, state: EvaluatorState) {
            state.addLast(value)
        }
    }

    class InitString(override val expression: Expression) : Operation() {
        override fun toString(): String {
            return "InitString"
        }

        override fun invoke(ctx: Context<*>, state: EvaluatorState) {
            state.addLast(StringBuilder())
        }
    }

    class ConcatToString(override val expression: Expression) : Operation() {
        override fun toString(): String {
            return "ConcatToString"
        }

        override fun invoke(ctx: Context<*>, state: EvaluatorState) {
            val value = state.removeLast()
            if (value is UndefinedResult) {
                throw EvalException(value.expression, value.value.message)
            }
            (state.last() as StringBuilder).append(value)
        }
    }

    class BuildString(override val expression: Expression) : Operation() {
        override fun toString(): String {
            return "BuildString"
        }

        override fun invoke(ctx: Context<*>, state: EvaluatorState) {
            val builder = state.last() as StringBuilder
            state.setLast(builder.toString())
        }
    }

    class InitArray(override val expression: Expression, val size: Int) : Operation() {
        override fun toString(): String {
            return "InitArray"
        }

        override fun invoke(ctx: Context<*>, state: EvaluatorState) {
            state.addLast(arrayOfNulls<Any?>(size))
        }
    }

    class AddArrayElement(override val expression: Expression, val index: Int) : Operation() {
        override fun toString(): String {
            return "AddArrayElement"
        }

        @Suppress("UNCHECKED_CAST")
        override fun invoke(ctx: Context<*>, state: EvaluatorState) {
            val value = state.removeLast()
            if (value is UndefinedResult) {
                throw EvalException(value.expression, value.value.message)
            }
            (state.last() as Array<Any?>)[index] = value
        }
    }

    class BuildArray(override val expression: Expression) : Operation() {
        override fun toString(): String {
            return "BuildArray"
        }

        override fun invoke(ctx: Context<*>, state: EvaluatorState) {
            val builder = state.last() as Array<*>
            state.setLast(builder.toList())
        }
    }

    class InitObject(override val expression: Expression, val size: Int) : Operation() {
        override fun toString(): String {
            return "InitObject"
        }

        override fun invoke(ctx: Context<*>, state: EvaluatorState) {
            state.addLast(LinkedHashMap<String, Any?>(size))
        }
    }

    class AddPairToObject(override val expression: Expression) : Operation() {
        override fun toString(): String {
            return "AddPairToObject"
        }

        @Suppress("UNCHECKED_CAST")
        override fun invoke(ctx: Context<*>, state: EvaluatorState) {
            val value = state.removeLast()
            if (value is UndefinedResult) {
                throw EvalException(value.expression, value.value.message)
            }
            val key = state.removeLast()
            if (key is UndefinedResult) {
                throw EvalException(key.expression, key.value.message)
            }
            (state.last() as MutableMap<Any?, Any?>)[key] = value
        }
    }

    class BuildObject(override val expression: Expression) : Operation() {
        override fun toString(): String {
            return "BuildObject"
        }

        override fun invoke(ctx: Context<*>, state: EvaluatorState) {
            val builder = state.last() as MutableMap<*, *>
            state.setLast(builder.toMap())
        }
    }

    class GetVar(override val expression: Expression.Variable) : Operation() {
        override fun toString(): String {
            return "GetVar('${expression.name}')"
        }

        override fun invoke(ctx: Context<*>, state: EvaluatorState) {
            val result = try {
                ctx.getVar(expression.name)
            } catch (ex: EvalException) {
                throw ex
            } catch (ex: Exception) {
                throw EvalException(expression, ex.toString(), ex)
            }
            val value = if (result is Undefined) {
                UndefinedResult(expression, result)
            } else {
                result
            }
            state.addLast(value)
        }
    }

    class UnOp(override val expression: Expression.UnOp) : Operation() {
        override fun toString(): String {
            return "UnOp(${expression.decl.name})"
        }

        override fun invoke(ctx: Context<*>, state: EvaluatorState) {
            val function = ctx.getOpFunction(expression.decl.name)
                ?: throw EvalException(
                    expression,
                    "unary operator function '${expression.decl.name}' " +
                            "not defined"
                )
            var value = state.last()
            if (value is UndefinedResult) {
                throw EvalException(value.expression, value.value.message)
            }
            val result = try {
                function.invoke(ctx, value)
            } catch (ex: EvalException) {
                throw ex
            } catch (ex: Exception) {
                throw EvalException(expression, ex.toString(), ex)
            }
            value = if (result is Undefined) {
                UndefinedResult(expression, result)
            } else {
                result
            }
            state.setLast(value)
        }
    }

    class CallFunction(override val expression: Expression.FunctionCall, val argNames: List<String>) : Operation() {
        override fun toString(): String {
            return "Call(${expression.name})"
        }

        override fun invoke(ctx: Context<*>, state: EvaluatorState) {
            val function = ctx.getFunction(expression.name) ?: throw EvalException(
                expression,
                "function '${expression.name}' not defined"
            )
            val args = NamedArgs((state.last() as Array<*>).toList(), argNames)
            val result = try {
                function(ctx, args)
            } catch (ex: EvalException) {
                throw ex
            } catch (ex: Exception) {
                throw EvalException(expression, ex.toString(), ex)
            }
            val value = if (result is Undefined) {
                UndefinedResult(expression, result)
            } else {
                result
            }
            state.setLast(value)
        }
    }

    class CallMethod(override val expression: Expression, val name: String, val argNames: List<String>) : Operation() {
        override fun toString(): String {
            return "CallMethod($name)"
        }

        override fun invoke(ctx: Context<*>, state: EvaluatorState) {
            val function = ctx.getMethod(name)
                ?: throw EvalException(
                    expression,
                    "method '$name' not defined"
                )
            val args = NamedArgs((state.removeLast() as Array<*>).toList(), argNames)
            var value = state.last()
            if (value !is UndefinedResult) {
                val result = try {
                    function.invoke(ctx, value, args)
                } catch (ex: EvalException) {
                    throw ex
                } catch (ex: Exception) {
                    throw EvalException(expression, ex.toString(), ex)
                }
                value = if (result is Undefined) {
                    UndefinedResult(expression, result)
                } else {
                    result
                }
                state.setLast(value)
            }
        }
    }

    class CondBinOp(override val expression: Expression, val name: String, val condOperationsCount: Int) : Operation() {
        override fun toString(): String {
            return "CondBinOp($name)"
        }

        override fun invoke(ctx: Context<*>, state: EvaluatorState) {
            val function = ctx.getBinaryOpFunction(name)
                ?: throw EvalException(
                    expression,
                    "operator function '$name' not defined"
                )
            val value = state.last()
            if (value is UndefinedResult) {
                throw EvalException(value.expression, value.value.message)
            }
            val condResult = if (function is ConditionalBinOpFunction) {
                try {
                    function.condition(ctx, value)
                } catch (ex: EvalException) {
                    throw ex
                } catch (ex: Exception) {
                    throw EvalException(expression, ex.toString(), ex)
                }
            } else {
                true
            }
            if (!condResult) {
                state.jump(condOperationsCount)
            } else {
                state.addLast(function)
            }
        }
    }

    class InvokeBinOp(override val expression: Expression, val name: String) : Operation() {
        override fun toString(): String {
            return "InvokeBinOp($name)"
        }

        override fun invoke(ctx: Context<*>, state: EvaluatorState) {
            val right = state.removeLast()
            if (right is UndefinedResult) {
                throw EvalException(right.expression, right.value.message)
            }
            val function = state.removeLast() as BinOpFunction
            val left = state.last()
            val result = try {
                function.invoke(ctx, left, right)
            } catch (ex: EvalException) {
                throw ex
            } catch (ex: Exception) {
                throw EvalException(expression, ex.toString(), ex)
            }
            val value = if (result is Undefined) {
                UndefinedResult(expression, result)
            } else {
                result
            }
            state.setLast(value)
        }
    }

    class TransformOp(
        override val expression: Expression,
        val method: String,
        val operator: String,
        val argNames: List<String>
    ) : Operation() {
        override fun toString(): String {
            return "TransformOp(${operator}_$method)"
        }

        override fun invoke(ctx: Context<*>, state: EvaluatorState) {
            val args = NamedArgs((state.removeLast() as Array<*>).toList(), argNames)
            val subject = state.last()
            val function: Method
            val finalSubject = if (subject is UndefinedResult) {
                function = ctx.getRescueMethod(method, operator) ?: return
                subject.value
            } else {
                function = ctx.getMethod(method, operator) ?: throw EvalException(
                    expression,
                    "method '`$operator`$method' not defined"
                )
                subject
            }
            val result = try {
                function.invoke(ctx, finalSubject, args)
            } catch (ex: EvalException) {
                throw ex
            } catch (ex: Exception) {
                throw EvalException(expression, ex.toString(), ex)
            }
            val value = if (result is Undefined) {
                UndefinedResult(expression, result)
            } else {
                result
            }
            state.setLast(value)
        }
    }
}
