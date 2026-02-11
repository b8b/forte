package org.cikit.forte.lib.core

import org.cikit.forte.core.BinOpFunction
import org.cikit.forte.core.Context
import org.cikit.forte.core.DependencyAware
import org.cikit.forte.core.FilterMethod
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.StringConcatenation
import org.cikit.forte.core.Suspended
import org.cikit.forte.core.Undefined

class BinaryConcat private constructor(
    private val filterString: FilterMethod
): BinOpFunction, DependencyAware {

    constructor(ctx: Context<*>) : this(ctx.filterString)

    override fun withDependencies(ctx: Context<*>): DependencyAware {
        val filterString = ctx.filterString
        return if (filterString === this.filterString) {
            this
        } else {
            BinaryConcat(filterString)
        }
    }

    override fun invoke(left: Any?, right: Any?): Any {
        val leftStr = when (left) {
            is CharSequence -> left
            else -> {
                val result = filterString(left, NamedArgs.Empty)
                if (result is Suspended) {
                    return concatSuspended(result, right)
                }
                result as CharSequence
            }
        }
        val rightStr = when (right) {
            is CharSequence -> right
            else -> {
                val result = filterString(right, NamedArgs.Empty)
                if (result is Suspended) {
                    return concatSuspended(leftStr, result)
                }
                result as CharSequence
            }
        }
        return StringConcatenation.concat(leftStr, rightStr)
    }

    private fun concatSuspended(
        left: Suspended,
        right: Any?
    ) = Suspended { ctx ->
        val leftStr = left.eval(ctx)
        if (leftStr is Undefined) {
            return@Suspended leftStr
        }
        leftStr as CharSequence
        val rightStr = when (right) {
            is CharSequence -> right
            else -> {
                var result = filterString(right, NamedArgs.Empty)
                if (result is Suspended) {
                    result = result.eval(ctx)
                    if (result is Undefined) {
                        return@Suspended result
                    }
                }
                result as CharSequence
            }
        }
        StringConcatenation.concat(leftStr, rightStr)
    }

    private fun concatSuspended(
        leftStr: CharSequence,
        right: Suspended
    ) = Suspended { ctx ->
        val rightStr = right.eval(ctx)
        if (rightStr is Undefined) {
            return@Suspended rightStr
        }
        rightStr as CharSequence
        StringConcatenation.concat(leftStr, rightStr)
    }
}
