package org.cikit.forte.parser

import okio.Path

class ParsedTemplate(
    val input: String,
    val path: Path?,
    val nodes: List<Node>
)