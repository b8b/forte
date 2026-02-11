package org.cikit.forte.core

import org.cikit.forte.lib.core.UnaryNot
import org.cikit.forte.parser.Expression

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
        is Expression.ByteStringLiteral -> {
            operations.add(Operation.Const(expression, expression.value))
            Expression.ByteStringLiteral(
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
                operations.add(
                    Operation.InitArray(expression, expression.children.size)
                )
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
                operations.add(
                    Operation.Const(expression, emptyMap<String, Any?>())
                )
                expression.pairs
            } else {
                operations.add(
                    Operation.InitObject(expression, expression.pairs.size)
                )
                val pairs = ArrayList<Pair<Expression, Expression>>(
                    expression.pairs.size
                )
                for ((k, v) in expression.pairs) {
                    pairs += Pair(
                        compileExpressionInternal(operations, k),
                        compileExpressionInternal(operations, v)
                    )
                    operations.add(Operation.AddPairToObject(v))
                }
                pairs
            }
            operations.add(Operation.BuildObject(expression))
            Expression.ObjectLiteral(
                expression.first,
                expression.last,
                pairs,
                operations.opSubList(firstOperation, operations.size)
            )
        }
        is Expression.CompAccess -> {
            val left = compileExpressionInternal(operations, expression.left)
            val right = compileExpressionInternal(operations, expression.right)
            operations.add(Operation.ComputedGet(expression))
            Expression.CompAccess(
                expression.first,
                left,
                right,
                operations.opSubList(firstOperation, operations.size)
            )
        }
        is Expression.SliceAccess -> {
            val left = compileExpressionInternal(operations, expression.left)
            val compiledArgs = compileNamedArgs(
                operations,
                expression,
                expression.args
            )
            operations.add(
                Operation.SliceGet(
                    expression = expression,
                    argNames = expression.args.names
                )
            )
            Expression.SliceAccess(
                expression.first,
                left,
                compiledArgs,
                operations.opSubList(firstOperation, operations.size)
            )
        }
        is Expression.Access -> {
            val left = compileExpressionInternal(operations, expression.left)
            operations.add(Operation.ConstGet(expression, expression.name))
            Expression.Access(
                expression.first,
                expression.last,
                left,
                expression.name,
                operations.opSubList(firstOperation, operations.size)
            )
        }
        is Expression.FunctionCall -> {
            val compiledArgs = compileNamedArgs(
                operations,
                expression,
                expression.args,
            )
            operations.add(Operation.CallFunction(
                expression,
                Context.Key.Call(expression.name),
                expression.args.names)
            )
            Expression.FunctionCall(
                expression.first,
                expression.name,
                compiledArgs,
                operations.opSubList(firstOperation, operations.size)
            )
        }
        is Expression.UnOp -> {
            val right = compileExpressionInternal(operations, expression.right)
            operations.add(Operation.UnOp(
                expression,
                Context.Key.Unary(expression.decl.name))
            )
            Expression.UnOp(
                expression.tokens,
                expression.decl,
                expression.alias,
                right,
                operations.opSubList(firstOperation, operations.size)
            )
        }
        is Expression.InvokeOp -> {
            when (expression.left) {
                is Expression.Access -> {
                    val leftLeft = compileExpressionInternal(
                        operations,
                        expression.left.left
                    )
                    val methodKey = Context.Key.Apply.create(
                        expression.left.name,
                        Method.OPERATOR
                    )
                    val functionKey = Context.Key.Call(expression.left.name)
                    val compiledArgs = compileNamedArgs(
                        operations,
                        expression,
                        expression.args
                    )
                    operations.add(
                        Operation.CallNamedMethod(
                            expression.left,
                            methodKey,
                            functionKey,
                            expression.args.names
                        )
                    )
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
                        compiledArgs,
                        operations.opSubList(firstOperation, operations.size)
                    )
                }
                is Expression.CompAccess -> {
                    //subject
                    val leftLeft = compileExpressionInternal(
                        operations,
                        expression.left.left
                    )
                    //method name
                    val leftRight = compileExpressionInternal(
                        operations,
                        expression.left.right
                    )
                    val compiledLeft = Expression.CompAccess(
                        expression.left.first,
                        leftLeft,
                        leftRight,
                        operations.opSubList(firstOperation, operations.size)
                    )
                    //args
                    val compiledArgs = compileNamedArgs(
                        operations,
                        expression,
                        expression.args
                    )
                    operations.add(
                        Operation.CallComputedMethod(
                            expression,
                            expression.args.names
                        )
                    )
                    Expression.InvokeOp(
                        expression.first,
                        expression.last,
                        compiledLeft,
                        compiledArgs,
                        operations.opSubList(firstOperation, operations.size)
                    )
                }

                else -> throw EvalException(
                    expression,
                    "illegal invoke() operation on ${expression.left}"
                )
            }
        }
        is Expression.TransformOp -> {
            val method = expression.name
            val operator = expression.decl.negate?.name ?: expression.decl.name
            val left = compileExpressionInternal(operations, expression.left)
            val compiledArgs = compileNamedArgs(
                operations,
                expression,
                expression.args
            )
            operations.add(
                Operation.TransformOp(
                    expression,
                    Context.Key.Apply<Method>(method, operator),
                    expression.args.names
                )
            )
            if (expression.decl.negate != null) {
                operations.add(
                    Operation.UnOp(
                        expression,
                        UnaryNot.KEY,
                        allowHidden = true
                    )
                )
            }
            Expression.TransformOp(
                expression.tokens,
                expression.decl,
                expression.alias,
                left,
                expression.name,
                compiledArgs,
                operations.opSubList(firstOperation, operations.size)
            )
        }
        is Expression.BinOp -> {
            val name = expression.decl.negate?.name ?: expression.decl.name

            val leftExpr: Expression
            val rightExpr: Expression
            if (expression.decl.reverse) {
                leftExpr = expression.right
                rightExpr = expression.left
            } else {
                leftExpr = expression.left
                rightExpr = expression.right
            }

            val left = compileExpressionInternal(operations, leftExpr)

            val index1 = operations.size
            operations.add(
                Operation.CondBinOp(expression, Context.Key.Binary(name), 0)
            )

            val index2 = operations.size
            val right = compileExpressionInternal(operations, rightExpr)
            operations.add(Operation.InvokeBinOp(expression, name))

            val condOperationsCount = operations.size - index2
            operations[index1] = Operation.CondBinOp(
                expression,
                Context.Key.Binary(name),
                condOperationsCount
            )

            if (expression.decl.negate != null) {
                operations.add(
                    Operation.UnOp(
                        expression,
                        UnaryNot.KEY,
                        allowHidden = true
                    )
                )
            }

            Expression.BinOp(
                expression.tokens,
                expression.decl,
                expression.alias,
                left,
                right,
                operations.opSubList(firstOperation, operations.size)
            )
        }
    }
}

private fun compileNamedArgs(
    operations: MutableList<Operation>,
    expression: Expression,
    args: Expression.NamedArgs
): Expression.NamedArgs {
    operations.add(
        Operation.InitArray(expression, args.values.size)
    )
    val compiledArgs = buildList(capacity = args.values.size) {
        var index = 0
        for (arg in args.values) {
            this += compileExpressionInternal(operations, arg)
            operations.add(Operation.AddArrayElement(arg, index++))
        }
    }
    return Expression.NamedArgs(args.names, compiledArgs)
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
