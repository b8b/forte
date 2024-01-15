package org.cikit.forte.core

import org.cikit.forte.parser.Expression
import org.cikit.forte.parser.Node
import org.cikit.forte.parser.ParsedTemplate

class Command(
    val name: String,
    val args: Expression.NamedArgs,
    val body: (ctx: Context.Builder<*>) -> Unit
)

fun interface CommandFunction {
    operator fun invoke(ctx: Context.Builder<*>, args: Expression.NamedArgs)
}

fun interface ControlFunction {
    operator fun invoke(ctx: Context.Builder<*>, branches: List<Command>)
}

sealed interface Invokable

fun interface UnaryOpFunction : Invokable {
    operator fun invoke(ctx: Context<*>, arg: Expression): Any?
}

fun interface UnaryFunction : Invokable {
    operator fun invoke(ctx: Context<*>, arg: Expression.NamedArgs): Any?
}

fun interface BinaryFunction : Invokable {
    operator fun invoke(
        ctx: Context<*>,
        left: Any?,
        right: Expression.NamedArgs
    ): Any?
}

fun interface BinaryOpFunction : Invokable {
    operator fun invoke(ctx: Context<*>, left: Any?, right: Expression): Any?
}

open class Undefined(open val message: String)

fun <R> Context.Builder<R>.evalTemplate(template: ParsedTemplate): Context.Builder<R> {
    for (node in template.nodes) {
        evalCommand(template, node)
    }
    return this
}

private fun Context.Builder<*>.evalCommand(template: ParsedTemplate, cmd: Node) {
    when (cmd) {
        is Node.Comment -> {}
        is Node.Command -> {
            cmd.callCommand(this, cmd.name, cmd.content)
        }

        is Node.Control -> {
            val branches = (listOf(cmd.first) + cmd.branches).map { branch ->
                Command(
                    name = branch.first.name,
                    args = branch.first.content,
                    body = { ctx: Context.Builder<*> ->
                        for (x in branch.body) {
                            ctx.evalCommand(template, x)
                        }
                    }
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
            throw EvalException(expression, ex.message, ex)
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
                    get(subject, evalExpression(expression.right))
                } catch (ex: EvalException) {
                    throw ex
                } catch (ex: Exception) {
                    throw EvalException(expression, ex.message, ex)
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
                    get(subject, expression.name)
                } catch (ex: EvalException) {
                    throw ex
                } catch (ex: Exception) {
                    throw EvalException(expression, ex.message, ex)
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
            "undeclared function ${expression.name}"
        )
        val result = try {
            function.invoke(this, expression.args)
        } catch (ex: EvalException) {
            throw ex
        } catch (ex: Exception) {
            throw EvalException(expression, ex.message, ex)
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
                "undeclared binary function ${expression.decl.name}"
            )
        val result = try {
            function.invoke(this, expression.right)
        } catch (ex: EvalException) {
            throw ex
        } catch (ex: Exception) {
            throw EvalException(expression, ex.message, ex)
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
                val functionName = "invoke_${expression.left.name}"
                val function = getBinaryFunction(functionName)
                    ?: throw EvalException(
                        expression,
                        "undeclared binary function $functionName"
                    )
                val result = try {
                    function.invoke(this, subject, expression.args)
                } catch (ex: EvalException) {
                    throw ex
                } catch (ex: Exception) {
                    throw EvalException(expression.left, ex.message, ex)
                }
                if (result is Undefined) {
                    UndefinedResult(expression.left, result)
                } else {
                    result
                }
            }
            else -> when (val function = evalExpression(expression.left)) {
                is UnaryFunction -> {
                    val result = try {
                        function.invoke(this, expression.args)
                    } catch (ex: EvalException) {
                        throw ex
                    } catch (ex: Exception) {
                        throw EvalException(expression.left, ex.message, ex)
                    }
                    if (result is Undefined) {
                        UndefinedResult(expression.left, result)
                    } else {
                        result
                    }
                }
                else -> throw EvalException(
                    expression,
                    "cannot call $function as ${UnaryFunction::class}"
                )
            }
        }
    }
    is Expression.TransformOp -> {
        val functionName = "${expression.decl.name}_${expression.name}"
        val subject = evalExpressionInternal(expression.left)
        val (function, finalSubject) = if (subject is UndefinedResult) {
            val f = getRescueFunction(functionName) ?: throw EvalException(
                subject.expression,
                subject.value.message
            )
            f to subject.value
        } else {
            val f = getBinaryFunction(functionName) ?: throw EvalException(
                expression,
                "undeclared binary function $functionName"
            )
            f to subject
        }
        val result = try {
            function.invoke(this, finalSubject, expression.args)
        } catch (ex: EvalException) {
            throw ex
        } catch (ex: Exception) {
            throw EvalException(expression, ex.message, ex)
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
                "undeclared binary op function ${expression.decl.name}"
            )
        val result = try {
            function.invoke(this, subject, expression.right)
        } catch (ex: EvalException) {
            throw ex
        } catch (ex: Exception) {
            throw EvalException(expression, ex.message, ex)
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
    args: Expression.NamedArgs
) {
    val function = ctx.getCommand(name)
        ?: throw EvalException(this, "undeclared command $name")
    return try {
        function.invoke(ctx, args)
    } catch (ex: EvalException) {
        throw ex
    } catch (ex: Exception) {
        throw EvalException(this, ex.message, ex)
    }
}

private fun Node.callControl(
    ctx: Context.Builder<*>,
    name: String,
    branches: List<Command>
) {
    val function = ctx.getControl(name)
        ?: throw EvalException(this, "undeclared command $name")
    return try {
        function.invoke(ctx, branches)
    } catch (ex: EvalException) {
        throw ex
    } catch (ex: Exception) {
        throw EvalException(this, ex.message, ex)
    }
}
