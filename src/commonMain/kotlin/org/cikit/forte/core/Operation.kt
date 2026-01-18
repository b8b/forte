package org.cikit.forte.core

import org.cikit.forte.lib.core.FilterGet
import org.cikit.forte.parser.Expression

sealed class Operation {

    abstract val expression: Expression
    abstract operator fun invoke(ctx: Context<*>, state: EvaluatorState)

    class Const(
        override val expression: Expression,
        val value: Any?
    ) : Operation() {
        override fun toString(): String {
            return expression.toString()
        }

        override fun invoke(ctx: Context<*>, state: EvaluatorState) {
            state.addLast(expression, value)
        }
    }

    class InitString(override val expression: Expression) : Operation() {
        override fun toString(): String {
            return "InitString"
        }

        override fun invoke(ctx: Context<*>, state: EvaluatorState) {
            state.addLast(expression, StringBuilder())
        }
    }

    class ConcatToString(override val expression: Expression) : Operation() {
        override fun toString(): String {
            return "ConcatToString"
        }

        override fun invoke(ctx: Context<*>, state: EvaluatorState) {
            val convertedValue = when (val value = state.removeLast()) {
                is CharSequence -> value
                else -> {
                    val toString = try {
                        ctx.filterString
                    } catch (ex: Exception) {
                        throw EvalException(
                            expression,
                            "failed to convert value " +
                                    "of type '${typeName(value)} to string",
                            ex
                        )
                    }
                    val result = toString.invoke(value, NamedArgs.Empty)
                    if (result !is CharSequence) {
                        throw EvalException(
                            expression,
                            "failed to convert value " +
                                    "of type '${typeName(value)}' to string"
                        )
                    }
                    result
                }
            }
            (state.last() as StringBuilder).append(convertedValue)
        }
    }

    class BuildString(override val expression: Expression) : Operation() {
        override fun toString(): String {
            return "BuildString"
        }

        override fun invoke(ctx: Context<*>, state: EvaluatorState) {
            val builder = state.last() as StringBuilder
            state.setLast(expression, builder.toString())
        }
    }

    class InitArray(
        override val expression: Expression,
        val size: Int
    ) : Operation() {
        override fun toString(): String {
            return "InitArray"
        }

        override fun invoke(ctx: Context<*>, state: EvaluatorState) {
            state.addLast(expression, arrayOfNulls<Any?>(size))
        }
    }

    class AddArrayElement(
        override val expression: Expression,
        val index: Int
    ) : Operation() {
        override fun toString(): String {
            return "AddArrayElement"
        }

        @Suppress("UNCHECKED_CAST")
        override fun invoke(ctx: Context<*>, state: EvaluatorState) {
            val value = state.removeLast()
            (state.last() as Array<Any?>)[index] = value
        }
    }

    class BuildArray(override val expression: Expression) : Operation() {
        override fun toString(): String {
            return "BuildArray"
        }

        override fun invoke(ctx: Context<*>, state: EvaluatorState) {
            val builder = state.last() as Array<*>
            state.setLast(expression, builder.toList())
        }
    }

    class InitObject(
        override val expression: Expression,
        val size: Int
    ) : Operation() {
        override fun toString(): String {
            return "InitObject"
        }

        override fun invoke(ctx: Context<*>, state: EvaluatorState) {
            state.addLast(expression, LinkedHashMap<String, Any?>(size))
        }
    }

    class AddPairToObject(override val expression: Expression) : Operation() {
        override fun toString(): String {
            return "AddPairToObject"
        }

        @Suppress("UNCHECKED_CAST")
        override fun invoke(ctx: Context<*>, state: EvaluatorState) {
            val value = state.removeLast()
            val key = state.removeLast()
            (state.last() as MutableMap<Any?, Any?>)[key] = value
        }
    }

    class BuildObject(override val expression: Expression) : Operation() {
        override fun toString(): String {
            return "BuildObject"
        }

        override fun invoke(ctx: Context<*>, state: EvaluatorState) {
            val builder = state.last() as MutableMap<*, *>
            state.setLast(expression, builder.toMap())
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
            state.addLast(expression, result)
        }
    }

    class UnOp(
        override val expression: Expression,
        val key: Context.Key.Unary,
        val allowHidden: Boolean = false
    ) : Operation() {
        override fun toString(): String {
            return "UnOp($key)"
        }

        override fun invoke(ctx: Context<*>, state: EvaluatorState) {
            val function = ctx.getOpFunction(key)
                ?: throw EvalException(
                    expression,
                    "unary operator function '$key' " +
                            "not defined"
                )
            if (!allowHidden && function.isHidden) {
                throw EvalException(
                    expression,
                    "unary operator function '$key' " +
                            "is hidden"
                )
            }
            val value = state.last()
            val result = try {
                function.invoke(value)
            } catch (ex: EvalException) {
                throw ex
            } catch (ex: Exception) {
                throw EvalException(expression, ex.toString(), ex)
            }
            state.setLast(expression, result)
        }
    }

    class CallFunction(
        override val expression: Expression.FunctionCall,
        val key: Context.Key.Call,
        val argNames: List<String>,
        val allowHidden: Boolean = false
    ) : Operation() {
        override fun toString(): String {
            return "Call(${expression.name})"
        }

        override fun invoke(ctx: Context<*>, state: EvaluatorState) {
            val function = ctx.getFunction(key)
                ?: throw EvalException(
                    expression,
                    "function '$key' not defined"
                )
            if (!allowHidden && function.isHidden) {
                throw EvalException(
                    expression,
                    "function '$key' is hidden"
                )
            }
            val args = NamedArgs((state.last() as Array<*>).toList(), argNames)
            val result = try {
                function(args)
            } catch (ex: EvalException) {
                throw ex
            } catch (ex: Exception) {
                throw EvalException(expression, ex.toString(), ex)
            }
            state.setLast(expression, result)
        }
    }

    class CallMethod(
        override val expression: Expression,
        val method: Context.Key.Apply<*>,
        val argNames: List<String>,
        val allowHidden: Boolean = false
    ) : Operation() {
        override fun toString(): String {
            return "CallMethod($method)"
        }

        override fun invoke(ctx: Context<*>, state: EvaluatorState) {
            val function: Method = ctx.getMethod(method)
                ?: throw EvalException(
                    expression,
                    "method '$method' not defined"
                )
            if (!allowHidden && function.isHidden) {
                throw EvalException(
                    expression,
                    "method '$method' is hidden"
                )
            }
            val args = NamedArgs(
                (state.removeLast() as Array<*>).toList(),
                argNames
            )
            val subject = if (function.isRescue) {
                state.rescueLast()
            } else {
                state.last()
            }
            val result = try {
                function.invoke(subject, args)
            } catch (ex: EvalException) {
                throw ex
            } catch (ex: Exception) {
                throw EvalException(expression, ex.toString(), ex)
            }
            if (result !== subject) {
                state.setLast(expression, result)
            }
        }
    }

    class CondBinOp(
        override val expression: Expression,
        val key: Context.Key.Binary,
        val condOperationsCount: Int,
        val allowHidden: Boolean = false
    ) : Operation() {
        override fun toString(): String {
            return "CondBinOp($key)"
        }

        override fun invoke(ctx: Context<*>, state: EvaluatorState) {
            val function = ctx.getBinaryOpFunction(key)
                ?: throw EvalException(
                    expression,
                    "operator function '$key' not defined"
                )
            if (!allowHidden && function.isHidden) {
                throw EvalException(
                    expression,
                    "operator function '$key' is hidden"
                )
            }
            if (function is ConditionalBinOpFunction) {
                val value = if (function.isRescue) {
                    state.rescueLast()
                } else {
                    state.last()
                }
                val condResult = try {
                    function.checkCondition(value)
                } catch (ex: EvalException) {
                    throw ex
                } catch (ex: Exception) {
                    throw EvalException(expression, ex.toString(), ex)
                }
                when (condResult) {
                    is ConditionalResult.Continue -> {
                        state.addLast(expression, function)
                    }
                    is ConditionalResult.Return -> {
                        state.setLast(expression, condResult.value)
                        state.jump(condOperationsCount)
                    }
                }
            } else {
                state.addLast(expression, function)
            }
        }
    }

    class InvokeBinOp(
        override val expression: Expression,
        val name: String
    ) : Operation() {
        override fun toString(): String {
            return "InvokeBinOp($name)"
        }

        override fun invoke(ctx: Context<*>, state: EvaluatorState) {
            val right = state.removeLast()
            val function = state.removeLast() as BinOpFunction
            val left = if (function.isRescue) {
                state.rescueLast()
            } else {
                state.last()
            }
            val result = try {
                function.invoke(left, right)
            } catch (ex: EvalException) {
                throw ex
            } catch (ex: Exception) {
                throw EvalException(expression, ex.toString(), ex)
            }
            if (result !== left) {
                state.setLast(expression, result)
            }
        }
    }

    class TransformOp(
        override val expression: Expression,
        val method: Context.Key.Apply<*>,
        val argNames: List<String>,
        val allowHidden: Boolean = false
    ) : Operation() {
        override fun toString(): String {
            return "TransformOp($method)"
        }

        override fun invoke(ctx: Context<*>, state: EvaluatorState) {
            val function: Method = ctx.getMethod(method)
                ?: throw EvalException(
                    expression,
                    "method '$method' not defined"
                )
            if (!allowHidden && function.isHidden) {
                throw EvalException(
                    expression,
                    "method '$method' is hidden"
                )
            }
            val args = NamedArgs(
                (state.removeLast() as Array<*>).toList(),
                argNames
            )
            val subject = if (function.isRescue) {
                state.rescueLast()
            } else {
                state.last()
            }
            val result = try {
                function.invoke(subject, args)
            } catch (ex: EvalException) {
                throw ex
            } catch (ex: Exception) {
                throw EvalException(expression, ex.toString(), ex)
            }
            if (result !== subject) {
                state.setLast(expression, result)
            }
        }
    }

    class ApplyGet(override val expression: Expression) : Operation() {
        override fun toString(): String {
            return "Get()"
        }

        override fun invoke(ctx: Context<*>, state: EvaluatorState) {
            val args = NamedArgs(
                (state.removeLast() as Array<*>).toList(),
                FilterGet.singleArg
            )
            val filterGet = ctx.filterGet
            val value = if (filterGet.isRescue) {
                state.rescueLast()
            } else {
                state.last()
            }
            val result = filterGet(value, args)
            if (result !== value) {
                state.setLast(expression, result)
            }
        }
    }

    class ApplySlice(
        override val expression: Expression,
        val argNames: List<String>,
    ) : Operation() {
        override fun toString(): String {
            return "Slice()"
        }

        override fun invoke(ctx: Context<*>, state: EvaluatorState) {
            val args = NamedArgs(
                (state.removeLast() as Array<*>).toList(),
                argNames
            )
            val filterSlice = ctx.filterSlice
            val value = if (filterSlice.isRescue) {
                state.rescueLast()
            } else {
                state.last()
            }
            val result = filterSlice(value, args)
            if (result !== value) {
                state.setLast(expression, result)
            }
        }
    }
}
