package org.cikit.forte.eval

import org.cikit.forte.core.Branch
import org.cikit.forte.core.Context
import org.cikit.forte.core.EvalException
import org.cikit.forte.core.Undefined
import org.cikit.forte.parser.Expression
import org.cikit.forte.parser.Node
import org.cikit.forte.parser.ParsedTemplate

internal val UNCOMPILED_NODE: suspend (Context.Builder<*>) -> Unit = {}

internal val UNCOMPILED_EXPRESSION = listOf(
    Operation.Const(
        Expression.Malformed(emptyList()),
        Undefined("uncompiled expression")
    )
)

fun ParsedTemplate.compile(): ParsedTemplate {
    val template = this
    val compiledNodes = nodes.map { it.compile(template) }
    return ParsedTemplate(input, path, compiledNodes)
}

private fun Map<String, Expression>.compile() = buildMap {
    for ((k, v) in this@compile) {
        put(k, v.compile())
    }
}

internal fun Node.compile(template: ParsedTemplate) = when (val cmd = this) {
    is Node.Comment -> cmd
    is Node.Command -> cmd.compile()
    is Node.Control -> cmd.compile(template)
    is Node.Text -> cmd.compile(template)
    is Node.Emit -> cmd.compile()
}

private fun Node.Command.compile(): Node.Command {
    val content = content.compile()
    return Node.Command(first, name, content, last) { ctx ->
        val function = ctx.getCommandTag(name)
            ?: throw EvalException(this, "undefined command $name")
        try {
            function.invoke(ctx, content)
        } catch (ex: EvalException) {
            throw ex
        } catch (ex: Exception) {
            throw EvalException(this, ex.toString(), ex)
        }
    }
}

private fun Node.Control.compile(template: ParsedTemplate): Node.Control {
    val name = first.first.name
    val compiledBranches = buildList {
        this += first.compile(template)
        for (branch in branches) {
            this += branch.compile(template)
        }
    }
    return Node.Control(first, branches) { ctx ->
        val function = ctx.getControlTag(name)
            ?: throw EvalException(this, "undefined command $name")
        try {
            function.invoke(ctx, compiledBranches)
        } catch (ex: EvalException) {
            throw ex
        } catch (ex: Exception) {
            throw EvalException(this, ex.toString(), ex)
        }
    }
}

private fun Node.Branch.compile(template: ParsedTemplate): Branch {
    val bodyCompiled = body.map { it.compile(template) }
    return Branch(
        name = first.name,
        args = first.content.compile(),
        body = ParsedTemplate(
            input = template.input,
            path = template.path,
            nodes = bodyCompiled
        )
    )
}

private fun Node.Text.compile(template: ParsedTemplate): Node.Text {
    val text = template.input.substring(
        content.first..content.last
    )
    val trimmed = when {
        trimLeft && trimRight -> text.trim()
        trimLeft -> text.trimStart()
        trimRight -> text.trimEnd()
        else -> text
    }
    return Node.Text(
        content,
        trimLeft,
        trimRight,
        if (trimmed.isNotEmpty()) {
            { ctx -> ctx.emit(trimmed) }
        } else {
            null
        }
    )
}

private fun Node.Emit.compile(): Node.Emit {
    val content = content.compile()
    return Node.Emit(first, content, last) { ctx ->
        val result = ctx.evalExpression(content)
        ctx.emit(result)
    }
}

fun Expression.compile(): Expression {
    val operations = ArrayList<Operation>()
    return compileExpressionInternal(operations, this)
}

private fun compileExpressionInternal(
    operations: MutableList<Operation>,
    expression: Expression,
): Expression {
    val firstOperation = operations.size
    return when (expression) {
        is Expression.Malformed -> throw EvalException(
            expression,
            "malformed expression: $expression"
        )
        is Expression.SubExpression -> {
            Expression.SubExpression(
                compileExpressionInternal(operations, expression.content),
                operations.opSubList(firstOperation, operations.size)
            )
        }
        is Expression.Variable -> {
            operations.add(Operation.GetVar(expression))
            Expression.Variable(
                expression.first,
                expression.name,
                operations.opSubList(firstOperation, operations.size)
            )
        }
        is Expression.NullLiteral -> {
            operations.add(Operation.Const(expression, null))
            Expression.NullLiteral(
                expression.token,
                operations.opSubList(firstOperation, operations.size)
            )
        }
        is Expression.BooleanLiteral -> {
            operations.add(Operation.Const(expression, expression.value))
            Expression.BooleanLiteral(
                expression.token,
                expression.value,
                operations.opSubList(firstOperation, operations.size)
            )
        }
        is Expression.NumericLiteral -> {
            operations.add(Operation.Const(expression, expression.value))
            Expression.NumericLiteral(
                expression.first,
                expression.last,
                expression.value,
                operations.opSubList(firstOperation, operations.size)
            )
        }
        is Expression.StringLiteral -> {
            operations.add(Operation.Const(expression, expression.value))
            Expression.StringLiteral(
                expression.first,
                expression.last,
                expression.value,
                operations.opSubList(firstOperation, operations.size)
            )
        }
        is Expression.StringInterpolation -> {
            val children = if (expression.children.isEmpty()) {
                operations.add(Operation.Const(expression, ""))
                expression.children
            } else {
                operations.add(Operation.InitString(expression))
                val children = ArrayList<Expression>(expression.children.size)
                for (child in expression.children) {
                    children += compileExpressionInternal(operations, child)
                    operations.add(Operation.ConcatToString(child))
                }
                operations.add(Operation.BuildString(expression))
                children.toList()
            }
            Expression.StringInterpolation(
                children,
                operations.opSubList(firstOperation, operations.size)
            )
        }
        is Expression.ArrayLiteral -> {
            val children = if (expression.children.isEmpty()) {
                operations.add(Operation.Const(expression, emptyList<Any?>()))
                expression.children
            } else {
                operations.add(Operation.InitArray(expression, expression.children.size))
                val children = ArrayList<Expression>(expression.children.size)
                var index = 0
                for (child in expression.children) {
                    children += compileExpressionInternal(operations, child)
                    operations.add(Operation.AddArrayElement(child, index++))
                }
                operations.add(Operation.BuildArray(expression))
                children.toList()
            }
            Expression.ArrayLiteral(
                expression.first,
                expression.last,
                children,
                operations.opSubList(firstOperation, operations.size)
            )
        }
        is Expression.ObjectLiteral -> {
            val pairs = if (expression.pairs.isEmpty()) {
                operations.add(Operation.Const(expression, emptyMap<String, Any?>()))
                expression.pairs
            } else {
                operations.add(Operation.InitObject(expression, expression.pairs.size))
                val pairs = ArrayList<Pair<Expression, Expression>>(expression.pairs.size)
                for ((k, v) in expression.pairs) {
                    pairs += Pair(
                        compileExpressionInternal(operations, k),
                        compileExpressionInternal(operations, v)
                    )
                    operations.add(Operation.AddPairToObject(v))
                }
                operations.add(Operation.BuildObject(expression))
                pairs
            }
            Expression.ObjectLiteral(
                expression.first,
                expression.last,
                pairs,
                operations.opSubList(firstOperation, operations.size)
            )
        }
        is Expression.CompAccess -> {
            val left = compileExpressionInternal(operations, expression.left)
            operations.add(Operation.InitArray(expression, 1))
            val right = compileExpressionInternal(operations, expression.right)
            operations.add(Operation.AddArrayElement(expression.right, 0))
            operations.add(Operation.TransformOp(expression, "get", "pipe", listOf("key")))
            Expression.CompAccess(
                expression.first,
                left,
                right,
                operations.opSubList(firstOperation, operations.size)
            )
        }
        is Expression.Access -> {
            val left = compileExpressionInternal(operations, expression.left)
            operations.add(Operation.InitArray(expression, 1))
            operations.add(Operation.Const(expression, expression.name))
            operations.add(Operation.AddArrayElement(expression, 0))
            operations.add(Operation.TransformOp(expression, "get", "pipe", listOf("key")))
            Expression.Access(
                expression.first,
                expression.last,
                left,
                expression.name,
                operations.opSubList(firstOperation, operations.size)
            )
        }
        is Expression.FunctionCall -> {
            operations.add(Operation.InitArray(expression, expression.args.values.size))
            val args = ArrayList<Expression>(expression.args.values.size)
            var index = 0
            for (arg in expression.args.values) {
                args += compileExpressionInternal(operations, arg)
                operations.add(Operation.AddArrayElement(arg, index++))
            }
            operations.add(Operation.CallFunction(expression, expression.args.names))
            Expression.FunctionCall(
                expression.first,
                expression.name,
                Expression.NamedArgs(expression.args.names, args),
                operations.opSubList(firstOperation, operations.size)
            )
        }
        is Expression.UnOp -> {
            val right = compileExpressionInternal(operations, expression.right)
            operations.add(Operation.UnOp(expression))
            Expression.UnOp(
                expression.tokens,
                expression.decl,
                right,
                operations.opSubList(firstOperation, operations.size)
            )
        }
        is Expression.InvokeOp -> {
            when (expression.left) {
                is Expression.Access -> {
                    val leftLeft = compileExpressionInternal(operations, expression.left.left)
                    operations.add(Operation.InitArray(expression, expression.args.values.size))
                    val args = ArrayList<Expression>(expression.args.values.size)
                    var index = 0
                    for (arg in expression.args.values) {
                        args += compileExpressionInternal(operations, arg)
                        operations.add(Operation.AddArrayElement(arg, index++))
                    }
                    operations.add(Operation.CallMethod(expression, expression.left.name, expression.args.names))
                    Expression.InvokeOp(
                        expression.first,
                        expression.last,
                        Expression.Access(
                            expression.left.first,
                            expression.left.last,
                            leftLeft,
                            expression.left.name,
                            emptyList()
                        ),
                        Expression.NamedArgs(expression.args.names, args),
                        operations.opSubList(firstOperation, operations.size)
                    )
                }
                else -> {
                    val left = compileExpressionInternal(operations, expression.left)
                    operations.add(Operation.InitArray(expression, expression.args.values.size))
                    val args = ArrayList<Expression>(expression.args.values.size)
                    var index = 0
                    for (arg in expression.args.values) {
                        args += compileExpressionInternal(operations, arg)
                        operations.add(Operation.AddArrayElement(arg, index++))
                    }
                    operations.add(Operation.CallMethod(expression, "invoke", expression.args.names))
                    Expression.InvokeOp(
                        expression.first,
                        expression.last,
                        left,
                        Expression.NamedArgs(expression.args.names, args),
                        operations.opSubList(firstOperation, operations.size)
                    )
                }
            }
        }
        is Expression.TransformOp -> {
            val method = expression.name
            val operator = expression.decl.name
            val left = compileExpressionInternal(operations, expression.left)
            operations.add(Operation.InitArray(expression, expression.args.values.size))
            val args = ArrayList<Expression>(expression.args.values.size)
            var index = 0
            for (arg in expression.args.values) {
                args += compileExpressionInternal(operations, arg)
                operations.add(Operation.AddArrayElement(arg, index++))
            }
            operations.add(Operation.TransformOp(expression, method, operator, expression.args.names))
            Expression.TransformOp(
                expression.tokens,
                expression.decl,
                left,
                expression.name,
                Expression.NamedArgs(expression.args.names, args),
                operations.opSubList(firstOperation, operations.size)
            )
        }
        is Expression.BinOp -> {
            val left = compileExpressionInternal(operations, expression.left)

            val index1 = operations.size
            operations.add(Operation.CondBinOp(expression, expression.decl.name, 0))

            val index2 = operations.size
            val right = compileExpressionInternal(operations, expression.right)
            operations.add(Operation.InvokeBinOp(expression, expression.decl.name))

            val condOperationsCount = operations.size - index2
            operations[index1] = Operation.CondBinOp(expression, expression.decl.name, condOperationsCount)

            Expression.BinOp(
                expression.tokens,
                expression.decl,
                left,
                right,
                operations.opSubList(firstOperation, operations.size)
            )
        }
    }
}

private fun List<Operation>.opSubList(start: Int, endExclusive: Int) =
    OperationSubList(this, start, endExclusive)

private class OperationSubList(
    val operations: List<Operation>,
    val start: Int,
    val endExclusive: Int
) : List<Operation> {

    override val size: Int
        get() = endExclusive - start

    override fun isEmpty(): Boolean {
        return size <= 0
    }

    override fun contains(element: Operation): Boolean {
        for (i in start until endExclusive) {
            if (operations[i] ==  element) {
                return true
            }
        }
        return false
    }

    override fun iterator(): Iterator<Operation> =
        object : Iterator<Operation> {
            private var index = 0

            override fun next(): Operation {
                val internalIndex = start + index
                if (internalIndex >= endExclusive) {
                    throw NoSuchElementException()
                }
                index++
                return operations[internalIndex]
            }

            override fun hasNext(): Boolean {
                return start + index < endExclusive
            }
        }

    override fun containsAll(elements: Collection<Operation>): Boolean {
        return elements.all { contains(it) }
    }

    override fun get(index: Int): Operation {
        val internalIndex = start + index
        if (internalIndex !in start until endExclusive) {
            throw IndexOutOfBoundsException()
        }
        return operations[internalIndex]
    }

    override fun indexOf(element: Operation): Int {
        for (i in start until endExclusive) {
            if (operations[i] ==  element) {
                return i
            }
        }
        return -1
    }

    override fun lastIndexOf(element: Operation): Int {
        for (i in (start until endExclusive).reversed()) {
            if (operations[i] ==  element) {
                return i
            }
        }
        return -1
    }

    override fun listIterator(): ListIterator<Operation> =
        buildList {
            for (i in start until endExclusive) {
                add(operations[i])
            }
        }.listIterator()

    override fun listIterator(index: Int): ListIterator<Operation> =
        buildList {
            for (i in start until endExclusive) {
                add(operations[i])
            }
        }.listIterator(index)

    override fun subList(
        fromIndex: Int,
        toIndex: Int
    ): List<Operation> {
        val internalFromIndex = start + fromIndex
        if (internalFromIndex !in start until endExclusive) {
            throw IndexOutOfBoundsException()
        }
        val internalToIndex = start + toIndex
        if (internalToIndex !in start until endExclusive) {
            throw IndexOutOfBoundsException()
        }
        return OperationSubList(operations, internalFromIndex, internalToIndex)
    }
}
