import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.io.bytestring.ByteStringBuilder
import kotlinx.io.bytestring.decodeToString
import kotlinx.serialization.json.*
import org.cikit.forte.Forte
import org.cikit.forte.core.Context
import org.cikit.forte.emitter.YamlEmitter
import org.cikit.forte.eval.evalTemplate
import org.cikit.forte.parser.Declarations
import kotlin.wasm.unsafe.Pointer
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi
import kotlin.wasm.unsafe.withScopedMemoryAllocator

@WasmImport("wasi_snapshot_preview1", "args_sizes_get")
external fun wasiArgsSizesGet(argcPtr: UInt, argvBufSizePtr: UInt): Int

@WasmImport("wasi_snapshot_preview1", "args_get")
external fun wasiArgsGet(argv: UInt, argvBuf: UInt): Int

val forte = Forte {
    declarations += Declarations.Command("load_json", setOf("endload"))
    context.defineControlTag("load_json") { ctx, branches ->
        val tag = branches.single()
        val jsonText = ctx.scope(Context.StringResultBuilder())
            .evalTemplate(tag.body)
            .result
        val data = Json.decodeFromString<JsonObject>(jsonText)
        for ((k, v) in data) {
            ctx.setVar(k, v.toAny())
        }
    }
}

@OptIn(UnsafeWasmMemoryApi::class)
fun main() {
    val args = mutableListOf<String>()
    withScopedMemoryAllocator { allocator ->
        val argcPtr = allocator.allocate(UInt.SIZE_BYTES)
        val argvBufSizePtr = allocator.allocate(UInt.SIZE_BYTES)
        val ret1 = wasiArgsSizesGet(argcPtr.address, argvBufSizePtr.address)
        if (ret1 != 0) {
            error("args_sizes_get failed with error code $ret1")
        }
        val argc = argcPtr.loadInt()
        val argvPtr = allocator.allocate(argc * UInt.SIZE_BYTES)
        val argvBufPtr = allocator.allocate(argvBufSizePtr.loadInt())
        val ret2 = wasiArgsGet(argvPtr.address, argvBufPtr.address)
        if (ret2 != 0) {
            error("args_get failed with error code $ret2")
        }
        for (i in 0 until argc) {
            val argPtrAddress = (argvPtr + i * UInt.SIZE_BYTES).loadInt()
            val argPtr = Pointer(argPtrAddress.toUInt())

            val builder = ByteStringBuilder()
            while (true) {
                val b = (argPtr + builder.size).loadByte()
                if (b == 0.toByte()) {
                    break
                }
                builder.append(b)
            }
            args += builder.toByteString().decodeToString()
        }
    }
    try {
        MainScope().launch {
            val forteScope = forte.captureToList()
            for (arg in args) {
                val parsedTemplate = forte.parseTemplate(arg)
                forteScope.evalTemplate(parsedTemplate)
            }
            val output = YamlEmitter.encodeToString {
                emitStartArray()
                for (result in forteScope.result) {
                    emit(result)
                }
                emitEndArray()
            }
            println(output)
        }
    } catch (ex: Exception) {
        println("failed: $ex")
        ex.printStackTrace()
        throw ex
    }
}

private fun JsonElement.toAny(): Any? = when (this) {
    is JsonNull -> null
    is JsonObject -> entries.associate { (k, v) -> k to v.toAny() }
    is JsonArray -> map { v -> v.toAny() }
    is JsonPrimitive -> {
        booleanOrNull
            ?: intOrNull ?: longOrNull
            ?: floatOrNull ?: doubleOrNull
            ?: content
    }
}
