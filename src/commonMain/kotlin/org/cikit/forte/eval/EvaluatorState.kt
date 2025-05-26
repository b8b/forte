package org.cikit.forte.eval

class EvaluatorState(
    private val operations: List<Operation>,
    private val stack: ArrayDeque<Any?> = ArrayDeque()
) {
    private var ip = 0

    fun next(): Operation? {
        if (ip < operations.size) {
            return operations[ip++]
        }
        return null
    }

    fun jump(relOffset: Int) {
        require(relOffset > 0) {
            "invalid non positive jump: $relOffset"
        }
        require(ip + relOffset <= operations.size) {
            "invalid jump: $relOffset"
        }
        ip += relOffset
    }

    fun addLast(value: Any?) {
        stack.addLast(value)
    }

    fun removeLast(): Any? {
        return stack.removeLast()
    }

    fun setLast(value: Any?) {
        stack[stack.size - 1] = value
    }

    fun last(): Any? {
        return stack.last()
    }

    fun lastOrNull(): Any? {
        return stack.lastOrNull()
    }
}
