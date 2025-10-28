package org.cikit.forte.core

import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.decodeToByteString
import kotlinx.io.bytestring.encode
import org.cikit.forte.core.CoreDeprecated.defineDeprecatedFunctions
import org.cikit.forte.emitter.JsonEmitter
import org.cikit.forte.emitter.YamlEmitter
import org.cikit.forte.eval.EvaluationResult
import org.cikit.forte.eval.evalExpression
import org.cikit.forte.eval.evalTemplate
import org.cikit.forte.eval.tryEvalExpression
import org.cikit.forte.parser.Expression
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.roundToInt

object Core {
    private fun eq(subject: Any?, other: Any?) = subject == other

    private fun `in`(subject: Any?, list: Any?): Boolean = when (list) {
        is String -> list.indexOf(subject as String) >= 0
        is Iterable<*> -> subject in list
        else -> error("invalid type for in: ${typeName(list)}")
    }

    private fun range(subject: Any?, other: Any?): Any = when (subject) {
        is Int -> (subject..(other as Int)).toList()
        is Long -> (subject .. (other as Long)).toList()
        else -> error("invalid type for range: ${typeName(subject)}")
    }

    private fun plus(subject: Any?, other: Any?): Any = when (subject) {
        is Int -> subject + (other as Int)
        is Float -> subject + (other as Float)
        is Double -> subject + (other as Double)
        is String -> subject + (other as String)
        else -> error("invalid type for plus: ${typeName(subject)}")
    }

    private fun minus(subject: Any?, other: Any?): Any = when (subject) {
        is Int -> subject - (other as Int)
        is Float -> subject - (other as Float)
        is Double -> subject - (other as Double)
        else -> error("invalid type for minus: ${typeName(subject)}")
    }

    private fun mul(subject: Any?, other: Any?): Any = when (subject) {
        is Int -> subject * (other as Int)
        is Float -> subject * (other as Float)
        is Double -> subject * (other as Double)
        else -> error("invalid type for multiply: ${typeName(subject)}")
    }

    private fun div(subject: Any?, other: Any?): Any = when (subject) {
        is Int -> subject / (other as Int)
        is Float -> subject / (other as Float)
        is Double -> subject / (other as Double)
        else -> error("invalid type for division: ${typeName(subject)}")
    }

    private fun toInt(subject: Any?): Int = when (subject) {
        is Boolean -> if (subject) 1 else 0
        is Int -> subject
        is Long -> subject.toInt()
        is Float -> subject.roundToInt()
        is Double -> subject.roundToInt()
        is String -> subject.toInt()
        else -> error("cannot convert ${typeName(subject)} to int")
    }

    private fun toString(subject: Any?): String = when (subject) {
        null -> "null"
        is Boolean -> if (subject) "true" else "false"
        is Int -> subject.toString()
        is Float -> subject.toString()
        is Double -> subject.toString()
        is String -> subject
        else -> error("cannot convert ${typeName(subject)} to string")
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

    private fun listFilter(
        ctx: Context<*>,
        filter: Method,
        filterArgs: NamedArgs,
        source: Iterable<*>,
    ): Any? {
        val target = mutableListOf<Any?>()
        val sourceIt = source.iterator()
        while (sourceIt.hasNext()) {
            val item = sourceIt.next()
            val include = filter.invoke(ctx, item, filterArgs)
            if (include == true) {
                target.add(item)
            }
            if (include is Suspended) {
                return Suspended {
                    if (include.eval() == true) {
                        target.add(item)
                    }
                    while (sourceIt.hasNext()) {
                        val item = sourceIt.next()
                        val include = filter.invoke(ctx, item, filterArgs)
                        if (include == true) {
                            target.add(item)
                        }
                        if (include is Suspended) {
                            val finalInclude = include.eval()
                            if (finalInclude == true) {
                                target.add(item)
                            }
                        }
                    }
                    target.toList()
                }
            }
        }
        return target.toList()
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
        .defineDeprecatedFunctions()

        .defineCommandTag("set") { ctx, args ->
            val varName = args.getValue("varName")
            val value = args.getValue("value")
            ctx.setVar(
                ctx.evalExpression(varName) as String,
                ctx.evalExpression(value)
            )
        }

        .defineControlTag("if") { ctx, branches ->
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

        .defineControlTag("for") { ctx, branches ->
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
                        val list = ctx.evalExpression(listValue)
                        for (item in list as Iterable<*>) {
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

        .defineControlTag("macro") { ctx, branches ->
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
                                { macroCtx.tryEvalExpression(defaultValue) }
                            )
                        }
                        macroCtx.setVar(name, value)
                    }
                }
                Suspended {
                    for (name in finalArgNames) {
                        when (val value = macroCtx.getVar(name)) {
                            is EvaluationResult -> {
                                macroCtx.setVar(name, value.get())
                            }
                        }
                    }
                    macroCtx.captureToString()
                        .evalTemplate(cmd.body)
                        .result
                }
            }
        }

        .defineFilter("get") { _, subject, args ->
            val key: Any?
            args.use { key = requireAny("key") }
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
                else -> error("invalid type for gt: ${typeName(left)}")
            }
        }

        .defineBinaryOpFunction("ge") { _, left, right ->
            when (left) {
                is String -> left >= (right as String)
                is Float -> left >= (right as Float)
                is Double -> left >= (right as Double)
                is Int -> left >= (right as Int)
                else -> error("invalid type for ge: ${typeName(left)}")
            }
        }

        .defineBinaryOpFunction("lt") { _, left, right ->
            when (left) {
                is String -> left < (right as String)
                is Float -> left < (right as Float)
                is Double -> left < (right as Double)
                is Int -> left < (right as Int)
                else -> error("invalid type for lt: ${typeName(left)}")
            }
        }

        .defineBinaryOpFunction("le") { _, left, right ->
            when (left) {
                is String -> left <= (right as String)
                is Float -> left <= (right as Float)
                is Double -> left <= (right as Double)
                is Int -> left <= (right as Int)
                else -> error("invalid type for le: ${typeName(left)}")
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
                else -> error("cannot convert ${typeName(subject)} to float")
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

                        else -> error(
                            "invalid option for strings: $strings"
                        )
                    }
                }

                else -> error("cannot convert ${typeName(subject)} to list")
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
                is Iterable<*> -> subject.firstOrNull()
                else -> error("invalid type for first: ${typeName(subject)}")
            }
        }

        .defineFilter("last") { _, subject, args ->
            args.requireEmpty()
            when (subject) {
                is List<*> -> subject.lastOrNull()
                else -> error("invalid type for last: ${typeName(subject)}")
            }
        }

        .defineFilter("keys") { _, subject, args ->
            args.requireEmpty()
            when (subject) {
                is Map<*, *> -> subject.keys.toList()
                is List<*> -> listOf("size") + subject.indices.toList()
                else -> emptyList()
            }
        }

        .defineFilter("length") { _, subject, args ->
            args.requireEmpty()
            when (subject) {
                is Map<*, *> -> subject.size
                is Collection<*> -> subject.size
                is String -> subject.length
                else -> error("invalid type for length: ${typeName(subject)}")
            }
        }

        .defineFilter("select") { ctx, subject, args ->
            val test: String
            val filterArgs: NamedArgs
            args.use {
                test = require("test")
                filterArgs = optional(
                    "arg",
                    { NamedArgs(listOf(it), emptyList()) },
                    { NamedArgs(emptyList(), emptyList()) }
                )
            }
            val filter = ctx.getMethod(test, "is")
                ?: error("undefined test: $test")
            if (subject !is Iterable<*>) {
                error("invalid type for select: ${typeName(subject)}")
            }
            listFilter(ctx, filter, filterArgs, subject)
        }

        .defineFilter("reject") { ctx, subject, args ->
            val test: String
            val filterArgs: NamedArgs
            args.use {
                test = require("test")
                filterArgs = optional(
                    "arg",
                    { NamedArgs(listOf(it), emptyList()) },
                    { NamedArgs(emptyList(), emptyList()) }
                )
            }
            val filter = ctx.getMethod(test, "is_not")
                ?: error("undefined test: $test")
            if (subject !is Iterable<*>) {
                error("invalid type for reject: ${typeName(subject)}")
            }
            listFilter(ctx, filter, filterArgs, subject)
        }

        .defineFilter("selectattr") { ctx, subject, args ->
            val attr: Any
            val test: String
            val filterArgs: NamedArgs
            args.use {
                attr = require("attr")
                test = optional("test") { "defined" }
                filterArgs = optional(
                    "arg",
                    { NamedArgs(listOf(it), emptyList()) },
                    { NamedArgs(emptyList(), emptyList()) }
                )
            }
            val get = ctx.getMethod("get", CoreOperators.Filter.value)
                ?: error(
                    "filter `selectattr` failed with undefined filter `get`"
                )
            val getArgs = NamedArgs(listOf(attr), emptyList())
            val filter = ctx.getMethod(test, CoreOperators.Test.value)
                ?: error(
                    "filter `selectattr` failed with undefined test '$test'"
                )
            if (subject !is Iterable<*>) {
                error(
                    "filter `selectattr` is undefined " +
                            "for '${typeName(subject)}'"
                )
            }
            val sourceIt = subject.iterator()
            val target = mutableListOf<Any?>()
            while (sourceIt.hasNext()) {
                val item = sourceIt.next()
                val candidate = get.invoke(ctx, item, getArgs)
                val include = filter.invoke(ctx, candidate, filterArgs)
                if (include == true) {
                    target.add(item)
                } else if (include is Suspended) {
                    return@defineFilter Suspended {
                        if (include.eval() == true) {
                            target.add(item)
                        }
                        while (sourceIt.hasNext()) {
                            val item = sourceIt.next()
                            val candidate = get.invoke(ctx, item, getArgs)
                            val include = filter.invoke(
                                ctx,
                                candidate,
                                filterArgs
                            )
                            if (include == true) {
                                target.add(item)
                            }
                            if (include is Suspended) {
                                val finalInclude = include.eval()
                                if (finalInclude == true) {
                                    target.add(item)
                                }
                            }
                        }
                        target.toList()
                    }
                }
            }
            target.toList()
        }

        .defineFilter("unique") { _, subject, args ->
            args.requireEmpty()
            when (subject) {
                is Iterable<*> -> subject.toSet().toList()
                else -> error("invalid type for unique: ${typeName(subject)}")
            }
        }

        .defineFilter("join") { _, subject, args ->
            args.use {
                val separator: String = optional("separator") { ", " }
                when (subject) {
                    is Iterable<*> -> subject.joinToString(separator) { v ->
                        toString(v)
                    }

                    else -> error("invalid type for join: ${typeName(subject)}")
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
                    is Iterable<*> -> subject.sortedWith(comparator)
                    else -> error("invalid type for sort: ${typeName(subject)}")
                }
            }
        }

        .defineFilter("startswith") { _, subject, args ->
            args.use {
                val prefix: String = require("prefix")
                when (subject) {
                    is String -> subject.startsWith(prefix)
                    else -> error(
                        "invalid type for startswith: ${typeName(subject)}"
                    )
                }
            }
        }

        .defineFilter("endswith") { _, subject, args ->
            args.use {
                val suffix: String = require("suffix")
                when (subject) {
                    is String -> subject.endsWith(suffix)
                    else -> error(
                        "invalid type for endswith: ${typeName(subject)}"
                    )
                }
            }
        }

        .defineFilter("matches_glob") { _, subject, args ->
            args.use {
                val pattern: String = require("pattern")
                val ignoreCase: Boolean = optional("ignore_case") { true }
                val re = Glob(pattern, ignoreCase = ignoreCase).toRegex()
                when (subject) {
                    is String -> re.matches(subject)
                    else -> error(
                        "invalid type for matchesGlob: ${typeName(subject)}"
                    )
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
                    else -> error(
                        "invalid type for matchesRegex: ${typeName(subject)}"
                    )
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
                    else -> error(
                        "invalid type for regex_replace: ${typeName(subject)}"
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

                    else -> error(
                        "invalid type for replace: ${typeName(subject)}"
                    )
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

                    else -> error(
                        "invalid type for trim: ${typeName(subject)}"
                    )
                }
            }
        }

        .defineFilter("lower") { _, subject, args ->
            args.requireEmpty()
            when (subject) {
                is String -> subject.lowercase()
                else -> error("invalid type for lower: ${typeName(subject)}")
            }
        }

        .defineFilter("upper") { _, subject, args ->
            args.requireEmpty()
            when (subject) {
                is String -> subject.uppercase()
                else -> error("invalid type for upper: ${typeName(subject)}")
            }
        }

        .defineFilter("base64decode") { _, subject, args ->
            args.requireEmpty()
            @OptIn(ExperimentalEncodingApi::class)
            when (subject) {
                is String -> Base64.decodeToByteString(subject)

                else -> error(
                    "invalid type for base64decode: ${typeName(subject)}"
                )
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

                    else -> error(
                        "invalid type for base64encode: ${typeName(subject)}"
                    )
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
                else -> error("$subject.keys() is not a function")
            }
        }

        .build()

}
