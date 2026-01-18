package org.cikit.forte.lib.core

import org.cikit.forte.core.Context
import org.cikit.forte.core.DependencyAware
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.TestMethod

class IsInTest private constructor(
    private val comparable: FilterComparable,
) : TestMethod, DependencyAware {

    companion object {
        val KEY: Context.Key.Apply<IsInTest> =
            Context.Key.Apply.create("in", TestMethod.OPERATOR)
    }

    constructor(ctx: Context<*>) : this(ctx.filterComparable)

    override val isHidden: Boolean
        get() = false

    override fun withDependencies(ctx: Context<*>): IsInTest {
        val comparable = ctx.filterComparable
        return if (comparable === this.comparable) {
            this
        } else {
            IsInTest(comparable)
        }
    }

    override fun invoke(subject: Any?, args: NamedArgs): Boolean {
        val listValue: Any?
        args.use {
            listValue = requireAny("listValue")
        }
        return test(subject, listValue)
    }

    fun test(left: Any?, right: Any?): Boolean {
        val collection = when (right) {
            is CharSequence -> when (left) {
                is CharSequence -> return left.isEmpty() || right.contains(left)
                is Char -> return right.contains(left)
                else -> binOpTypeError("in", left, right)
            }
            is Char -> when (left) {
                is CharSequence -> return left.isEmpty() ||
                        (left.length == 1 && left[0] == right)
                is Char -> return left == right
                else -> binOpTypeError("in", left, right)
            }
            is Map<*, *> -> right.keys
            is Iterable<*> -> right
            else -> binOpTypeError("in", left, right)
        }
        val comparableLeft = comparable.test(left)
            ?: return left in collection
        return collection.any { item ->
            comparable.test(item)?.compareTo(comparableLeft) == 0
        }
    }
}
