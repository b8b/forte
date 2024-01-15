package org.cikit.forte.core

import okio.ByteString
import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.toByteString
import org.cikit.forte.emitter.JsonEmitter
import org.cikit.forte.emitter.YamlEmitter
import org.cikit.forte.parser.Expression
import kotlin.math.roundToInt

object Core {
    private fun readArgs(
        ctx: Context<*>,
        args: Expression.NamedArgs,
        vararg names: String,
    ): List<Any?> = args.read(*names).map { v ->
        if (v == null) {
            null
        } else {
            ctx.evalExpression(v)
        }
    }

    private fun eq(subject: Any?, other: Any?) = subject == other

    private fun `in`(subject: Any?, listValue: Any?): Boolean = when (listValue) {
        is String -> listValue.indexOf(subject as String) >= 0
        is List<*> -> subject in listValue
        else -> error("invalid type for in: $listValue")
    }

    private fun range(subject: Any?, other: Any?): Any = when (subject) {
        is Int -> (subject..(other as Int)).toList()
        else -> Undefined("invalid type for range: $subject")
    }

    private fun plus(subject: Any?, other: Any?): Any = when (subject) {
        is Int -> subject + (other as Int)
        is Float -> subject + (other as Float)
        is Double -> subject + (other as Double)
        is String -> subject + (other as String)
        else -> Undefined("invalid type for plus: $subject")
    }

    private fun minus(subject: Any?, other: Any?): Any = when (subject) {
        is Int -> subject - (other as Int)
        is Float -> subject - (other as Float)
        is Double -> subject - (other as Double)
        else -> Undefined("invalid type for minus: $subject")
    }

    private fun mul(subject: Any?, other: Any?): Any = when (subject) {
        is Int -> subject * (other as Int)
        is Float -> subject * (other as Float)
        is Double -> subject * (other as Double)
        else -> Undefined("invalid type for multiply: $subject")
    }

    private fun div(subject: Any?, other: Any?): Any = when (subject) {
        is Int -> subject / (other as Int)
        is Float -> subject / (other as Float)
        is Double -> subject / (other as Double)
        else -> Undefined("invalid type for division: $subject")
    }

    private fun toInt(subject: Any?): Int = when (subject) {
        is Boolean -> if (subject) 1 else 0
        is Int -> subject
        is Long -> subject.toInt()
        is Float -> subject.roundToInt()
        is Double -> subject.roundToInt()
        is String -> subject.toInt()
        else -> error("cannot convert to int: $subject")
    }

    private fun toString(subject: Any?): String = when (subject) {
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

    val context = Context.builder()
        .setCommand("set") { ctx, args ->
            val (varName, value) = readArgs(ctx, args, "varName", "value")
            ctx.setVar(varName as String, value)
        }

        .setControl("if") { ctx, branches ->
            for (cmd in branches) {
                when (cmd.name) {
                    "else" -> {
                        cmd.body(ctx)
                        break
                    }

                    else -> {
                        val (condition) = readArgs(ctx, cmd.args, "condition")
                        if (condition == true) {
                            cmd.body(ctx)
                            break
                        }
                    }
                }
            }
        }

        .setControl("for") { ctx, branches ->
            for (cmd in branches) {
                when (cmd.name) {
                    "else" -> {
                        cmd.body(ctx)
                        break
                    }

                    else -> {
                        val (varNames, listValue, recursive, condition) =
                            readArgs(
                                ctx,
                                cmd.args,
                                "varNames",
                                "listValue",
                                "recursive?",
                                "condition?"
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
                            ctx.scope()
                                .setVar(varName, item)
                                .apply(cmd.body)
                        }
                        if (done) {
                            break
                        }
                    }
                }
            }
        }

        .setControl("macro") { ctx, branches ->
            val cmd = branches.single()
            val (functionNameExpr, argNamesExpr, argDefaultsExpr) = cmd.args.read(
                "name", "argNames", "argDefaults"
            )
            val functionName = ctx.evalExpression(functionNameExpr!!) as String
            val argNames = ctx.evalExpression(argNamesExpr!!) as List<*>
            argDefaultsExpr as Expression.ObjectLiteral

            val finalArgDefaults = argDefaultsExpr.pairs.associate { (k, v) ->
                ctx.evalExpression(k).toString() to v
            }
            val finalArgNames = argNames.map { name ->
                if (finalArgDefaults.containsKey(name)) {
                    "${name}?"
                } else {
                    name.toString()
                }
            }

            val defCtx = ctx.build()

            ctx.setFunction(functionName) { ctx, macroArgs ->
                val argValues = macroArgs.read(*finalArgNames.toTypedArray())
                with (defCtx.builder()) {
                    for (i in finalArgNames.indices) {
                        val k = finalArgNames[i].removeSuffix("?")
                        val valueExpr = argValues[i]
                        val value = if (valueExpr != null) {
                            // evaluate in the caller context
                            ctx.evalExpression(valueExpr)
                        } else {
                            // evaluate default in the macro context
                            evalExpression(finalArgDefaults.getValue(k))
                        }
                        setVar(k, value)
                    }
                    captureToString()
                        .apply(cmd.body)
                        .result
                }
            }
        }

        .setOpFunction("not") { ctx, arg ->
            !(ctx.evalExpression(arg) as Boolean)
        }

        .setFunction("range") { ctx, args ->
            val (start, end) = readArgs(ctx, args, "start", "end_inclusive")
            range(start, end)
        }

        .setBinaryOpFunction("eq") { ctx, left, right ->
            eq(left, ctx.evalExpression(right))
        }
        .setBinaryOpFunction("ne") { ctx, left, right ->
            !eq(left, ctx.evalExpression(right))
        }

        .setBinaryOpFunction("gt") { ctx, left, right ->
            when (left) {
                is String -> left > (ctx.evalExpression(right) as String)
                is Float -> left > (ctx.evalExpression(right) as Float)
                is Double -> left > (ctx.evalExpression(right) as Double)
                is Int -> left > (ctx.evalExpression(right) as Int)
                else -> Undefined("invalid type for gt: $left")
            }
        }

        .setBinaryOpFunction("ge") { ctx, left, right ->
            when (left) {
                is String -> left >= (ctx.evalExpression(right) as String)
                is Float -> left >= (ctx.evalExpression(right) as Float)
                is Double -> left >= (ctx.evalExpression(right) as Double)
                is Int -> left >= (ctx.evalExpression(right) as Int)
                else -> Undefined("invalid type for ge: $left")
            }
        }

        .setBinaryOpFunction("lt") { ctx, left, right ->
            when (left) {
                is String -> left < (ctx.evalExpression(right) as String)
                is Float -> left < (ctx.evalExpression(right) as Float)
                is Double -> left < (ctx.evalExpression(right) as Double)
                is Int -> left < (ctx.evalExpression(right) as Int)
                else -> Undefined("invalid type for lt: $left")
            }
        }

        .setBinaryOpFunction("le") { ctx, left, right ->
            when (left) {
                is String -> left <= (ctx.evalExpression(right) as String)
                is Float -> left <= (ctx.evalExpression(right) as Float)
                is Double -> left <= (ctx.evalExpression(right) as Double)
                is Int -> left <= (ctx.evalExpression(right) as Int)
                else -> Undefined("invalid type for le: $left")
            }
        }

        .setBinaryOpFunction("or") { ctx, left, right ->
            (left as Boolean) || (ctx.evalExpression(right) as Boolean)
        }

        .setBinaryOpFunction("and") { ctx, left, right ->
            (left as Boolean) && (ctx.evalExpression(right) as Boolean)
        }

        .setBinaryOpFunction("range") { ctx, left, right -> range(left, ctx.evalExpression(right)) }

        .setBinaryOpFunction("plus") { ctx, left, right -> plus(left, ctx.evalExpression(right)) }
        .setBinaryOpFunction("minus") { ctx, left, right -> minus(left, ctx.evalExpression(right)) }
        .setBinaryOpFunction("mul") { ctx, left, right -> mul(left, ctx.evalExpression(right)) }
        .setBinaryOpFunction("div") { ctx, left, right -> div(left, ctx.evalExpression(right)) }

        .setBinaryFunction("pipe_int") { _, subject, _ -> toInt(subject) }

        .setBinaryFunction("pipe_float") { _, subject, _ ->
            when (subject) {
                is Boolean -> if (subject) 1.0 else 0.0
                is Int -> subject.toDouble()
                is Float -> subject.toDouble()
                is Double -> subject
                is String -> subject.toDouble()
                else -> Undefined("cannot convert to float: $subject")
            }
        }

        .setBinaryFunction("pipe_string") { _, subject, _ -> toString(subject) }

        .setBinaryFunction("pipe_list") { ctx, subject, args ->
            val (strings) = readArgs(ctx, args, "strings?")
            strings as String?
            when (subject) {
                is List<*> -> subject
                is String -> when (strings ?: "codePoints") {
                    "empty" -> emptyList<String>()
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

                    else -> Undefined("invalid option for strings: $strings")
                }

                else -> Undefined("cannot convert to list: $subject")
            }
        }

        .setRescueFunction("pipe_default") { ctx, _, args ->
            val (defaultValue) = readArgs(ctx, args, "default_value")
            defaultValue
        }

        .setBinaryFunction("pipe_default") { _, subject, _ -> subject }

        .setBinaryFunction("pipe_first") { _, subject, _ ->
            when (subject) {
                is List<*> -> subject.firstOrNull()
                else -> Undefined("invalid type for first: $subject")
            }
        }

        .setBinaryFunction("pipe_last") { _, subject, _ ->
            when (subject) {
                is List<*> -> subject.lastOrNull()
                else -> Undefined("invalid type for last: $subject")
            }
        }

        .setBinaryFunction("pipe_keys") { _, subject, _ ->
            when (subject) {
                is Map<*, *> -> subject.keys.toList()
                is List<*> -> listOf("size") + (0 until subject.size).toList()
                else -> emptyList()
            }
        }

        .setBinaryFunction("pipe_length") { _, subject, _ ->
            when (subject) {
                is List<*> -> subject.size
                is Map<*, *> -> subject.size
                is String -> subject.length
                else -> Undefined("invalid type for length: $subject")
            }
        }

        .setBinaryFunction("pipe_reject") { ctx, subject, args ->
            val (test, arg) = readArgs(ctx, args, "test", "arg?")
            val filter: (Any?) -> Boolean = when (test) {
                "==", "eq", "equalto" -> { x -> eq(x, arg) }
                else -> return@setBinaryFunction Undefined(
                    "unimplemented reject test: $test"
                )
            }
            when (subject) {
                is List<*> -> subject.filterNot(filter)
                else -> Undefined("invalid type for reject: $subject")
            }
        }

        .setBinaryFunction("pipe_unique") { _, subject, _ ->
            when (subject) {
                is List<*> -> subject.toSet().toList()
                else -> Undefined("invalid type for unique: $subject")
            }
        }

        .setBinaryFunction("pipe_join") { ctx, subject, args ->
            val (separator) = readArgs(ctx, args, "separator")
            separator as String
            when (subject) {
                is List<*> -> subject.joinToString(separator) { v ->
                    toString(v)
                }

                else -> Undefined("invalid type for join: $subject")
            }
        }

        .setBinaryFunction("pipe_sort") { _, subject, _ ->
            when (subject) {
                is List<*> -> subject.sortedBy { toString(it) }
                else -> Undefined("invalid type for sort: $subject")
            }
        }

        .setBinaryFunction("pipe_startswith") { ctx, subject, args ->
            val (prefix) = readArgs(ctx, args, "prefix")
            prefix as String
            when (subject) {
                is String -> subject.startsWith(prefix)
                else -> Undefined("invalid type for startswith: $subject")
            }
        }

        .setBinaryFunction("pipe_endswith") { ctx, subject, args ->
            val (suffix) = readArgs(ctx, args, "suffix")
            suffix as String
            when (subject) {
                is String -> subject.endsWith(suffix)
                else -> Undefined("invalid type for endswith: $subject")
            }
        }

        .setBinaryFunction("pipe_matches_glob") { ctx, subject, args ->
            val (pattern, ignoreCase) = readArgs(
                ctx, args, "pattern", "ignore_case?"
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
                else -> Undefined("invalid type for matchesGlob: $subject")
            }
        }

        .setBinaryFunction("pipe_matches_regex") { ctx, subject, args ->
            val (pattern, ignoreCase) = readArgs(
                ctx, args, "pattern", "ignore_case?"
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
                else -> Undefined("invalid type for matchesRegex: $subject")
            }
        }

        .setBinaryFunction("pipe_regex_replace") { ctx, subject, args ->
            val (pattern, replacement, ignoreCase) = readArgs(
                ctx, args, "pattern", "replacement", "ignore_case?"
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
                else -> Undefined("invalid type for regex_replace: $subject")
            }
        }

        .setBinaryFunction("pipe_replace") { ctx, subject, args ->
            val (search, replacement, ignoreCase) = readArgs(
                ctx, args, "search", "replacement", "ignore_case?"
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

                else -> Undefined("invalid type for replace: $subject")
            }
        }

        .setBinaryOpFunction("in") { ctx, subject, listValue ->
            `in`(subject, ctx.evalExpression(listValue))
        }

        .setBinaryOpFunction("not_in") { ctx, subject, listValue ->
            !`in`(subject, ctx.evalExpression(listValue))
        }

        .setBinaryFunction("pipe_trim") { ctx, subject, args ->
            val (chars) = readArgs(ctx, args, "chars?")
            chars as String?
            when (subject) {
                is String -> if (chars == null) {
                    subject.trim()
                } else {
                    subject.trim(*chars.toCharArray())
                }

                else -> Undefined("invalid type for trim: $subject")
            }
        }

        .setBinaryFunction("pipe_lower") { _, subject, _ ->
            when (subject) {
                is String -> subject.lowercase()
                else -> Undefined("invalid type for lower: $subject")
            }
        }

        .setBinaryFunction("pipe_upper") { _, subject, _ ->
            when (subject) {
                is String -> subject.uppercase()
                else -> Undefined("invalid type for upper: $subject")
            }
        }

        .setBinaryFunction("pipe_base64decode") { _, subject, _ ->
            when (subject) {
                is String -> subject.decodeBase64()?.toByteArray()
                    ?: Undefined("error decoding base64")

                else -> Undefined("invalid type for base64decode: $subject")
            }
        }

        .setBinaryFunction("pipe_base64encode") { _, subject, _ ->
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

                    else -> Undefined("invalid type for base64encode: $subject")
                }
            }
        }

        .setBinaryFunction("pipe_to_json") { _, subject, _ -> toJson(subject) }
        .setBinaryFunction("pipe_tojson") { _, subject, _ -> toJson(subject) }
        .setBinaryFunction("pipe_json") { _, subject, _ -> toJson(subject) }
        .setBinaryFunction("pipe_to_yaml") { _, subject, _ -> toYaml(subject) }
        .setBinaryFunction("pipe_toyaml") { _, subject, _ -> toYaml(subject) }
        .setBinaryFunction("pipe_yaml") { _, subject, _ -> toYaml(subject) }

        .setRescueFunction("is_defined") { _, _, _ -> false }
        .setRescueFunction("is_not_defined") { _, _, _ -> true }
        .setBinaryFunction("is_defined") { _, _, _ -> true }
        .setBinaryFunction("is_not_defined") { _, _, _ -> false }

        .setBinaryFunction("is_string") { _, subject, _ -> isString(subject) }
        .setBinaryFunction("is_not_string") { _, subject, _ -> !isString(subject) }

        .setBinaryFunction("is_iterable") { _, subject, _ -> isIterable(subject) }
        .setBinaryFunction("is_not_iterable") { _, subject, _ -> !isIterable(subject) }

        .setBinaryFunction("invoke_keys") { _, subject, _ ->
            when (subject) {
                is Map<*, *> -> subject.keys.toList()
                is List<*> -> listOf("size") + (0 until subject.size).toList()
                else -> Undefined("$subject.keys() is not a function")
            }
        }

        .setBinaryFunction("invoke") { ctx, subject, args ->
            when (subject) {
                is UnaryFunction -> {
                    subject.invoke(ctx, args)
                }

                else -> {
                    Undefined("$subject is not a function")
                }
            }
        }

        .build()

}
