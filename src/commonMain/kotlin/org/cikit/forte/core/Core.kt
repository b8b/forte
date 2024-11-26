package org.cikit.forte.core

import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.decodeToByteString
import kotlinx.io.bytestring.encode
import org.cikit.forte.emitter.JsonEmitter
import org.cikit.forte.emitter.YamlEmitter
import org.cikit.forte.parser.Expression
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.roundToInt

object Core {
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

    private fun toJsonFilter(
        @Suppress("UNUSED_PARAMETER")
        ctx: Context<*>,
        subject: Any?,
        args: NamedArgs
    ): String {
        args.requireEmpty()
        return JsonEmitter.encodeToString { emit(subject) }
    }

    private fun toYamlFilter(
        @Suppress("UNUSED_PARAMETER")
        ctx: Context<*>,
        subject: Any?,
        args: NamedArgs
    ): String {
        args.requireEmpty()
        return YamlEmitter.encodeToString { emit(subject) }
    }

    private fun eqTest(
        @Suppress("UNUSED_PARAMETER")
        ctx: Context<*>,
        subject: Any?,
        args: NamedArgs
    ) : Boolean {
        return args.use {
            val other = requireAny("other")
            eq(subject, other)
        }
    }

    private fun notEqTest(
        ctx: Context<*>,
        subject: Any?,
        args: NamedArgs
    ) : Boolean {
        return !eqTest(ctx, subject, args)
    }

    private fun isStringTest(
        @Suppress("UNUSED_PARAMETER")
        ctx: Context<*>,
        subject: Any?,
        args: NamedArgs
    ) : Boolean {
        args.requireEmpty()
        return subject is String
    }

    private fun isNotStringTest(
        ctx: Context<*>,
        subject: Any?,
        args: NamedArgs
    ) : Boolean {
        return !isStringTest(ctx, subject, args)
    }

    private fun isIterableTest(
        @Suppress("UNUSED_PARAMETER")
        ctx: Context<*>,
        subject: Any?,
        args: NamedArgs
    ) : Boolean {
        args.requireEmpty()
        return subject is List<*> || subject is String
    }

    private fun isNotIterableTest(
        ctx: Context<*>,
        subject: Any?,
        args: NamedArgs
    ) : Boolean {
        return !isIterableTest(ctx, subject, args)
    }

    private fun <T> Context.Builder<T>.defineFilter(
        name: String,
        implementation: Method
    ) = defineMethod(name, "pipe", implementation)

    private fun <T> Context.Builder<T>.defineTest(
        name: String,
        implementation: Method,
        negateImplementation: Method
    ): Context.Builder<T> {
        defineMethod(name, "is", implementation)
        defineMethod(name, "is_not", negateImplementation)
        return this
    }

    val context = Context.builder()
        .defineCommand("set") { ctx, args ->
            val varName = args.getValue("varName")
            val value = args.getValue("value")
            ctx.setVar(
                ctx.evalExpression(varName) as String,
                ctx.evalExpression(value)
            )
        }

        .defineControl("if") { ctx, branches ->
            for (cmd in branches) {
                when (cmd.name) {
                    "else" -> {
                        ctx.evalTemplate(cmd.body)
                        break
                    }

                    else -> {
                        val condition = cmd.args.getValue("condition")
                        if (ctx.evalExpression(condition) as Boolean) {
                            ctx.evalTemplate(cmd.body)
                            break
                        }
                    }
                }
            }
        }

        .defineControl("for") { ctx, branches ->
            for (cmd in branches) {
                when (cmd.name) {
                    "else" -> {
                        ctx.evalTemplate(cmd.body)
                        break
                    }

                    else -> {
                        val varNames = cmd.args.getValue("varNames")
                        val listValue = cmd.args.getValue("listValue")
                        val varName = (ctx.evalExpression(varNames) as List<*>)
                            .singleOrNull() ?: error(
                                "destructuring in for loop is not implemented"
                            )
                        var done = false
                        for (item in ctx.evalExpression(listValue) as List<*>) {
                            done = true
                            ctx.scope()
                                .setVar(varName as String, item)
                                .evalTemplate(cmd.body)
                        }
                        if (done) {
                            break
                        }
                    }
                }
            }
        }

        .defineControl("macro") { ctx, branches ->
            val cmd = branches.single()
            val functionNameExpr = cmd.args.getValue("name")
            val argNamesExpr = cmd.args.getValue("argNames")
            val argDefaultsExpr = cmd.args.getValue("argDefaults")
            val functionName = ctx.evalExpression(functionNameExpr) as String
            val argNames = ctx.evalExpression(argNamesExpr) as List<*>
            argDefaultsExpr as Expression.ObjectLiteral

            val finalArgDefaults = argDefaultsExpr.pairs.associate { (k, v) ->
                ctx.evalExpression(k).toString() to v
            }
            val finalArgNames = argNames.map { name -> name as String }

            val defCtx = ctx.build()

            ctx.defineFunction(functionName) { _, args ->
                val macroCtx = defCtx.builder()
                args.use {
                    for (i in finalArgNames.indices) {
                        val name = finalArgNames[i]
                        val defaultValue = finalArgDefaults[name]
                        val value = if (defaultValue == null) {
                            requireAny(name)
                        } else {
                            optionalNullable(
                                name,
                                { it },
                                { macroCtx.evalExpression(defaultValue) }
                            )
                        }
                        macroCtx.setVar(name, value)
                    }
                }
                macroCtx.captureToString()
                    .evalTemplate(cmd.body)
                    .result
            }
        }

        .defineBinaryOpFunction("get") { _, subject, key ->
            when (subject) {
                null -> Undefined("cannot access property $key of null")
                is Map<*, *> -> {
                    if (!subject.containsKey(key)) {
                        Undefined("$subject does not contain key '$key'")
                    } else {
                        subject[key]
                    }
                }
                is List<*> -> when (key) {
                    is Int -> subject.getOrElse(key) {
                        Undefined("index out of bounds: $subject[$key]")
                    }
                    else -> Undefined(
                        "cannot access property '$key' of list $subject"
                    )
                }
                else -> Undefined("cannot access property '$key' of '$subject'")
            }
        }

        .defineOpFunction("not") { _, arg ->
            !(arg as Boolean)
        }

        .defineFunction("range") { _, args ->
            args.use {
                val start: Int = require("start")
                val end: Int  = require("end_inclusive")
                range(start, end)
            }
        }

        .defineBinaryOpFunction("eq") { _, left, right ->
            eq(left, right)
        }
        .defineBinaryOpFunction("ne") { _, left, right ->
            !eq(left, right)
        }

        .defineBinaryOpFunction("gt") { _, left, right ->
            when (left) {
                is String -> left > (right as String)
                is Float -> left > (right as Float)
                is Double -> left > (right as Double)
                is Int -> left > (right as Int)
                else -> Undefined("invalid type for gt: $left")
            }
        }

        .defineBinaryOpFunction("ge") { _, left, right ->
            when (left) {
                is String -> left >= (right as String)
                is Float -> left >= (right as Float)
                is Double -> left >= (right as Double)
                is Int -> left >= (right as Int)
                else -> Undefined("invalid type for ge: $left")
            }
        }

        .defineBinaryOpFunction("lt") { _, left, right ->
            when (left) {
                is String -> left < (right as String)
                is Float -> left < (right as Float)
                is Double -> left < (right as Double)
                is Int -> left < (right as Int)
                else -> Undefined("invalid type for lt: $left")
            }
        }

        .defineBinaryOpFunction("le") { _, left, right ->
            when (left) {
                is String -> left <= (right as String)
                is Float -> left <= (right as Float)
                is Double -> left <= (right as Double)
                is Int -> left <= (right as Int)
                else -> Undefined("invalid type for le: $left")
            }
        }

        .defineBinaryOpFunction("or", condition = false) { _, right ->
            right as Boolean
        }

        .defineBinaryOpFunction("and", condition = true) { _, right ->
            right as Boolean
        }

        .defineBinaryOpFunction("range") { _, left, right ->
            range(left, right)
        }

        .defineBinaryOpFunction("plus") { _, left, right ->
            plus(left, right)
        }

        .defineBinaryOpFunction("minus") { _, left, right ->
            minus(left, right)
        }

        .defineBinaryOpFunction("mul") { _, left, right ->
            mul(left, right)
        }

        .defineBinaryOpFunction("div") { _, left, right ->
            div(left, right)
        }

        .defineFilter("int") { _, subject, args ->
            args.requireEmpty()
            toInt(subject)
        }

        .defineFilter("float") { _, subject, args ->
            args.requireEmpty()
            when (subject) {
                is Boolean -> if (subject) 1.0 else 0.0
                is Int -> subject.toDouble()
                is Float -> subject.toDouble()
                is Double -> subject
                is String -> subject.toDouble()
                else -> Undefined("cannot convert to float: $subject")
            }
        }

        .defineFilter("string") { _, subject, args ->
            args.requireEmpty()
            toString(subject)
        }

        .defineFilter("list") { _, subject, args ->
            val strings: String
            args.use {
                strings = optional("strings") { "codePoints" }
            }
            when (subject) {
                is List<*> -> subject
                is String -> {
                    when (strings) {
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

                        else -> Undefined(
                            "invalid option for strings: $strings"
                        )
                    }
                }

                else -> Undefined("cannot convert to list: $subject")
            }
        }

        .defineRescueMethod("default", "pipe") { _, _, args ->
            args.use { requireAny("default_value") }
        }

        .defineFilter("default") { _, subject, args ->
            args.use { requireAny("default_value") }
            subject
        }

        .defineFilter("first") { _, subject, args ->
            args.requireEmpty()
            when (subject) {
                is List<*> -> subject.firstOrNull()
                else -> Undefined("invalid type for first: $subject")
            }
        }

        .defineFilter("last") { _, subject, args ->
            args.requireEmpty()
            when (subject) {
                is List<*> -> subject.lastOrNull()
                else -> Undefined("invalid type for last: $subject")
            }
        }

        .defineFilter("keys") { _, subject, args ->
            args.requireEmpty()
            when (subject) {
                is Map<*, *> -> subject.keys.toList()
                is List<*> -> listOf("size") + (0 until subject.size).toList()
                else -> emptyList()
            }
        }

        .defineFilter("length") { _, subject, args ->
            args.requireEmpty()
            when (subject) {
                is List<*> -> subject.size
                is Map<*, *> -> subject.size
                is String -> subject.length
                else -> Undefined("invalid type for length: $subject")
            }
        }

        .defineFilter("select") { ctx, subject, args ->
            args.use {
                val test: String = require("test")
                val filterArgs = optional(
                    "arg",
                    { NamedArgs(listOf(it), emptyList()) },
                    { NamedArgs(emptyList(), emptyList()) }
                )
                val filter = ctx.getMethod(test, "is")
                    ?: error("undefined test: $test")
                when (subject) {
                    is List<*> -> subject.filter { item ->
                        filter(ctx, item, filterArgs) as Boolean
                    }

                    else -> Undefined("invalid type for reject: $subject")
                }
            }
        }

        .defineFilter("reject") { ctx, subject, args ->
            args.use {
                val test: String = require("test")
                val filterArgs = optional(
                    "arg",
                    { NamedArgs(listOf(it), emptyList()) },
                    { NamedArgs(emptyList(), emptyList()) }
                )
                val filter = ctx.getMethod(test, "is")
                    ?: error("undefined test: $test")
                when (subject) {
                    is List<*> -> subject.filterNot { item ->
                        filter(ctx, item, filterArgs) as Boolean
                    }

                    else -> Undefined("invalid type for reject: $subject")
                }
            }
        }

        .defineFilter("unique") { _, subject, args ->
            args.requireEmpty()
            when (subject) {
                is List<*> -> subject.toSet().toList()
                else -> Undefined("invalid type for unique: $subject")
            }
        }

        .defineFilter("join") { _, subject, args ->
            args.use {
                val separator: String = optional("separator") { ", " }
                when (subject) {
                    is List<*> -> subject.joinToString(separator) { v ->
                        toString(v)
                    }

                    else -> Undefined("invalid type for join: $subject")
                }
            }
        }

        .defineFilter("sort") { _, subject, args ->
            args.use {
                val reverse = optional("reverse") { false }
                val caseSensitive = optional("case_sensitive") { false }
                val comparator: Comparator<Any?> = if (reverse) {
                    Comparator { a, b ->
                        toString(b).compareTo(toString(a), !caseSensitive)
                    }
                } else {
                    Comparator { a, b ->
                        toString(a).compareTo(toString(b), !caseSensitive)
                    }
                }
                when (subject) {
                    is List<*> -> subject.sortedWith(comparator)
                    else -> Undefined("invalid type for sort: $subject")
                }
            }
        }

        .defineFilter("startswith") { _, subject, args ->
            args.use {
                val prefix: String = require("prefix")
                when (subject) {
                    is String -> subject.startsWith(prefix)
                    else -> Undefined("invalid type for startswith: $subject")
                }
            }
        }

        .defineFilter("endswith") { _, subject, args ->
            args.use {
                val suffix: String = require("suffix")
                when (subject) {
                    is String -> subject.endsWith(suffix)
                    else -> Undefined("invalid type for endswith: $subject")
                }
            }
        }

        .defineFilter("matches_glob") { _, subject, args ->
            args.use {
                val pattern: String = require("pattern")
                val ignoreCase: Boolean = optional("ignore_case") { true }
                val re = if (ignoreCase) {
                    Glob(pattern).toRegex(RegexOption.IGNORE_CASE)
                } else {
                    Glob(pattern).toRegex()
                }
                when (subject) {
                    is String -> re.matches(subject)
                    else -> Undefined("invalid type for matchesGlob: $subject")
                }
            }
        }

        .defineFilter("matches_regex") { _, subject, args ->
            args.use {
                val pattern: String = require("pattern")
                val ignoreCase: Boolean = optional("ignore_case") { true }
                val re = if (ignoreCase) {
                    Regex(pattern, RegexOption.IGNORE_CASE)
                } else {
                    Regex(pattern)
                }
                when (subject) {
                    is String -> re.matches(subject)
                    else -> Undefined("invalid type for matchesRegex: $subject")
                }
            }
        }

        .defineFilter("regex_replace") { _, subject, args ->
            args.use {
                val pattern: String = require("pattern")
                val replacement: String = require("replacement")
                val ignoreCase: Boolean = optional("ignore_case") { true }
                val re = if (ignoreCase) {
                    Regex(pattern, RegexOption.IGNORE_CASE)
                } else {
                    Regex(pattern)
                }
                when (subject) {
                    is String -> subject.replace(re, replacement)
                    else -> Undefined(
                        "invalid type for regex_replace: $subject"
                    )
                }
            }
        }

        .defineFilter("replace") { _, subject, args ->
            args.use {
                val search: String = require("search")
                val replacement: String = require("replacement")
                val ignoreCase: Boolean = optional("ignore_case") { false }
                when (subject) {
                    is String -> subject.replace(
                        search,
                        replacement,
                        ignoreCase = ignoreCase
                    )

                    else -> Undefined("invalid type for replace: $subject")
                }
            }
        }

        .defineBinaryOpFunction("in") { _, subject, listValue ->
            `in`(subject, listValue)
        }

        .defineBinaryOpFunction("not_in") { _, subject, listValue ->
            !`in`(subject, listValue)
        }

        .defineFilter("trim") { _, subject, args ->
            args.use {
                val chars: String = optional("chars") { "" }
                when (subject) {
                    is String -> if (chars.isEmpty()) {
                        subject.trim()
                    } else {
                        subject.trim(*chars.toCharArray())
                    }

                    else -> Undefined("invalid type for trim: $subject")
                }
            }
        }

        .defineFilter("lower") { _, subject, args ->
            args.requireEmpty()
            when (subject) {
                is String -> subject.lowercase()
                else -> Undefined("invalid type for lower: $subject")
            }
        }

        .defineFilter("upper") { _, subject, args ->
            args.requireEmpty()
            when (subject) {
                is String -> subject.uppercase()
                else -> Undefined("invalid type for upper: $subject")
            }
        }

        .defineFilter("base64decode") { _, subject, args ->
            args.requireEmpty()
            @OptIn(ExperimentalEncodingApi::class)
            when (subject) {
                is String -> Base64.decodeToByteString(subject)

                else -> Undefined("invalid type for base64decode: $subject")
            }
        }

        .defineFilter("base64encode") { _, subject, args ->
            args.requireEmpty()
            @OptIn(ExperimentalEncodingApi::class)
            when (subject) {
                is ByteString -> Base64.UrlSafe.encode(subject)
                is ByteArray -> Base64.UrlSafe.encode(subject)
                else -> when (subject) {
                    is List<*> -> {
                        val result = ByteArray(subject.size) { i ->
                            (subject[i] as Int).toByte()
                        }
                        Base64.UrlSafe.encode(result)
                    }

                    else -> Undefined("invalid type for base64encode: $subject")
                }
            }
        }

        .defineFilter("to_json", ::toJsonFilter)
        .defineFilter("tojson", ::toJsonFilter)
        .defineFilter("json", ::toJsonFilter)
        .defineFilter("to_yaml", ::toYamlFilter)
        .defineFilter("toyaml", ::toYamlFilter)
        .defineFilter("yaml", ::toYamlFilter)

        .defineTest("eq", ::eqTest, ::notEqTest)
        .defineTest("equalto", ::eqTest, ::notEqTest)
        .defineTest("==", ::eqTest, ::notEqTest)

        .defineRescueMethod("defined", "is") { _, _, args ->
            args.requireEmpty()
            false
        }

        .defineRescueMethod("defined", "is_not") { _, _, args ->
            args.requireEmpty()
            true
        }

        .defineTest("defined",
            { _, _, args ->
                args.requireEmpty()
                true
            },
            { _, _, args ->
                args.requireEmpty()
                false
            }
        )

        .defineTest("string", ::isStringTest, ::isNotStringTest)
        .defineTest("iterable", ::isIterableTest, ::isNotIterableTest)

        .defineMethod("keys") { _, subject, args ->
            args.requireEmpty()
            when (subject) {
                is Map<*, *> -> subject.keys.toList()
                is List<*> -> (0 until subject.size).toList()
                else -> Undefined("$subject.keys() is not a function")
            }
        }

        .build()

}
