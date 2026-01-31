package org.cikit.forte.lib.core

import org.cikit.forte.core.Context
import org.cikit.forte.core.Function
import org.cikit.forte.core.TestMethod
import org.cikit.forte.core.typeName

val Context<*>.filterNumber: FilterNumber
    get() = getMethod(FilterNumber.KEY)
        ?: error("${FilterNumber.KEY} is not defined")

val Context<*>.filterComparable: FilterComparable
    get() = getMethod(FilterComparable.KEY)
        ?: error("${FilterComparable.KEY} is not defined")

val Context<*>.testIn: IsInTest
    get() = getMethod(IsInTest.KEY)
        ?: error("${IsInTest.KEY} is not defined")

val Context<*>.testIterable: TestMethod
    get() = getMethod(IsIterableTest.KEY)
        ?: error("${IsIterableTest.KEY} is not defined")

val Context<*>.testDefined: TestMethod
    get() = getMethod(IsDefinedTest.KEY)
        ?: error("${IsDefinedTest.KEY} is not defined")

/**
 * core extensions
 *
 * Register the core language functions.
 *
 * These functions are mainly concerned with handling basic types.
 * To introduce support for additional data types in the host language,
 * these implementations have to be extended.
 */
fun <R> Context.Builder<R>.defineCoreExtensions(): Context.Builder<R> {
    defineControlTag("if", ControlIf())
    defineControlTag("for", ControlFor())
    defineControlTag("macro", ControlMacro())
    defineControlTag("filter", ControlFilter())
    defineControlTag("raw", ControlRaw())
    defineControlTag("set", ControlSet())
    defineCommandTag("set", CommandSet())

    defineCommandTag("include", CommandInclude())
    defineCommandTag("from", CommandFrom())
    defineCommandTag("import", CommandImport())
    defineControlTag("extends", ControlExtends())
    defineControlTag(ControlBlock.KEY, ControlBlock())

    defineMethod(FilterDefault.KEY, FilterDefault())
    defineMethod(FilterGet.KEY, FilterGet.Hidden)
    defineMethod(FilterSlice.KEY, FilterSlice())
    defineMethod(FilterComparable.KEY, FilterComparable())
    defineMethod(FilterNumber.KEY, FilterNumber())

    defineMethod(IsInTest.KEY, IsInTest(this))

    defineFunction("range", RangeFunction() as Function)

    defineOpFunction("not", UnaryNot())

    defineBinaryOpFunction("eq", BinaryEq(this))
    defineBinaryOpFunction("ne", BinaryNe(this))

    defineBinaryOpFunction("ge", BinaryGe(this))
    defineBinaryOpFunction("gt", BinaryGt(this))
    defineBinaryOpFunction("le", BinaryLe(this))
    defineBinaryOpFunction("lt", BinaryLt(this))

    defineBinaryOpFunction("and", BinaryAnd())
    defineBinaryOpFunction("or", BinaryOr())

    defineBinaryOpFunction("plus", BinaryPlus(this))
    defineBinaryOpFunction("minus", BinaryMinus(this))
    defineBinaryOpFunction("mul", BinaryMul(this))
    defineBinaryOpFunction("div", BinaryDiv(this))
    defineBinaryOpFunction("tdiv", BinaryTDiv(this))
    defineBinaryOpFunction("rem", BinaryRem(this))
    defineBinaryOpFunction("pow", BinaryPow(this))

    defineBinaryOpFunction("in", BinaryIn(this))

    defineBinaryOpFunction("if", BinaryIf())
    defineBinaryOpFunction("else", BinaryElse())

    val filterLength = FilterLength()
    defineMethod("length", filterLength)
    defineMethod("count", filterLength)

    defineMethod("list", FilterList())

    defineMethod("int", FilterInt(this))
    defineMethod("float", FilterFloat(this))
    defineMethod("string", FilterString())

    defineMethod("invoke", ApplyInvoke())

    defineMethod("boolean", IsBooleanTest())
    defineMethod("true", IsTrueTest())
    defineMethod("false", IsFalseTest())
    defineMethod("number", IsNumberTest(this))
    defineMethod("integer", IsIntegerTest(this))
    defineMethod("float", IsFloatTest(this))
    defineMethod("string", IsStringTest())
    defineMethod("iterable", IsIterableTest())
    defineMethod("mapping", IsMappingTest())

    defineBinaryOpFunction("concat", BinaryConcat(this))

    defineMethod(IsDefinedTest.KEY, IsDefinedTest())

    defineMethod("sameas", IsSameAsTest())

    val isEqTest = IsEqTest(this)
    defineMethod("eq", isEqTest)
    defineMethod("equalto", isEqTest)
    defineMethod("==", isEqTest)

    val isNeTest = IsNeTest(this)
    defineMethod("ne", isNeTest)
    defineMethod("!=", isNeTest)

    val isGtTest = IsGtTest(this)
    defineMethod("gt", isGtTest)
    defineMethod("greaterthan", isGtTest)
    defineMethod(">", isGtTest)

    val isGeTest = IsGeTest(this)
    defineMethod("ge", isGeTest)
    defineMethod(">=", isGeTest)

    val isLtTest = IsLtTest(this)
    defineMethod("lt", isLtTest)
    defineMethod("lessthan", isLtTest)
    defineMethod("<", isLtTest)

    val isLeTest = IsLeTest(this)
    defineMethod("le", isLeTest)
    defineMethod("<=", isLeTest)

    defineMethod("callable", IsCallableTest())
    defineMethod("filter", IsFilterTest())
    defineMethod("test", IsTestTest())

    return this
}

fun unpackList(
    varNames: List<*>,
    value: Any?
): Array<Pair<String, Any?>> {
    varNames.singleOrNull()?.let { varName ->
        varName as String
        return arrayOf(varName to value)
    }
    val valuesIt = (value as Iterable<*>).iterator()
    val result = Array(varNames.size) { i ->
        val varName = varNames[i] as String
        if (!valuesIt.hasNext()) {
            error(
                "not enough items in list value: $i, " +
                        "expected ${varNames.size}"
            )
        }
        varName to valuesIt.next()
    }
    require(!valuesIt.hasNext()) {
        "more than ${varNames.size} items in list value"
    }
    return result
}

fun binOpTypeError(
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
