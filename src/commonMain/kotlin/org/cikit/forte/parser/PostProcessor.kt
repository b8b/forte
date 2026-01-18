package org.cikit.forte.parser

interface PostProcessor {
    fun transform(template: ParsedTemplate): ParsedTemplate = template
    fun transform(control: Node.Control): Node.Control = control
}
