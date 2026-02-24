package org.cikit.forte.lib.core

import org.cikit.forte.core.BinOpFunction
import org.cikit.forte.core.Context
import org.cikit.forte.core.DependencyAware
import org.cikit.forte.core.StringConcatenation

class BinaryPlus private constructor(
    private val number: FilterNumber
): BinOpFunction, DependencyAware {

    constructor(ctx: Context<*>) : this(ctx.filterNumber)

    override fun withDependencies(ctx: Context<*>): DependencyAware {
        val number = ctx.filterNumber
        return if (number === this.number) {
            this
        } else {
            BinaryPlus(number)
        }
    }

    override fun invoke(left: Any?, right: Any?): Any? {
        val leftNumber = try {
            number.requireNumber(left)
        } catch (ex: Exception) {
            return when (left) {
                is CharSequence -> {
                    if (right !is CharSequence) {
                        binOpTypeError("plus", left, right)
                    }
                    StringConcatenation.concat(left, right)
                }
                is List<*> -> {
                    if (right !is List<*>) {
                        binOpTypeError("plus", left, right)
                    }
                    left + right
                }

                else -> throw ex
            }
        }
        return leftNumber.plus(number.requireNumber(right))
    }
}
