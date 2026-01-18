package org.cikit.forte.core

import org.cikit.forte.parser.Expression

interface EvaluatorState {
    fun isEmpty(): Boolean

    fun jump(relOffset: Int)
    fun addLast(expression: Expression, value: Any?)
    fun setLast(expression: Expression, value: Any?)
    fun removeLast(): Any?
    fun last(): Any?
    fun rescueLast(): Any?
}
