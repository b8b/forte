package org.cikit.forte

class Glob(val pattern: String) {

    fun toRegex() = convertPattern().toRegex()
    fun toRegex(option: RegexOption) = convertPattern().toRegex(option)
    fun toRegex(options: Set<RegexOption>) = convertPattern().toRegex(options)

    private fun convertPattern(): String {
        // If we are doing extended matching,
        // this boolean is true when we are inside a group
        // (eg {*.html,*.js}), and false otherwise.
        var inGroup = 0

        // target to hold converted pattern
        val target = StringBuilder()

        for (ch in pattern) {
            when (ch) {
                '/', '$', '^', '+', '.', '(', ')', '=', '!', '|' -> {
                    target.append("\\")
                    target.append(ch)
                }
                '?' -> target.append(".")
                '[', ']' -> target.append(ch)
                '{' -> {
                    target.append('(')
                    inGroup++
                }
                '}' -> {
                    target.append(')')
                    inGroup--
                }
                ',' -> if (inGroup > 0) {
                    target.append('|')
                } else {
                    target.append('\\')
                    target.append(ch)
                }
                '*' -> {
                    target.append(".*")
                }
                else -> target.append(ch)
            }
        }
        return target.toString()
    }

}
