package org.cikit.forte.lib.core

import org.cikit.forte.core.BinOpFunction
import org.cikit.forte.core.Context
import org.cikit.forte.core.DependencyAware
import org.cikit.forte.core.StringConcatenation

class BinaryMul private constructor(
    private val number: FilterNumber
): BinOpFunction, DependencyAware {

    constructor(ctx: Context<*>) : this(ctx.filterNumber)

    override fun withDependencies(ctx: Context<*>): DependencyAware {
        val number = ctx.filterNumber
        return if (number === this.number) {
            this
        } else {
            BinaryMul(number)
        }
    }

    override fun invoke(left: Any?, right: Any?): Any? {
        val leftNumber = try {
            number(left)
        } catch (ex: Exception) {
            return when (left) {
                is CharSequence -> {
                    val right = number(right).toIntOrNull()
                        ?: binOpTypeError("mul", left, right)
                    StringConcatenation.replicate(left, right)
                }

                else -> throw ex
            }
        }
        return leftNumber.mul(number(right)).result
    }
}
