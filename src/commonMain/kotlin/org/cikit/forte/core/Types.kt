package org.cikit.forte.core

import org.cikit.forte.parser.Expression
import org.cikit.forte.parser.Node
import org.cikit.forte.parser.ParsedTemplate
import kotlin.jvm.JvmInline

fun typeName(value: Any?): String {
    if (value == null) {
        return "null"
    }
    return value::class.simpleName ?: value::class.toString()
}

interface InlineString : CharSequence {
    fun appendTo(target: Appendable) =
        target.append(this.toString())

    fun appendTo(stringBuilder: StringBuilder) =
        stringBuilder.append(this.toString())
}

class RawString(
    val input: String,
    val startIndex: Int = 0,
    val endIndex: Int = input.length
) : InlineString {

    override val length: Int
        get() = endIndex - startIndex

    override fun appendTo(target: Appendable): Appendable {
        return target.append(input, startIndex, endIndex)
    }

    override fun appendTo(stringBuilder: StringBuilder): StringBuilder {
        return stringBuilder.append(input, startIndex, endIndex)
    }

    override fun get(index: Int): Char {
        val realIndex = startIndex + index
        if (realIndex in startIndex until endIndex) {
            return input[realIndex]
        }
        throw IllegalArgumentException(
            "index $index out of bounds: length = $length"
        )
    }

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        val realIndex = this.startIndex + startIndex
        if (realIndex !in this.startIndex until this.endIndex) {
            throw IllegalArgumentException(
                "index $startIndex out of bounds: length = $length"
            )
        }
        val realEndIndex = this.startIndex + endIndex
        if (realEndIndex !in this.startIndex until this.endIndex) {
            throw IllegalArgumentException(
                "index $endIndex out of bounds: length = $length"
            )
        }
        return input.subSequence(realIndex, realEndIndex)
    }

    override fun equals(other: Any?): Boolean {
        if (other is RawString) {
            return toString() == other.toString()
        }
        return toString() == other
    }

    override fun hashCode(): Int {
        return toString().hashCode()
    }

    override fun toString(): String {
        return input.substring(startIndex, endIndex)
    }
}

fun CharSequence.concatToString() = this as? String
    ?: (this as? InlineString)?.toString()
    ?: buildString(length) { append(this@concatToString) }

interface ComparableValue : Comparable<ComparableValue> {
    val value: Any?
}

interface NumericValue {
    val result: Number

    val isInt: Boolean
    val isFloat: Boolean
    val hasDecimalPart: Boolean

    val maxBitLength: Int
        get() = 8192

    fun plus(other: NumericValue): NumericValue
    fun minus(other: NumericValue): NumericValue
    fun mul(other: NumericValue): NumericValue
    fun div(other: NumericValue): NumericValue
    fun tdiv(other: NumericValue): NumericValue
    fun rem(other: NumericValue): NumericValue
    fun pow(other: NumericValue): NumericValue
    fun negate(): NumericValue

    fun toComparableValue(originalValue: Any?): ComparableValue
    fun toIntValue(): NumericValue
    fun toFloatValue(): NumericValue
    fun toStringValue(): CharSequence

    fun intOrNull(): Int?
    fun longOrNull(): Long?
    fun doubleOrNull(): Double?
}

class Branch(
    val name: String,
    val args: Map<String, Expression>,
    val body: List<Node>
)

interface CommandTag {
    val isHidden: Boolean
        get() = false

    suspend operator fun invoke(
        ctx: Context.Builder<*>,
        template: ParsedTemplate,
        args: Map<String, Expression>
    )
}

interface ControlTag {
    val isHidden: Boolean
        get() = false

    suspend operator fun invoke(
        ctx: Context.Builder<*>,
        template: ParsedTemplate,
        branches: List<Branch>
    )
}

interface DependencyAware {
    fun withDependencies(ctx: Context<*>): DependencyAware = this
}

interface UnOpFunction {
    val isHidden: Boolean
        get() = false

    operator fun invoke(arg: Any?): Any?
}

interface BinOpFunction {
    val isHidden: Boolean
        get() = false

    val isRescue: Boolean
        get() = false

    operator fun invoke(left: Any?, right: Any?): Any?
}

sealed class ConditionalResult {
    data object Continue : ConditionalResult()
    class Return(val value: Any?) : ConditionalResult()
}

interface ConditionalBinOpFunction : BinOpFunction {
    override val isHidden: Boolean
        get() = false

    fun checkCondition(left: Any?): ConditionalResult
}

interface Function {
    val isHidden: Boolean
        get() = false

    operator fun invoke(args: NamedArgs): Any?
}

@JvmInline
value class MethodOperator<T: Method>(val value: String)

interface Method {
    companion object {
        val OPERATOR = MethodOperator<Method>("invoke")
    }

    val operator: String
        get() = OPERATOR.value

    val isRescue: Boolean
        get() = false

    val isHidden: Boolean
        get() = false

    operator fun invoke(subject: Any?, args: NamedArgs): Any?
}

open class Undefined(open val message: String) {
    override fun toString(): String {
        return "Undefined(message='$message')"
    }
}

class Suspended(
    val eval: suspend (Context.Evaluator<*>) -> Any?
) : Undefined("evaluation has been suspended")

class MaskedList(val list: List<Any?>) : Iterable<Any?> {
    override fun iterator(): Iterator<Any?> {
        return list.iterator()
    }
}
