import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import org.cikit.forte.Forte
import org.cikit.forte.eval.evalTemplate
import org.cikit.forte.parser.Declarations
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement

val scope = MainScope()

val forte = Forte {
    declarations += Declarations.Command("load_json", setOf("endload"))
    context.defineControlTag("load_json") { ctx, branches ->
        val tag = branches.single()
        val jsonText = ctx.scope()
            .captureToString()
            .evalTemplate(tag.body)
            .result
        val data = Json.decodeFromString<JsonObject>(jsonText)
        for ((k, v) in data) {
            ctx.setVar(k, v.toAny())
        }
    }
}

fun main() {
    val template = document.getElementById("template") as HTMLTextAreaElement
    val button = document.getElementById("eval") as HTMLButtonElement
    val autorender = document.getElementById("autorender") as HTMLInputElement
    template.oninput = {
        scope.launch {
            render(template)
        }
    }
    template.onchange = {
        scope.launch {
            render(template)
        }
    }
    button.onclick = {
        scope.launch {
            render(template)
        }
    }
    scope.launch {
        render(template)
    }
    autorender.onchange = {
        if (autorender.checked) {
            template.oninput = {
                scope.launch {
                    render(template)
                }
            }
            template.onchange = {
                scope.launch {
                    render(template)
                }
            }
        } else {
            template.oninput = null
            template.onchange = null
        }
    }
}

private suspend fun render(template: HTMLTextAreaElement) {
    val root = document.getElementById("root") as HTMLDivElement
    try {
        val templateText = template.value
        val parsedTemplate = forte.parseTemplate(templateText)
        root.innerHTML = forte.captureToString()
            .evalTemplate(parsedTemplate)
            .result
    } catch (ex: Exception) {
        root.innerHTML = "<pre>$ex</pre>"
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
