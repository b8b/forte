package org.cikit.forte

import okio.ByteString.Companion.toByteString
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1

private const val rt = "rt"
private const val callPrefix = "$rt.call_"
private const val callCmdPrefix = "$rt.cmd_"
private const val callControlPrefix = "$rt.control_"
private const val applyPrefix = "$rt.apply_"

private class CoreFunction<T>(val name: String) {
    constructor(func: T) : this(
        when (func) {
            is KFunction<*> -> func.name
            is KProperty1<*, *> -> func.name
            else -> error("unsupported function type: $func")
        }
    )

    val qName: String get() = "$rt.$name"
}

private object RT {
    val context = CoreFunction(Runtime::context)
    val emit = CoreFunction(Runtime::emit)
    val get = CoreFunction(Runtime::get)

    val makeString = CoreFunction(Runtime::makeString)
    val makeList = CoreFunction(Runtime::makeList)
    val makeMap = CoreFunction(Runtime::makeMap)
    val makeCommand = CoreFunction(Runtime::makeCommand)
}

private class Location(val line: Int, val col: Int)
private class Mapping(val source: Location, val generated: Location)

private typealias SourceMap = List<Mapping>

private val encodeVlqChars =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

private fun Appendable.encodeVlq(value: Int) {
    var valueToEncode = if (value < 0) {
        ((0 - value) shl 1) or 1
    } else {
        value shl 1
    }
    while (true) {
        var next5Bit = valueToEncode and 0x1F
        valueToEncode = valueToEncode shr 5
        if (valueToEncode != 0) {
            // add continuation bit
            next5Bit = next5Bit or 0x20
            append(encodeVlqChars[next5Bit])
        } else {
            append(encodeVlqChars[next5Bit])
            break
        }
    }
}

private fun SourceMap.encodeToString(): String = buildString {
    val entriesSorted = this@encodeToString.toMutableList()

    entriesSorted.sortWith { a, b ->
        var result = a.generated.line.compareTo(b.generated.line)
        if (result == 0) result = a.generated.col.compareTo(b.generated.col)
        result
    }

    var lastLine = 1
    var lastCol = 1

    var lastSourceLine = 1
    var lastSourceCol = 1

    var appendComma = false

    for (entry in entriesSorted) {
        while (lastLine < entry.generated.line) {
            append(";")
            lastLine++
            lastCol = 0
            appendComma = false
        }

        if (appendComma) {
            append(",")
        }

        // generated col
        require(lastCol <= entry.generated.col)
        encodeVlq(entry.generated.col - lastCol)
        lastCol = entry.generated.col

        // index into list of sources
        encodeVlq(0)

        // source line
        require(lastSourceLine <= entry.source.line)
        encodeVlq(entry.source.line - lastSourceLine)
        lastSourceLine = entry.source.line

        // source col
        encodeVlq(entry.source.col - lastSourceCol)
        lastSourceCol = entry.source.col

        appendComma = true
    }
}

private fun SourceMap.toJson(input: String) = JsonEmitter.encodeToString {
    emitObject {
        emitScalar("version")
        emitScalar(3)
        emitScalar("sources")
        emitArray {
            emitScalar("template")
        }
        emitScalar("sourcesContent")
        emitArray {
            emitScalar(input)
        }
        emitScalar("names")
        emitArray {
        }
        emitScalar("mappings")
        emitScalar(encodeToString())
    }
}

private class JsTranspiler(
    val input: String,
    val target: Appendable,
    val indent: String = ""
) {

    private val sourceMap = mutableListOf<Mapping>()
    private val sourceLines = input.split('\n').map { line ->
        line.length + 1
    }
    private var writingLine = 2
    private var writingCol = 1

    fun sourceMap(): SourceMap {
        return sourceMap.toList()
    }

    private fun appendMapping(sourcePos: Int) {
        var pos0 = 0
        sourceLines.forEachIndexed { index, length ->
            if (sourcePos < pos0 + length) {
                val sourceLocation = Location(index + 1, sourcePos - pos0 + 1)
                val generatedLocation = Location(writingLine, writingCol)
                sourceMap += Mapping(sourceLocation, generatedLocation)
                return
            }
            pos0 += length
        }
        error("sourcePos $sourcePos not found in input")
    }

    private fun appendLine() {
        target.appendLine()
        writingLine++
        writingCol = 1
    }

    private fun escape(input: String): String {
        return input
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
    }

    fun transpileCommand(cmd: Node, indent: String = this.indent) {
        when (cmd) {
            is Node.Comment -> {
                val commentStr =
                    input.substring(cmd.content.first..cmd.content.last)
                        .replaceAfter("\n", "...")
                        .replace("\n", "")
                appendMapping(cmd.first.first)
                target.append(indent).append("//")
                target.append(escape(commentStr))
                appendLine()
            }

            is Node.Command -> {
                appendMapping(cmd.first.first)
                target.append(indent)
                    .append(callCmdPrefix)
                    .append(cmd.name)
                    .append("(")
                renderNamedArgs(cmd.content)
                target.append(");")
                appendLine()
            }

            is Node.Control -> {
                target.append(indent)
                    .append(callControlPrefix)
                    .append(cmd.first.first.name)
                    .append("({branches: ")
                    .append(RT.makeList.qName)
                    .append("([")
                appendLine()
                for (branch in listOf(cmd.first) + cmd.branches) {
                    appendMapping(branch.first.first.first)
                    target.append(indent).append("    ")
                        .append(RT.makeCommand.qName)
                        .append("({")
                    appendLine()

                    target.append(indent).append("        ")
                        .append("command: \"")
                        .append(branch.first.name)
                        .append("\",")
                    appendLine()

                    target.append(indent).append("        ")
                        .append("args: () => ")
                    renderNamedArgs(branch.first.content)

                    if (branch.body.isNotEmpty()) {
                        target.append(",")
                        appendLine()
                        target.append(indent).append("        ")
                            .append("body: () => {")
                        appendLine()
                        branch.body.forEach {
                            transpileCommand(it, indent = "$indent            ")
                        }
                        target.append(indent).append("        ")
                            .append("}")
                    }

                    appendLine()
                    target.append(indent).append("    ").append("}),")
                    appendLine()
                }
                target.append(indent).append("])});")
                appendLine()
            }

            is Node.Text -> {
                appendMapping(cmd.content.first)
                val text = input.substring(
                    cmd.content.first..cmd.content.last
                )
                val trimmed = when {
                    cmd.trimLeft && cmd.trimRight -> text.trim()
                    cmd.trimLeft -> text.trimStart()
                    cmd.trimRight -> text.trimEnd()
                    else -> text
                }
                if (trimmed.isNotEmpty()) {
                    target.append(indent)
                        .append(RT.emit.qName)
                        .append("(\"")
                        .append(escape(text))
                        .append("\");")
                    appendLine()
                }
            }

            is Node.Emit -> {
                appendMapping(cmd.first.first)
                target.append(indent)
                    .append(RT.emit.qName)
                    .append("(")
                transpile(cmd.content)
                target.append(");")
                appendLine()
            }

            else -> error("unexpected node: $cmd")
        }
    }

    private fun transpile(n: Node.Expression) {
        when (n) {
            is Node.SubExpression -> {
                target.append("(")
                transpile(n.content)
                target.append(")")
            }
            is Node.Empty -> {
                target.append("()")
            }
            is Node.Malformed -> {
                error("malformed expression: $n")
            }
            is Node.Variable -> {
                renderAccess(n)
            }
            is Node.NullLiteral -> {
                target.append("null")
            }
            is Node.BooleanLiteral -> {
                target.append(n.value.toString())
            }
            is Node.NumericLiteral -> {
                target.append(n.value.toString())
            }
            is Node.StringLiteral -> {
                target
                    .append(RT.makeString.qName)
                    .append("([\"")
                    .append(escape(n.value))
                    .append("\"])")
            }
            is Node.StringInterpolation -> {
                target
                    .append(RT.makeString.qName)
                    .append("([")
                n.children.forEachIndexed { index, item ->
                    if (index > 0) {
                        target.append(", ")
                    }
                    transpile(item)
                }
                target
                    .append("])")
            }
            is Node.ArrayLiteral -> {
                target
                    .append(RT.makeList.qName)
                    .append("([")
                n.children.forEachIndexed { index, item ->
                    if (index > 0) {
                        target.append(", ")
                    }
                    target.append("(")
                    transpile(item)
                    target.append(")")
                }
                target
                    .append("])")
            }
            is Node.ObjectLiteral -> {
                target
                    .append(RT.makeMap.qName)
                    .append("([")
                n.pairs.forEachIndexed { index, (k, v) ->
                    if (index > 0) {
                        target.append(", ")
                    }
                    when (k) {
                        is Node.Variable -> target
                            .append("\"")
                            .append(k.name)
                            .append("\"")
                        is Node.StringLiteral -> target
                            .append("\"")
                            .append(escape(k.value))
                            .append("\"")
                        else -> transpile(k)
                    }
                    target.append(", (")
                    transpile(v)
                    target.append(")")
                }
                target.append("])")
            }
            is Node.CompAccess -> {
                renderAccess(n)
            }
            is Node.Access -> {
                renderAccess(n)
            }
            is Node.FunctionCall -> {
                target.append(callPrefix).append(n.name)
                renderNamedArgs(n.args)
            }
            is Node.UnOp -> {
                when (n.decl.name) {
                    else -> {
                        target.append(callPrefix).append(n.decl.name)
                        renderNamedArgs(listOf("0" to n.right))
                    }
                }
            }
            is Node.ExtensionCall -> {
                target
                    .append(applyPrefix)
                    .append(n.name)
                    .append("(")
                transpile(n.left)
                target.append(", () => (")
                renderNamedArgs(n.args)
                target.append("))")
            }
            is Node.MethodCall -> {
                target
                    .append(applyPrefix)
                    .append("invoke(")
                transpile(n.left)
                target.append(", () => (")
                renderNamedArgs(n.args)
                target.append("))")
            }
            is Node.BinOp -> {
                when (n.decl.name) {
                    "pipe" -> {
                        when (n.right) {
                            is Node.Variable -> {
                                target
                                    .append(applyPrefix)
                                    .append(n.right.name)
                                    .append("(")
                                transpile(n.left)
                                target
                                    .append(", () => ({}))")
                            }

                            is Node.FunctionCall -> {
                                target
                                    .append(applyPrefix)
                                    .append(n.right.name)
                                    .append("(")
                                transpile(n.left)
                                target
                                    .append(", () => (")
                                renderNamedArgs(n.right.args)
                                target
                                    .append("))")
                            }

                            else -> error("invalid operand for ${n.decl.name}: ${n.right}")
                        }
                    }
                    "is", "is_not" -> {
                        when (n.right) {
                            is Node.Variable -> {
                                target
                                    .append(applyPrefix)
                                    .append(n.decl.name).append("_")
                                    .append(n.right.name).append("(")
                                transpile(n.left)
                                target.append(", () => ({}))")
                            }

                            is Node.FunctionCall -> {
                                target.append(applyPrefix)
                                    .append(n.decl.name).append("_")
                                    .append(n.right.name).append("(")
                                transpile(n.left)
                                target.append(", () => (")
                                renderNamedArgs(n.right.args)
                                target.append("))")
                            }

                            else -> error("invalid operand for ${n.decl.name}: ${n.right}")
                        }
                    }
                    "assign" -> when (n.left) {
                        is Node.Variable -> {
                            target
                                .append(applyPrefix)
                                .append("assign")
                                .append("(")
                            target.append("\"${n.left.name}\", () => (")
                            renderNamedArgs(listOf("0" to n.right))
                            target
                                .append("))")
                        }
                        else -> error("expected name: ${n.left}")
                    }
                    else -> {
                        target
                            .append(applyPrefix)
                            .append(n.decl.name)
                            .append("(")
                        transpile(n.left)
                        target.append(", () => (")
                        renderNamedArgs(listOf("0" to n.right))
                        target.append("))")
                    }
                }
            }
        }
    }

    private fun renderNamedArgs(args: List<Pair<String, Node.Expression>>) {
        val namedArgs = mutableSetOf<String>()
        target.append("({")
        args.forEachIndexed { index, (name, item) ->
            if (index > 0) {
                target.append(", ")
            }
            require(name !in namedArgs)
            namedArgs += name
            target.append(name).append(": ")
            transpile(item)
        }
        target.append("})")
    }

    private fun renderAccess(node: Node.Expression) {
        val keys = mutableListOf<() -> Unit>()
        var n = node
        while (true) {
            n = when (n) {
                is Node.Access -> {
                    val finalNode = n
                    keys.add {
                        target
                            .append("\"")
                            .append(finalNode.name)
                            .append("\"")
                    }
                    n.left
                }
                is Node.CompAccess -> {
                    val finalNode = n.right
                    keys.add {
                        when (finalNode) {
                            is Node.StringLiteral,
                            is Node.StringInterpolation -> {
                                transpile(finalNode)
                            }
                            else -> {
                                target.append("(")
                                transpile(finalNode)
                                target.append(")")
                            }
                        }
                    }
                    n.left
                }
                else -> break
            }
        }


        // n - root node of access chain
        // keys - access operation renderers

        if (n is Node.Variable) {
            target.append(RT.get.qName).append("(")
                .append(RT.context.qName)
                .append(", [\"").append(n.name).append("\"")
        } else {
            target.append(RT.get.qName).append("((")
            transpile(n)
            target.append("), [")
        }

        for (i in keys.indices.reversed()) {
            target.append(", ")
            keys[i]()
        }
        target.append("])")
    }
}

fun ParsedTemplate.transpileToJs(embeddedSourceMap: Boolean = false): String {
    val target = StringBuilder()
    target.appendLine("function template_exec(rt) {")
    val transpiler = JsTranspiler(input, target, "    ")
    for (cmd in nodes) {
        transpiler.transpileCommand(cmd)
    }
    target.appendLine()
    target.appendLine("    return 0;")
    target.appendLine("}")
    target.appendLine()
    target.appendLine("module.exports = { exec: template_exec };")
    if (embeddedSourceMap) {
        target.appendLine()
        target.append("//# sourceMappingURL=data:application/json;base64,")
        target.append(
            transpiler.sourceMap()
                .toJson(input)
                .encodeToByteArray()
                .toByteString()
                .base64()
        )
    }
    return target.toString()
}
