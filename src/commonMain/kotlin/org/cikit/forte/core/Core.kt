package org.cikit.forte.core

import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.buildByteString
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

object Core {
    private fun eq(subject: Any?, other: Any?) = subject == other

    private fun binOpTypeError(
        op: String,
        left: Any?,
        right: Any?
    ): Nothing =
        throw IllegalArgumentException(
            buildString {
                append("operator `")
                append(op)
                append("` undefined for operands of type '")
                append(typeName(left))
                append("' and '")
                append(typeName(right))
                append("'")
            }
        )

    private fun `in`(subject: Any?, list: Any?): Boolean = when (list) {
        is CharSequence -> when (subject) {
            is String -> list.indexOf(subject) >= 0
            is CharSequence -> list.indexOf(subject.toString()) >= 0
            is Char -> subject in list
            else -> binOpTypeError("in", subject, list)
        }
        is Iterable<*> -> subject in list
        else -> binOpTypeError("in", subject, list)
    }

    private fun range(subject: Any?, other: Any?): Any = when (subject) {
        is Int -> when (other) {
            is Long -> (subject .. other).toList()
            is Number -> (subject..other.toInt()).toList()
            else -> binOpTypeError("range", subject, other)
        }

        is Long -> when (other) {
            is Number -> (subject..other.toLong()).toList()
            else -> binOpTypeError("range", subject, other)
        }

        is Number -> when (other) {
            is Long -> (subject.toLong()..other).toList()
            is Number -> (subject.toInt()..other.toInt()).toList()
            else -> binOpTypeError("range", subject, other)
        }

        else -> binOpTypeError("range", subject, other)
    }

    private fun compareNumbers(a: Number, b: Number): Int {
        return when (a) {
            is Int -> when (b) {
                is Int -> a.compareTo(b)
                is Double -> a.toDouble().compareTo(b)
                is Long -> a.toLong().compareTo(b)
                is Float -> a.toDouble().compareTo(b.toDouble())

                else -> a.compareTo(b.toInt())
            }
            is Double -> a.compareTo(b.toDouble())
            is Long -> when (b) {
                is Double -> a.toDouble().compareTo(b)
                is Float -> a.toDouble().compareTo(b.toDouble())
                else -> a.compareTo(b.toLong())
            }
            is Float -> a.toDouble().compareTo(b.toDouble())

            else -> when (b) {
                is Double -> a.toDouble().compareTo(b)
                is Float -> a.toDouble().compareTo(b.toDouble())

                else -> a.toLong().compareTo(b.toLong())
            }
        }
    }

    private fun compareAny(
        a: Any?,
        b: Any?,
        ignoreCase: Boolean = false
    ): Int? {
        if (b == null) {
            return null
        }
        return when (a) {
            null -> null

            is Number -> when (b) {
                is Number -> compareNumbers(a, b)

                else -> null
            }

            is String -> when (b) {
                is String -> a.compareTo(b, ignoreCase)
                is CharSequence -> a.compareTo(b.toString(), ignoreCase)

                else -> null
            }

            is CharSequence -> when (b) {
                is String -> a.toString().compareTo(b, ignoreCase)
                is CharSequence -> a.toString().compareTo(
                    b.toString(),
                    ignoreCase
                )

                else -> null
            }

            else -> null
        }
    }

    private fun plus(subject: Any?, other: Any?): Any = when (subject) {
        is Int -> when (other) {
            is Int -> subject + other
            is Double -> subject + other
            is Long -> subject + other
            is Float -> subject + other
            is Number -> subject + other.toInt()
            else -> binOpTypeError("plus", subject, other)
        }

        is Double -> when (other) {
            is Number -> subject + other.toDouble()
            else -> binOpTypeError("plus", subject, other)
        }

        is Long -> when (other) {
            is Int -> subject + other
            is Double -> subject + other
            is Long -> subject + other
            is Float -> subject + other
            is Number -> subject + other.toLong()
            else -> binOpTypeError("plus", subject, other)
        }

        is Float -> when (other) {
            is Double -> subject + other
            is Number -> subject + other.toFloat()
            else -> binOpTypeError("plus", subject, other)
        }

        is Number -> when (other) {
            is Int -> subject.toInt() + other
            is Double -> subject.toDouble() + other
            is Long -> subject.toLong() + other
            is Float -> subject.toFloat() + other
            is Number -> subject.toInt() + other.toInt()
            else -> binOpTypeError("plus", subject, other)
        }

        is CharSequence -> when (other) {
            is CharSequence -> buildString {
                append(subject)
                append(other)
            }

            else -> binOpTypeError("plus", subject, other)
        }

        else -> binOpTypeError("plus", subject, other)
    }

    private fun minus(subject: Any?, other: Any?): Any = when (subject) {
        is Int -> when (other) {
            is Int -> subject - other
            is Double -> subject - other
            is Long -> subject - other
            is Float -> subject - other
            is Number -> subject - other.toInt()
            else -> binOpTypeError("minus", subject, other)
        }

        is Double -> when (other) {
            is Number -> subject - other.toDouble()
            else -> binOpTypeError("minus", subject, other)
        }

        is Long -> when (other) {
            is Int -> subject - other
            is Double -> subject - other
            is Long -> subject - other
            is Float -> subject - other
            is Number -> subject - other.toLong()
            else -> binOpTypeError("minus", subject, other)
        }

        is Float -> when (other) {
            is Double -> subject - other
            is Number -> subject - other.toFloat()
            else -> binOpTypeError("minus", subject, other)
        }

        is Number -> when (other) {
            is Int -> subject.toInt() - other
            is Double -> subject.toDouble() - other
            is Long -> subject.toLong() - other
            is Float -> subject.toFloat() - other
            is Number -> subject.toInt() - other.toInt()
            else -> binOpTypeError("minus", subject, other)
        }

        else -> binOpTypeError("minus", subject, other)
    }

    private fun mul(subject: Any?, other: Any?): Any = when (subject) {
        is Int -> when (other) {
            is Int -> subject * other
            is Double -> subject * other
            is Long -> subject * other
            is Float -> subject * other
            is Number -> subject * other.toInt()
            else -> binOpTypeError("mul", subject, other)
        }

        is Double -> when (other) {
            is Number -> subject * other.toDouble()
            else -> binOpTypeError("mul", subject, other)
        }

        is Long -> when (other) {
            is Int -> subject * other
            is Double -> subject * other
            is Long -> subject * other
            is Float -> subject * other
            is Number -> subject * other.toLong()
            else -> binOpTypeError("mul", subject, other)
        }

        is Float -> when (other) {
            is Double -> subject * other
            is Number -> subject * other.toFloat()
            else -> binOpTypeError("mul", subject, other)
        }

        is Number -> when (other) {
            is Int -> subject.toInt() * other
            is Double -> subject.toDouble() * other
            is Long -> subject.toLong() * other
            is Float -> subject.toFloat() * other
            is Number -> subject.toInt() * other.toInt()
            else -> binOpTypeError("mul", subject, other)
        }

        else -> binOpTypeError("mul", subject, other)
    }

    private fun div(subject: Any?, other: Any?): Any = when (subject) {
        is Int -> when (other) {
            is Int -> subject / other
            is Double -> subject / other
            is Long -> subject / other
            is Float -> subject / other
            is Number -> subject / other.toInt()
            else -> binOpTypeError("div", subject, other)
        }

        is Double -> when (other) {
            is Number -> subject / other.toDouble()
            else -> binOpTypeError("div", subject, other)
        }

        is Long -> when (other) {
            is Int -> subject / other
            is Double -> subject / other
            is Long -> subject / other
            is Float -> subject / other
            is Number -> subject / other.toLong()
            else -> binOpTypeError("div", subject, other)
        }

        is Float -> when (other) {
            is Double -> subject / other
            is Number -> subject / other.toFloat()
            else -> binOpTypeError("div", subject, other)
        }

        is Number -> when (other) {
            is Int -> subject.toInt() / other
            is Double -> subject.toDouble() / other
            is Long -> subject.toLong() / other
            is Float -> subject.toFloat() / other
            is Number -> subject.toInt() / other.toInt()
            else -> binOpTypeError("div", subject, other)
        }

        else -> binOpTypeError("div", subject, other)
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
    ): Any {
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

    private fun isNumberTest(
        @Suppress("UNUSED_PARAMETER")
        ctx: Context<*>,
        subject: Any?,
        args: NamedArgs
    ) : Boolean {
        args.requireEmpty()
        return subject is Number
    }

    private fun isNotNumberTest(
        ctx: Context<*>,
        subject: Any?,
        args: NamedArgs
    ) : Boolean {
        return !isNumberTest(ctx, subject, args)
    }

    private fun isStringTest(
        @Suppress("UNUSED_PARAMETER")
        ctx: Context<*>,
        subject: Any?,
        args: NamedArgs
    ) : Boolean {
        args.requireEmpty()
        return subject is CharSequence
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
        return subject is Iterable<*> || subject is CharSequence
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
    ) = defineMethod(name, CoreOperators.Filter.value, implementation)

    private fun <T> Context.Builder<T>.defineTest(
        name: String,
        implementation: Method,
        negateImplementation: Method
    ): Context.Builder<T> {
        defineMethod(name, CoreOperators.Test.value, implementation)
        defineMethod(name, CoreOperators.TestNot.value, negateImplementation)
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
                        val value = ctx.evalExpression(condition)
                        require(value is Boolean) {
                            "invalid type '${typeName(value)}' " +
                                    "for arg 'condition': expected 'Boolean'"
                        }
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
                            .singleOrNull() ?: throw IllegalArgumentException(
                                "destructuring in for loop is not implemented"
                            )
                        var done = false
                        val list = ctx.evalExpression(listValue)
                        require(list is Iterable<*>) {
                            "invalid type '${typeName(list)}' for arg " +
                                    "'listValue': expected 'Iterable<*>'"

                        }
                        for (item in list) {
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
                is Map<*, *> -> {
                    if (!subject.containsKey(key)) {
                        Undefined("key '$key' of type '${typeName(key)}' " +
                                "is missing in the Map operand " +
                                "of type '${typeName(subject)}'")
                    } else {
                        subject[key]
                    }
                }
                is List<*> -> when (key) {
                    is Int -> subject.getOrElse(key) {
                        Undefined("index $key out of bounds for List operand " +
                                "of type '${typeName(subject)}' " +
                                "with size ${subject.size}")
                    }
                    else -> Undefined(
                        "invalid key '$key' of type '${typeName(key)}' for " +
                                " List operand of type '${typeName(subject)}'"
                    )
                }
                else -> Undefined(
                    "invalid operand " +
                            "of type '${typeName(subject)}' with key '$key' " +
                            "of type '${typeName(key)}'"
                )
            }
        }

        .defineOpFunction("not") { _, arg ->
            require(arg is Boolean) {
                "operator `not` is undefined for operand " +
                        "of type '${typeName(arg)}'"
            }
            !arg
        }

        .defineFunction("range") { _, args ->
            val start: Number
            val end: Number
            args.use {
                start = require("start")
                end  = require("end_inclusive")
            }
            range(start, end)
        }

        .defineBinaryOpFunction("eq") { _, left, right ->
            eq(left, right)
        }
        .defineBinaryOpFunction("ne") { _, left, right ->
            !eq(left, right)
        }

        .defineBinaryOpFunction("gt") { _, left, right ->
            when (val result = compareAny(left, right)) {
                null -> binOpTypeError("gt", left, right)
                else -> result > 0
            }
        }

        .defineBinaryOpFunction("ge") { _, left, right ->
            when (val result = compareAny(left, right)) {
                null -> binOpTypeError("ge", left, right)
                else -> result >= 0
            }
        }

        .defineBinaryOpFunction("lt") { _, left, right ->
            when (val result = compareAny(left, right)) {
                null -> binOpTypeError("lt", left, right)
                else -> result < 0
            }
        }

        .defineBinaryOpFunction("le") { _, left, right ->
            when (val result = compareAny(left, right)) {
                null -> binOpTypeError("le", left, right)
                else -> result <= 0
            }
        }

        .defineBinaryOpFunction("or", condition = false) { _, right ->
            if (right !is Boolean) {
                binOpTypeError("or", false, right)
            }
            right
        }

        .defineBinaryOpFunction("and", condition = true) { _, right ->
            if (right !is Boolean) {
                binOpTypeError("and", true, right)
            }
            right
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
            when (subject) {
                is Int -> subject
                is Long -> subject
                is Number -> subject.toInt()
                is Boolean -> if (subject) 1 else 0
                is String -> subject.toLong()
                is CharSequence -> subject.toString().toLong()
                else -> throw IllegalArgumentException(
                    "invalid operand of type '${typeName(subject)}'"
                )
            }
        }

        .defineFilter("float") { _, subject, args ->
            args.requireEmpty()
            when (subject) {
                is Boolean -> if (subject) 1.0 else 0.0
                is Number -> subject.toDouble()
                is String -> subject.toDouble()
                is CharSequence -> subject.toString().toDouble()
                else -> throw IllegalArgumentException(
                    "invalid operand of type '${typeName(subject)}'"
                )
            }
        }

        .defineFilter("string") { _, subject, args ->
            args.requireEmpty()
            when (subject) {
                is String -> subject
                is CharSequence -> subject.toString()
                is Number -> subject.toString()
                is Boolean -> if (subject) "true" else "false"
                null -> "null"
                else -> throw IllegalArgumentException(
                    "invalid operand of type '${typeName(subject)}'"
                )
            }
        }

        .defineFilter("list") { _, subject, args ->
            val strings: CharSequence
            args.use {
                strings = optional("strings") { "codePoints" }
            }
            when (subject) {
                is CharSequence -> {
                    when (strings.toString()) {
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

                        else -> throw IllegalArgumentException(
                            "invalid option '$strings' for arg 'strings'"
                        )
                    }
                }

                is Map<*, *> -> subject.keys.toList()

                is Iterable<*> -> subject.toList()

                else -> throw IllegalArgumentException(
                    "invalid operand of type '${typeName(subject)}'"
                )
            }
        }

        .defineRescueMethod("default", CoreOperators.Filter.value) { _, _, args ->
            args.use { requireAny("default_value") }
        }

        .defineFilter("default") { _, subject, args ->
            args.use { requireAny("default_value") }
            subject
        }

        .defineFilter("first") { _, subject, args ->
            args.requireEmpty()
            when (subject) {
                is CharSequence -> subject.firstOrNull()
                is Iterable<*> -> subject.firstOrNull()
                else -> throw IllegalArgumentException(
                    "invalid operand of type '${typeName(subject)}'"
                )
            }
        }

        .defineFilter("last") { _, subject, args ->
            args.requireEmpty()
            when (subject) {
                is CharSequence -> subject.lastOrNull()
                is Iterable<*> -> subject.lastOrNull()
                else -> throw IllegalArgumentException(
                    "invalid operand of type '${typeName(subject)}'"
                )
            }
        }

        .defineFilter("keys") { _, subject, args ->
            args.requireEmpty()
            when (subject) {
                is Map<*, *> -> subject.keys.toList()
                is List<*> -> subject.indices.toList()
                else -> emptyList()
            }
        }

        .defineFilter("length") { _, subject, args ->
            args.requireEmpty()
            when (subject) {
                is Map<*, *> -> subject.size
                is Collection<*> -> subject.size
                is CharSequence -> subject.length
                else -> throw IllegalArgumentException(
                    "invalid operand of type '${typeName(subject)}'"
                )
            }
        }

        .defineFilter("select") { ctx, subject, args ->
            val test: CharSequence
            val filterArgs: NamedArgs
            args.use {
                test = require("test")
                filterArgs = optional(
                    "arg",
                    { NamedArgs(listOf(it), emptyList()) },
                    { NamedArgs(emptyList(), emptyList()) }
                )
            }
            val filter = ctx.getMethod(
                test.toString(),
                CoreOperators.Test.value
            ) ?: throw IllegalArgumentException(
                "invalid value for arg 'test': test '$test' not defined"
            )
            require(subject is Iterable<*>) {
                "invalid operand of type '${typeName(subject)}'"
            }
            listFilter(ctx, filter, filterArgs, subject)
        }

        .defineFilter("reject") { ctx, subject, args ->
            val test: CharSequence
            val filterArgs: NamedArgs
            args.use {
                test = require("test")
                filterArgs = optional(
                    "arg",
                    { NamedArgs(listOf(it), emptyList()) },
                    { NamedArgs(emptyList(), emptyList()) }
                )
            }
            val filter = ctx.getMethod(
                test.toString(),
                CoreOperators.TestNot.value
            ) ?: throw IllegalArgumentException(
                "invalid value for arg 'test': test '$test' not defined"
            )
            require(subject is Iterable<*>) {
                "invalid operand of type '${typeName(subject)}'"
            }
            listFilter(ctx, filter, filterArgs, subject)
        }

        .defineFilter("selectattr") { ctx, subject, args ->
            val attr: Any
            val test: CharSequence
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
                ?: error("filter 'get' not defined")
            val getArgs = NamedArgs(listOf(attr), emptyList())
            val filter = ctx.getMethod(
                test.toString(),
                CoreOperators.Test.value
            ) ?: throw IllegalArgumentException(
                "invalid value for arg 'test': test '$test' not defined"
            )
            require(subject is Iterable<*>) {
                "invalid operand of type '${typeName(subject)}'"
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
                else -> throw IllegalArgumentException(
                    "invalid operand of type '${typeName(subject)}'"
                )
            }
        }

        .defineFilter("join") { ctx, subject, args ->
            val separator: CharSequence
            args.use {
                separator = optional("separator") { ", " }
            }
            val toString = ctx.getMethod(
                "string",
                CoreOperators.Filter.value
            ) ?: error("filter 'string' not defined")
            when (subject) {
                is Iterable<*> -> subject.joinToString(separator) { v ->
                    val result = try {
                        toString.invoke(ctx, v, NamedArgs.Empty)
                    } catch (ex: Exception) {
                        throw RuntimeException(
                            "failed to convert list element " +
                                    "of type '${typeName(v)}' to string" +
                                    (ex.message?.let { ": $it" } ?: ""),
                            ex
                        )
                    }
                    if (result !is String) {
                        throw IllegalArgumentException(
                            "failed to convert list element " +
                                    "of type '${typeName(v)}' to string"
                        )
                    }
                    result
                }

                else -> throw IllegalArgumentException(
                    "invalid operand of type '${typeName(subject)}'"
                )
            }
        }

        .defineFilter("sort") { _, subject, args ->
            val reverse: Boolean
            val caseSensitive: Boolean
            args.use {
                reverse = optional("reverse") { false }
                caseSensitive = optional("case_sensitive") { false }
            }
            if (subject !is Iterable<*>) {
                throw IllegalArgumentException(
                    "invalid operand of type '${typeName(subject)}'"
                )
            }

            val elementIt = subject.iterator()
            if (!elementIt.hasNext()) {
                return@defineFilter subject
            }
            val firstElement = elementIt.next()
            if (!elementIt.hasNext()) {
                return@defineFilter subject
            }

            val comparator = when (firstElement) {
                is Number -> Comparator { a, b ->
                    if (a !is Number) {
                        throw IllegalArgumentException(
                            "non-comparable list elements " +
                                    "of type '${typeName(a)}' " +
                                    "and '${typeName(b)}'"
                        )
                    }
                    if (b !is Number) {
                        throw IllegalArgumentException(
                            "non-comparable list elements " +
                                    "of type '${typeName(a)}' " +
                                    "and '${typeName(b)}'"
                        )
                    }
                    compareNumbers(a, b)
                }
                is CharSequence -> Comparator<Any?> { a, b ->
                    val aStr = when (a) {
                        is String -> a
                        is CharSequence -> a.toString()

                        else -> throw IllegalArgumentException(
                            "non-comparable list elements " +
                                    "of type '${typeName(a)}' " +
                                    "and '${typeName(b)}'"
                        )
                    }
                    val bStr = when (b) {
                        is String -> b
                        is CharSequence -> b.toString()

                        else -> throw IllegalArgumentException(
                            "non-comparable list elements " +
                                    "of type '${typeName(a)}' " +
                                    "and '${typeName(b)}'"
                        )
                    }
                    aStr.compareTo(bStr, ignoreCase = !caseSensitive)
                }
                else -> throw IllegalArgumentException(
                    "non-comparable list element " +
                            "of type '${typeName(firstElement)}'"
                )
            }
            val finalComparator = if (reverse) {
                comparator.reversed()
            } else {
                comparator
            }

            subject.sortedWith(finalComparator)
        }

        .defineFilter("startswith") { _, subject, args ->
            val prefix: CharSequence
            args.use {
                prefix = require("prefix")
            }
            when (subject) {
                is CharSequence -> subject.startsWith(prefix)

                else -> throw IllegalArgumentException(
                    "invalid operand of type '${typeName(subject)}'"
                )
            }
        }

        .defineFilter("endswith") { _, subject, args ->
            val suffix: CharSequence
            args.use {
                suffix = require("suffix")
            }
            when (subject) {
                is CharSequence -> subject.endsWith(suffix)

                else -> throw IllegalArgumentException(
                    "invalid operand of type '${typeName(subject)}'"
                )
            }
        }

        .defineFilter("matches_glob") { _, subject, args ->
            val pattern: CharSequence
            val ignoreCase: Boolean
            args.use {
                pattern = require("pattern")
                ignoreCase = optional("ignore_case") { true }
            }
            val re = Glob(pattern, ignoreCase = ignoreCase).toRegex()
            when (subject) {
                is CharSequence -> re.matches(subject)
                else -> throw IllegalArgumentException(
                    "invalid operand of type '${typeName(subject)}'"
                )
            }
        }

        .defineFilter("matches_regex") { _, subject, args ->
            val pattern: CharSequence
            val ignoreCase: Boolean
            args.use {
                pattern = require("pattern")
                ignoreCase = optional("ignore_case") { true }
            }
            val re = if (ignoreCase) {
                Regex(pattern.toString(), RegexOption.IGNORE_CASE)
            } else {
                Regex(pattern.toString())
            }
            when (subject) {
                is CharSequence -> re.matches(subject)

                else -> throw IllegalArgumentException(
                    "invalid operand of type '${typeName(subject)}'"
                )
            }
        }

        .defineFilter("regex_replace") { _, subject, args ->
            val pattern: CharSequence
            val replacement: CharSequence
            val ignoreCase: Boolean
            args.use {
                pattern = require("pattern")
                replacement = require("replacement")
                ignoreCase = optional("ignore_case") { true }
            }
            val re = if (ignoreCase) {
                Regex(pattern.toString(), RegexOption.IGNORE_CASE)
            } else {
                Regex(pattern.toString())
            }
            when (subject) {
                is CharSequence -> subject.replace(re, replacement.toString())

                else -> throw IllegalArgumentException(
                    "invalid operand of type '${typeName(subject)}'"
                )
            }
        }

        .defineFilter("replace") { _, subject, args ->
            val search: CharSequence
            val replacement: CharSequence
            val ignoreCase: Boolean
            args.use {
                search = require("search")
                replacement = require("replacement")
                ignoreCase = optional("ignore_case") { false }
            }
            when (subject) {
                is String -> subject.replace(
                    search.toString(),
                    replacement.toString(),
                    ignoreCase = ignoreCase
                )

                is CharSequence -> subject.toString().replace(
                    search.toString(),
                    replacement.toString(),
                    ignoreCase = ignoreCase
                )

                else -> throw IllegalArgumentException(
                    "invalid operand of type'${typeName(subject)}'"
                )
            }
        }

        .defineBinaryOpFunction("in") { _, subject, listValue ->
            `in`(subject, listValue)
        }

        .defineBinaryOpFunction("not_in") { _, subject, listValue ->
            !`in`(subject, listValue)
        }

        .defineFilter("trim") { _, subject, args ->
            val chars: CharSequence
            args.use {
                chars = optional("chars") { "" }
            }
            when (subject) {
                is CharSequence -> if (chars.isEmpty()) {
                    subject.trim()
                } else {
                    subject.trim(*CharArray(chars.length) { i -> chars[i] })
                }

                else -> throw IllegalArgumentException(
                    "invalid operand of type '${typeName(subject)}'"
                )
            }
        }

        .defineFilter("lower") { _, subject, args ->
            args.requireEmpty()
            when (subject) {
                is String -> subject.lowercase()
                is CharSequence -> subject.toString().lowercase()

                else -> throw IllegalArgumentException(
                    "invalid operand of type '${typeName(subject)}'"
                )
            }
        }

        .defineFilter("upper") { _, subject, args ->
            args.requireEmpty()
            when (subject) {
                is String -> subject.uppercase()
                is CharSequence -> subject.toString().uppercase()

                else -> throw IllegalArgumentException(
                    "invalid operand of type '${typeName(subject)}'"
                )
            }
        }

        .defineFilter("base64decode") { _, subject, args ->
            args.requireEmpty()
            @OptIn(ExperimentalEncodingApi::class)
            when (subject) {
                is CharSequence -> Base64.decodeToByteString(subject)

                else -> throw IllegalArgumentException(
                    "invalid operand of type '${typeName(subject)}'"
                )
            }
        }

        .defineFilter("base64encode") { _, subject, args ->
            args.requireEmpty()
            @OptIn(ExperimentalEncodingApi::class)
            when (subject) {
                is ByteString -> Base64.UrlSafe.encode(subject)
                is ByteArray -> Base64.UrlSafe.encode(subject)
                is Iterable<*> -> {
                    val result = buildByteString {
                        for (element in subject) {
                            if (element !is Number) {
                                throw IllegalArgumentException(
                                    "non-numeric list element " +
                                            "of type '${typeName(element)}'"
                                )
                            }
                            append(element.toByte())
                        }
                    }
                    Base64.UrlSafe.encode(result)
                }

                else -> throw IllegalArgumentException(
                    "invalid operand of type '${typeName(subject)}'"
                )
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

        .defineRescueMethod(
            "defined",
            CoreOperators.Test.value
        ) { _, _, args ->
            args.requireEmpty()
            false
        }

        .defineRescueMethod(
            "defined",
            CoreOperators.TestNot.value
        ) { _, _, args ->
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

        .defineTest("number", ::isNumberTest, ::isNotNumberTest)
        .defineTest("string", ::isStringTest, ::isNotStringTest)
        .defineTest("iterable", ::isIterableTest, ::isNotIterableTest)

        .defineMethod("keys") { _, subject, args ->
            args.requireEmpty()
            when (subject) {
                is Map<*, *> -> subject.keys.toList()
                is List<*> -> (0 until subject.size).toList()

                else -> throw IllegalArgumentException(
                    "invalid operand of type '${typeName(subject)}'"
                )
            }
        }

        .build()

}
