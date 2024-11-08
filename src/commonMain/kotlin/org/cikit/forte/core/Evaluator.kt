package org.cikit.forte.core

import org.cikit.forte.parser.Expression
import org.cikit.forte.parser.Node
import org.cikit.forte.parser.ParsedTemplate

class Branch(
    val name: String,
    val args: Map<String, Expression>,
    val body: ParsedTemplate
)

fun interface CommandFunction {
    operator fun invoke(ctx: Context.Builder<*>, args: Map<String, Expression>)
}

fun interface ControlFunction {
    operator fun invoke(ctx: Context.Builder<*>, branches: List<Branch>)
}

fun interface UnOpFunction {
    operator fun invoke(ctx: Context<*>, arg: Any?): Any?
}

fun interface BinOpFunction {
    operator fun invoke(ctx: Context<*>, left: Any?, right: Any?): Any?
}

interface ConditionalBinOpFunction : BinOpFunction, UnOpFunction {
    fun condition(ctx: Context<*>, arg: Any?): Boolean

    override fun invoke(ctx: Context<*>, left: Any?, right: Any?): Any? {
        return if (condition(ctx, left)) {
            invoke(ctx, right)
        } else {
            left
        }
    }
}

fun interface Function {
    operator fun invoke(ctx: Context<*>, args: NamedArgs): Any?
}

fun interface Method {
    operator fun invoke(ctx: Context<*>, subject: Any?, args: NamedArgs): Any?
}

open class Undefined(open val message: String)

fun <R> Context.Builder<R>.evalTemplate(
    template: ParsedTemplate
): Context.Builder<R> {
    for (node in template.nodes) {
        evalCommand(template, node)
    }
    return this
}

private fun Context.Builder<*>.evalCommand(
    template: ParsedTemplate,
    cmd: Node
) {
    when (cmd) {
        is Node.Comment -> {}
        is Node.Command -> {
            cmd.callCommand(this, cmd.name, cmd.content)
        }

        is Node.Control -> {
            val branches = (listOf(cmd.first) + cmd.branches).map { branch ->
                Branch(
                    name = branch.first.name,
                    args = branch.first.content,
                    body = ParsedTemplate(
                        template.input,
                        template.path,
                        branch.body
                    )
                )
            }
            cmd.callControl(this, cmd.first.first.name, branches)
        }

        is Node.Text -> {
            val text = template.input.substring(
                cmd.content.first..cmd.content.last
            )
            val trimmed = when {
                cmd.trimLeft && cmd.trimRight -> text.trim()
                cmd.trimLeft -> text.trimStart()
                cmd.trimRight -> text.trimEnd()
                else -> text
            }
            if (trimmed.isNotEmpty()) {
                emit(trimmed)
            }
        }

        is Node.Emit -> {
            emit(evalExpression(cmd.content))
        }
    }
}

private class UndefinedResult(
    val expression: Expression,
    val value: Undefined
)

fun Context<*>.evalExpression(expression: Expression): Any? {
    val result = evalExpressionInternal(expression)
    if (result is UndefinedResult) {
        throw EvalException(result.expression, result.value.message)
    }
    return result
}

private fun Context<*>.evalExpressionInternal(
    expression: Expression
): Any? = when (expression) {
    is Expression.Malformed -> throw EvalException(
        expression,
        "malformed expression: $expression"
    )
    is Expression.SubExpression -> {
        evalExpressionInternal(expression.content)
    }
    is Expression.Variable -> {
        val result = try {
            getVar(expression.name)
        } catch (ex: EvalException) {
            throw ex
        } catch (ex: Exception) {
            throw EvalException(expression, ex.toString(), ex)
        }
        if (result is Undefined) {
            UndefinedResult(expression, result)
        } else {
            result
        }
    }
    is Expression.NullLiteral -> null
    is Expression.BooleanLiteral -> expression.value
    is Expression.NumericLiteral -> expression.value
    is Expression.StringLiteral -> expression.value
    is Expression.StringInterpolation -> expression.children
        .joinToString("") { evalExpression(it).toString() }
    is Expression.ArrayLiteral -> expression.children.map {
        evalExpression(it)
    }
    is Expression.ObjectLiteral -> expression.pairs.associate { (k, v) ->
        val key = when (k) {
            is Expression.Variable -> k.name
            is Expression.StringLiteral -> k.value
            else -> evalExpression(k)
        }
        key to evalExpression(v)
    }
    is Expression.CompAccess -> {
        when (val subject = evalExpressionInternal(expression.left)) {
            is UndefinedResult -> subject
            else -> {
                val result = try {
                    val key = evalExpression(expression.right)
                    getBinaryOpFunction("get")
                        ?.invoke(this, subject, key)
                        ?: Undefined("get operator function not defined")
                } catch (ex: EvalException) {
                    throw ex
                } catch (ex: Exception) {
                    throw EvalException(expression, ex.toString(), ex)
                }
                if (result is Undefined) {
                    UndefinedResult(expression, result)
                } else {
                    result
                }
            }
        }
    }
    is Expression.Access -> {
        when (val subject = evalExpressionInternal(expression.left)) {
            is UndefinedResult -> subject
            else -> {
                val result = try {
                    getBinaryOpFunction("get")
                        ?.invoke(this, subject, expression.name)
                        ?: Undefined("get operator function not defined")
                } catch (ex: EvalException) {
                    throw ex
                } catch (ex: Exception) {
                    throw EvalException(expression, ex.toString(), ex)
                }
                if (result is Undefined) {
                    UndefinedResult(expression, result)
                } else {
                    result
                }
            }
        }
    }
    is Expression.FunctionCall -> {
        val function = getFunction(expression.name) ?: throw EvalException(
            expression,
            "undefined function ${expression.name}"
        )
        val result = try {
            function(this, evalArgs(expression.args))
        } catch (ex: EvalException) {
            throw ex
        } catch (ex: Exception) {
            throw EvalException(expression, ex.toString(), ex)
        }
        if (result is Undefined) {
            UndefinedResult(expression, result)
        } else {
            result
        }
    }
    is Expression.UnOp -> {
        val function = getOpFunction(expression.decl.name)
            ?: throw EvalException(
                expression,
                "undefined unary operator function ${expression.decl.name}"
            )
        val result = try {
            function.invoke(this, evalExpression(expression.right))
        } catch (ex: EvalException) {
            throw ex
        } catch (ex: Exception) {
            throw EvalException(expression, ex.toString(), ex)
        }
        if (result is Undefined) {
            UndefinedResult(expression, result)
        } else {
            result
        }
    }
    is Expression.InvokeOp -> {
        when (expression.left) {
            is Expression.Access -> {
                val subject = evalExpression(expression.left.left)
                val methodName = expression.left.name
                val function = getMethod(methodName)
                    ?: throw EvalException(
                        expression,
                        "undefined method $methodName"
                    )
                val result = try {
                    function.invoke(this, subject, evalArgs(expression.args))
                } catch (ex: EvalException) {
                    throw ex
                } catch (ex: Exception) {
                    throw EvalException(expression.left, ex.toString(), ex)
                }
                if (result is Undefined) {
                    UndefinedResult(expression.left, result)
                } else {
                    result
                }
            }
            else -> {
                val subject = evalExpression(expression.left)
                val function = getMethod("invoke")
                    ?: throw EvalException(
                        expression,
                        "invoke method not defined"
                    )
                val result = try {
                    function.invoke(this, subject, evalArgs(expression.args))
                } catch (ex: EvalException) {
                    throw ex
                } catch (ex: Exception) {
                    throw EvalException(expression.left, ex.toString(), ex)
                }
                if (result is Undefined) {
                    UndefinedResult(expression.left, result)
                } else {
                    result
                }
            }
        }
    }
    is Expression.TransformOp -> {
        val operator = expression.decl.name
        val method = expression.name
        val subject = evalExpressionInternal(expression.left)
        val (function, finalSubject) = if (subject is UndefinedResult) {
            val f = getRescueMethod(method, operator) ?: throw EvalException(
                subject.expression,
                subject.value.message
            )
            f to subject.value
        } else {
            val f = getMethod(method, operator) ?: throw EvalException(
                expression,
                "undefined `$operator` operator method $method"
            )
            f to subject
        }
        val result = try {
            function.invoke(this, finalSubject, evalArgs(expression.args))
        } catch (ex: EvalException) {
            throw ex
        } catch (ex: Exception) {
            throw EvalException(expression, ex.toString(), ex)
        }
        if (result is Undefined) {
            UndefinedResult(expression, result)
        } else {
            result
        }
    }
    is Expression.BinOp -> {
        val subject = evalExpression(expression.left)
        val function = getBinaryOpFunction(expression.decl.name)
            ?: throw EvalException(
                expression,
                "undefined operator function ${expression.decl.name}"
            )
        val result = try {
            if (function is ConditionalBinOpFunction) {
                if (function.condition(this, subject)) {
                    function.invoke(this, evalExpression(expression.right))
                } else {
                    subject
                }
            } else {
                function.invoke(this, subject, evalExpression(expression.right))
            }
        } catch (ex: EvalException) {
            throw ex
        } catch (ex: Exception) {
            throw EvalException(expression, ex.toString(), ex)
        }
        if (result is Undefined) {
            UndefinedResult(expression, result)
        } else {
            result
        }
    }
}

private fun Node.callCommand(
    ctx: Context.Builder<*>,
    name: String,
    args: Map<String, Expression>
) {
    val function = ctx.getCommand(name)
        ?: throw EvalException(this, "undefined command $name")
    return try {
        function.invoke(ctx, args)
    } catch (ex: EvalException) {
        throw ex
    } catch (ex: Exception) {
        throw EvalException(this, ex.toString(), ex)
    }
}

private fun Node.callControl(
    ctx: Context.Builder<*>,
    name: String,
    branches: List<Branch>
) {
    val function = ctx.getControl(name)
        ?: throw EvalException(this, "undefined command $name")
    return try {
        function.invoke(ctx, branches)
    } catch (ex: EvalException) {
        throw ex
    } catch (ex: Exception) {
        throw EvalException(this, ex.toString(), ex)
    }
}

private fun Context<*>.evalArgs(
    namedArgs: Expression.NamedArgs
): NamedArgs {
    if (namedArgs.values.isEmpty()) {
        return NamedArgs.Empty
    }
    val providedValues = namedArgs.values.map { expression ->
        evalExpression(expression)
    }
    return NamedArgs(providedValues, namedArgs.names)
}
