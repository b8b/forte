package org.cikit.forte

import okio.ByteString
import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.toByteString
import kotlin.math.roundToInt

private fun eq(subject: Any?, other: Any?) = subject == other

private fun `in`(subject: Any?, listValue: Any?) = when (listValue) {
    null -> null
    is String -> listValue.indexOf(subject as String) >= 0
    is List<*> -> subject in listValue
    else -> error("invalid type for in: $listValue")
}

private fun range(subject: Any, other: Any): List<Any?> = when (subject) {
    is Int -> (subject..(other as Int)).toList()
    else -> error("invalid type for range: $subject")
}

private fun plus(subject: Any, other: Any?) = when (subject) {
    is Int -> subject + (other as Int)
    is Float -> subject + (other as Float)
    is Double -> subject + (other as Double)
    is String -> subject + (other as String)
    else -> error("invalid type for plus: $subject")
}

private fun minus(subject: Any, other: Any) = when (subject) {
    is Int -> subject - (other as Int)
    is Float -> subject - (other as Float)
    is Double -> subject - (other as Double)
    else -> error("invalid type for minus: $subject")
}

private fun toInt(subject: Any) = when (subject) {
    is Boolean -> if (subject) 1 else 0
    is Int -> subject
    is Long -> subject.toInt()
    is Float -> subject.roundToInt()
    is Double -> subject.roundToInt()
    is String -> subject.toInt()
    else -> error("cannot convert to int: $subject")
}

private fun toString(subject: Any?) = when (subject) {
    null -> "null"
    is Boolean -> if (subject) "true" else "false"
    is Int -> subject.toString()
    is Float -> subject.toString()
    is Double -> subject.toString()
    is String -> subject
    else -> error("cannot convert to string: $subject")
}

private fun toJson(subject: Any?): String = JsonEmitter.encodeToString {
    emit(subject)
}

private fun toYaml(subject: Any?): String = YamlEmitter.encodeToString {
    emit(subject)
}

private fun isString(subject: Any?): Boolean = subject is String

private fun isIterable(subject: Any?): Boolean =
    subject is List<*> || subject is String

fun Runtime.setCoreExtensions() {

    setFunction("cmd_set") { args ->
        val (varName, value) = readArgs(args, "varName", "value")
        varName as String
        setVar(varName, value)
    }

    setFunction("control_if") { args ->
        val (branches) = readArgs(args, "branches")
        for (cmd in branches as List<*>) {
            cmd as Runtime.Command
            when (cmd.name) {
                "else" -> {
                    cmd.body()
                    break
                }
                else -> {
                    val (condition) = readArgs(cmd.args(), "condition")
                    if (condition == true) {
                        cmd.body()
                        break
                    }
                }
            }
        }
    }

    setFunction("control_for") { args ->
        val (branches) = readArgs(args, "branches")
        for (cmd in branches as List<*>) {
            cmd as Runtime.Command
            when (cmd.name) {
                "else" -> {
                    cmd.body()
                    break
                }
                else -> {
                    val (varNames, listValue, recursive, condition) = readArgs(
                        cmd.args(),
                        "varNames", "listValue",
                        "recursive?", "condition?"
                    )
                    varNames as List<*>
                    listValue as List<*>
                    val varName = varNames.singleOrNull() as? String
                        ?: error("destructuring in for loop is not implemented")
                    if (recursive != null) {
                        error("recursive for loop is not implemented")
                    }
                    if (condition != null) {
                        error("conditional for loop is not implemented")
                    }
                    var done = false
                    for (item in listValue) {
                        done = true
                        enterScope()
                        try {
                            setVar(varName, item)
                            cmd.body()
                        } finally {
                            exitScope()
                        }
                    }
                    if (done) {
                        break
                    }
                }
            }
        }
    }

    setFunction("control_macro") { args ->
        val (branches) = readArgs(args, "branches")
        branches as List<*>
        val cmd = branches.single() as Runtime.Command
        val (functionName, functionArgs) = readArgs(cmd.args(), "name", "args")
        functionName as String
        functionArgs as Map<*, *>

        val callName = "call_$functionName"
        val argNames = functionArgs.map { (k, _) -> k.toString() }
        val argDefaults = functionArgs.map { (_, v) -> v }

        setFunction(callName) { macroArgs ->
            val argValues = readArgs(macroArgs, *argNames.toTypedArray())
            val target = StringBuilder()
            enterScope()
            try {
                startCapture { v -> target.append(v.toString()) }
                try {
                    argNames.forEachIndexed { index, name ->
                        setVar(name, argValues[index] ?: argDefaults[index])
                    }
                    cmd.body()
                } finally {
                    endCapture()
                }
            } finally {
                exitScope()
            }
            target.toString()
        }
    }

    setExtension("apply_eq") { subject, args ->
        val (other) = readArgs(args(), "other")
        eq(subject, other)
    }

    setExtension("apply_ne") { subject, args ->
        val (other) = readArgs(args(), "other")
        !eq(subject, other)
    }

    setExtension("apply_gt") { subject, args ->
        val (other) = readArgs(args(), "other")
        when (subject) {
            is String -> subject > (other as String)
            is Float -> subject > (other as Float)
            is Double -> subject > (other as Double)
            is Int -> subject > (other as Int)
            else -> error("invalid type for gt: $subject")
        }
    }

    setExtension("apply_ge") { subject, args ->
        val (other) = readArgs(args(), "other")
        when (subject) {
            is String -> subject >= (other as String)
            is Float -> subject >= (other as Float)
            is Double -> subject >= (other as Double)
            is Int -> subject >= (other as Int)
            else -> error("invalid type for ge: $subject")
        }
    }

    setExtension("apply_lt") { subject, args ->
        val (other) = readArgs(args(), "other")
        when (subject) {
            is String -> subject < (other as String)
            is Float -> subject < (other as Float)
            is Double -> subject < (other as Double)
            is Int -> subject < (other as Int)
            else -> error("invalid type for lt: $subject")
        }
    }

    setExtension("apply_le") { subject, args ->
        val (other) = readArgs(args(), "other")
        when (subject) {
            is String -> subject <= (other as String)
            is Float -> subject <= (other as Float)
            is Double -> subject <= (other as Double)
            is Int -> subject <= (other as Int)
            else -> error("invalid type for le: $subject")
        }
    }

    setExtension("apply_or") { subject, args ->
        if (subject == true) return@setExtension true
        val (other) = readArgs(args(), "other")
        (other as Boolean?) == true
    }

    setExtension("apply_and") { subject, args ->
        if (subject != true) return@setExtension false
        val (other) = readArgs(args(), "other")
        (other as Boolean?) == true
    }

    setFunction("call_not") { args ->
        val (other) = readArgs(args, "other")
        (other as Boolean?) == false
    }

    setExtension("apply_range") { subject, args ->
        if (subject == null) return@setExtension null
        val (end) = readArgs(args(), "end_inclusive")
        range(subject, end!!)
    }

    setFunction("call_range") { args ->
        val (start, end) = readArgs(args, "start", "end_inclusive")
        range(start!!, end!!)
    }

    setExtension("apply_plus") { subject, args ->
        if (subject == null) return@setExtension null
        val (other) = readArgs(args(), "other")
        plus(subject, other)
    }

    setExtension("apply_minus") { subject, args ->
        if (subject == null) return@setExtension null
        val (other) = readArgs(args(), "other")
        minus(subject, other!!)
    }

    setExtension("apply_int") { subject, _ ->
        if (subject == null) return@setExtension null
        toInt(subject)
    }

    setExtension("apply_float") { subject, _ ->
        when (subject) {
            null -> null
            is Boolean-> if (subject) 1.0 else 0.0
            is Int -> subject.toDouble()
            is Float -> subject
            is String -> subject.toDouble()
            else -> error("cannot convert to float: $subject")
        }
    }

    setExtension("apply_string") { subject, _ -> toString(subject) }

    setExtension("apply_list") { subject, args ->
        if (subject == null) return@setExtension emptyList<Any?>()
        val (strings) = readArgs(args(), "strings?")
        strings as String?
        when (subject) {
            is List<*> -> subject
            is String -> when (strings ?: "codePoints") {
                "empty" -> emptyList()
                "chars" -> subject.map { it.toString() }
                "codePoints" -> {
                    val result = mutableListOf<String>()
                    val charIt = subject.iterator()
                    while (charIt.hasNext()) {
                        val ch1 = charIt.next()
                        result += if (ch1.isHighSurrogate()) {
                            val ch2 = charIt.next()
                            charArrayOf(ch1, ch2).concatToString()
                        } else {
                            ch1.toString()
                        }
                    }
                    result.toList()
                }

                else -> error("invalid option for strings: $strings")
            }
            else -> error("cannot convert to list: $subject")
        }
    }

    setExtension("apply_default") { subject, args ->
        when (subject) {
            null -> {
                val (defaultValue) = readArgs(args(), "default_value")
                defaultValue
            }
            else -> subject
        }
    }

    setExtension("apply_first") { subject, _ ->
        if (subject == null) return@setExtension null
        when (subject) {
            is List<*> -> subject.firstOrNull()
            else -> error("invalid type for first: $subject")
        }
    }

    setExtension("apply_last") { subject, _ ->
        if (subject == null) return@setExtension null
        when (subject) {
            is List<*> -> subject.lastOrNull()
            else -> error("invalid type for last: $subject")
        }
    }

    setExtension("apply_keys") { subject, _ ->
        when (subject) {
            is Map<*, *> -> subject.keys.toList()
            is List<*> -> listOf("size") + (0 until subject.size).toList()
            else -> emptyList()
        }
    }

    setExtension("apply_length") { subject, _ ->
        if (subject == null) return@setExtension null
        when (subject) {
            is List<*> -> subject.size
            is String -> subject.length
            else -> error("invalid type for length: $subject")
        }
    }

    setExtension("apply_reject") { subject, args ->
        if (subject == null) return@setExtension null
        val (test, arg) = readArgs(args(), "test", "arg?")
        test as String
        val filter: (Any?) -> Boolean = when (test) {
            "==", "eq", "equalto" -> { x -> eq(x, arg) }
            else -> error("unimplemented reject test: $test")
        }
        when (subject) {
            is List<*> -> subject.filterNot(filter)
            else -> error("invalid type for reject: $subject")
        }
    }

    setExtension("apply_unique") { subject, _ ->
        if (subject == null) return@setExtension null
        when (subject) {
            is List<*> -> subject.toSet().toList()
            else -> error("invalid type for unique: $subject")
        }
    }

    setExtension("apply_join") { subject, args ->
        if (subject == null) return@setExtension null
        val (separator) = readArgs(args(), "separator")
        separator as String
        when (subject) {
            is List<*> -> subject.joinToString(separator) { v ->
                toString(v)
            }

            else -> error("invalid type for join: $subject")
        }
    }

    setExtension("apply_sort") { subject, _ ->
        if (subject == null) return@setExtension null
        when (subject) {
            is List<*> -> subject.sortedBy { toString(it) }
            else -> error("invalid type for sort: $subject")
        }
    }

    setExtension("apply_startswith") { subject, args ->
        if (subject == null) return@setExtension null
        val (prefix) = readArgs(args(), "prefix")
        prefix as String
        when (subject) {
            is String -> subject.startsWith(prefix)
            else -> error("invalid type for startswith: $subject")
        }
    }

    setExtension("apply_endswith") { subject, args ->
        if (subject == null) return@setExtension null
        val (suffix) = readArgs(args(), "suffix")
        suffix as String
        when (subject) {
            is String -> subject.endsWith(suffix)
            else -> error("invalid type for endswith: $subject")
        }
    }

    setExtension("apply_matches_glob") { subject, args ->
        if (subject == null) return@setExtension null
        val (pattern, ignoreCase) = readArgs(
            args(), "pattern", "ignore_case?"
        )
        pattern as String
        ignoreCase as Boolean?
        val options = when (ignoreCase ?: true) {
            true -> setOf(RegexOption.IGNORE_CASE)
            else -> emptySet()
        }
        val re = Glob(pattern).toRegex(options)
        when (subject) {
            is String -> re.matches(subject)
            else -> error("invalid type for matchesGlob: $subject")
        }
    }

    setExtension("apply_matches_regex") { subject, args ->
        if (subject == null) return@setExtension null
        val (pattern, ignoreCase) = readArgs(
            args(), "pattern", "ignore_case?"
        )
        pattern as String
        ignoreCase as Boolean?
        val options = when (ignoreCase ?: true) {
            true -> setOf(RegexOption.IGNORE_CASE)
            else -> emptySet()
        }
        val re = pattern.toRegex(options)
        when (subject) {
            is String -> re.matches(subject)
            else -> error("invalid type for matchesRegex: $subject")
        }
    }

    setExtension("apply_regex_replace") { subject, args ->
        if (subject == null) return@setExtension null
        val (pattern, replacement, ignoreCase) = readArgs(
            args(), "pattern", "replacement", "ignore_case?"
        )
        pattern as String
        replacement as String
        ignoreCase as Boolean?
        val options = when (ignoreCase ?: true) {
            true -> setOf(RegexOption.IGNORE_CASE)
            else -> emptySet()
        }
        val re = pattern.toRegex(options)
        when (subject) {
            is String -> subject.replace(re, replacement)
            else -> error("invalid type for regex_replace: $subject")
        }
    }

    setExtension("apply_replace") { subject, args ->
        if (subject == null) return@setExtension null
        val (search, replacement, ignoreCase) = readArgs(args(),
            "search", "replacement", "ignore_case?"
        )
        search as String
        replacement as String
        ignoreCase as Boolean?
        when (subject) {
            is String -> subject.replace(
                search,
                replacement,
                ignoreCase = ignoreCase ?: false
            )
            else -> error("invalid type for replace: $subject")
        }
    }

    setExtension("apply_in") { subject, args ->
        if (subject == null) return@setExtension null
        val (listValue) = readArgs(args(), "list")
        `in`(subject, listValue) == true
    }

    setExtension("apply_not_in") { subject, args ->
        if (subject == null) return@setExtension null
        val (listValue) = readArgs(args(), "list")
        `in`(subject, listValue) == false
    }

    setExtension("apply_trim") { subject, args ->
        if (subject == null) return@setExtension null
        val (chars) = readArgs(args(), "chars?")
        chars as String?
        when (subject) {
            is String -> if (chars == null) {
                subject.trim()
            } else {
                subject.trim(*chars.toCharArray())
            }
            else -> error("invalid type for trim: $subject")
        }
    }

    setExtension("apply_lower") { subject, _ ->
        if (subject == null) return@setExtension null
        when (subject) {
            is String -> subject.lowercase()
            else -> error("invalid type for lower: $subject")
        }
    }

    setExtension("apply_upper") { subject, _ ->
        if (subject == null) return@setExtension null
        when (subject) {
            is String -> subject.uppercase()
            else -> error("invalid type for upper: $subject")
        }
    }

    setExtension("apply_base64decode") { subject, _ ->
        if (subject == null) return@setExtension null
        when (subject) {
            is String -> subject.decodeBase64()?.toByteArray()
            else -> error("invalid type for base64decode")
        }
    }

    setExtension("apply_base64encode") { subject, _ ->
        if (subject == null) return@setExtension null
        when (subject) {
            is ByteString -> subject.base64()
            is ByteArray -> subject.toByteString().base64()
            else -> when (subject) {
                is List<*> -> {
                    val result = ByteArray(subject.size) { i ->
                        (subject[i] as Int).toByte()
                    }
                    result.toByteString().base64()
                }
                else -> error("invalid type for base64encode")
            }
        }
    }

    setExtension("apply_to_json") { subject, _ -> toJson(subject) }
    setExtension("apply_tojson") { subject, _ -> toJson(subject) }
    setExtension("apply_json") { subject, _ -> toJson(subject) }
    setExtension("apply_to_yaml") { subject, _ -> toYaml(subject) }
    setExtension("apply_toyaml") { subject, _ -> toYaml(subject) }
    setExtension("apply_yaml") { subject, _ -> toYaml(subject) }

    setExtension("apply_is_defined") { subject, _ -> !eq(subject, null) }
    setExtension("apply_is_not_defined") { subject, _ -> eq(subject, null) }

    setExtension("apply_is_string") { subject, _ -> isString(subject) }
    setExtension("apply_is_not_string") { subject, _ -> !isString(subject) }

    setExtension("apply_is_iterable") { subject, _ -> isIterable(subject) }
    setExtension("apply_is_not_iterable") { subject, _ -> !isIterable(subject) }
}
