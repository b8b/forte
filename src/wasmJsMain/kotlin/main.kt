import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.dom.appendElement
import kotlinx.dom.appendText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.cikit.forte.Forte
import org.cikit.forte.core.Branch
import org.cikit.forte.core.Context
import org.cikit.forte.core.ControlTag
import org.cikit.forte.core.loadJson
import org.cikit.forte.lib.funit.defineFUnitExtensions
import org.cikit.forte.lib.funit.fUnitDeclarations
import org.cikit.forte.parser.Declarations
import org.cikit.forte.parser.Node
import org.cikit.forte.parser.ParsedTemplate
import org.cikit.forte.parser.sourceTokenRange
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLPreElement
import org.w3c.dom.HTMLTextAreaElement

val scope = MainScope()

val forte = Forte {
    declarations += Declarations.Command("load_json", setOf("endload"))
    declarations.addAll(fUnitDeclarations)
    context.defineControlTag(
        "load_json",
        object : ControlTag {
            override suspend fun invoke(
                ctx: Context.Builder<*>,
                template: ParsedTemplate,
                branches: List<Branch>
            ) {
                val tag = branches.single()
                val jsonText = ctx.scope()
                    .renderToString()
                    .evalNodes(template, tag.body)
                    .result
                val data = Json.decodeFromString<JsonObject>(jsonText)
                for ((k, v) in data) {
                    ctx.loadJson(k, v)
                }
            }
        }
    )
    context.defineFUnitExtensions()
}

fun main() {
    val template = document.getElementById("template") as HTMLTextAreaElement
    val button = document.getElementById("eval") as HTMLButtonElement
    val autorender = document.getElementById("autorender") as HTMLInputElement
    val echo = document.getElementById("echo") as HTMLInputElement
    template.oninput = {
        scope.launch {
            render(template, echo.checked)
        }
    }
    template.onchange = {
        scope.launch {
            render(template, echo.checked)
        }
    }
    button.onclick = {
        scope.launch {
            render(template, echo.checked)
        }
    }
    scope.launch {
        render(template, echo.checked)
    }
    autorender.onchange = {
        if (autorender.checked) {
            template.oninput = {
                scope.launch {
                    render(template, echo.checked)
                }
            }
            template.onchange = {
                scope.launch {
                    render(template, echo.checked)
                }
            }
        } else {
            template.oninput = null
            template.onchange = null
        }
    }
    echo.onchange = {
        if (autorender.checked) {
            scope.launch {
                render(template, echo.checked)
            }
        }
    }
}

private suspend fun render(
    template: HTMLTextAreaElement,
    echo: Boolean
) {
    val root = document.getElementById("root") as HTMLDivElement
    val templateText = template.value
    val template = try {
        forte.parseTemplate(templateText)
    } catch (ex: Exception) {
        root.innerHTML = "<pre>$ex</pre>"
        return
    }
    if (!echo) {
        try {
            val result = forte.renderToString()
                .evalTemplate(template)
                .result
            root.innerHTML = formatMarkdown(result)
        } catch (ex: Exception) {
            root.innerHTML = "<pre>$ex</pre>"
        }
        return
    }
    root.innerHTML = ""
    var ctx = forte.scope()
    for (node in template.nodes) {
        if (node is Node.Text) {
            val output = ctx.renderToString()
                .evalNodes(template, listOf(node))
                .result
            root.appendElement("p") {
                innerHTML = formatMarkdown(output)
            }
        } else {
            val tokenRange = node.sourceTokenRange()
            val nodeSource = templateText.substring(
                tokenRange.first.first .. tokenRange.second.last
            )
            val sourceElement: HTMLPreElement
            root.appendElement("small") {
                sourceElement = appendElement("pre") {
                    appendText(nodeSource)
                } as HTMLPreElement
            }
            val backup = ctx.scope()
            try {
                val output = ctx.renderToString()
                    .evalNodes(template, listOf(node))
                    .result
                sourceElement.appendText(" \u2705")
                root.appendElement("p") {
                    innerHTML = formatMarkdown(output)
                }
            } catch (ex: Exception) {
                // reset context
                ctx = backup
                sourceElement.appendText(" \u274C")
                root.appendElement("div") {
                    this.setAttribute("style", "margin-left: 40px;")
                    appendElement("small") {
                        appendText("${ex.message}")
                    }
                }
            }
        }
    }
}

private fun formatMarkdown(input: String): String {
    val flavor = CommonMarkFlavourDescriptor()
    val parsedTree = MarkdownParser(flavor)
        .buildMarkdownTreeFromString(input)
    val html = HtmlGenerator(input, parsedTree, flavor, true)
        .generateHtml()
    return html
}
