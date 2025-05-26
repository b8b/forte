package org.cikit.forte.core

import org.cikit.forte.eval.tryEvalExpression
import org.cikit.forte.parser.Expression
import org.cikit.forte.parser.Node
import org.cikit.forte.parser.ParsedTemplate

class Branch(
    val name: String,
    val args: Map<String, Expression>,
    val body: ParsedTemplate
)

@Deprecated("migrate to suspending api")
fun interface CommandFunction {
    operator fun invoke(ctx: Context.Builder<*>, args: Map<String, Expression>)
}

fun interface CommandTag {
    suspend operator fun invoke(ctx: Context.Builder<*>, args: Map<String, Expression>)
}

@Deprecated("migrate to suspending api")
fun interface ControlFunction {
    operator fun invoke(ctx: Context.Builder<*>, branches: List<Branch>)
}

fun interface ControlTag {
    suspend operator fun invoke(ctx: Context.Builder<*>, branches: List<Branch>)
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

class Suspended(val eval: suspend () -> Any?) : Undefined(
    "evaluation has been suspended"
)

open class Undefined(open val message: String) {
    override fun toString(): String {
        return "Undefined(message='$message')"
    }
}

@Deprecated("migrate to suspending api")
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

@Deprecated("migrate to suspending api")
fun Context<*>.evalExpression(expression: Expression): Any? {
    return tryEvalExpression(expression).getOrThrow()
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
