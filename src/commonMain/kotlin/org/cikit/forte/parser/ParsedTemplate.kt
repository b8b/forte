package org.cikit.forte.parser

import org.cikit.forte.core.UPath

class ParsedTemplate(
    val input: String,
    val path: UPath?,
    val nodes: List<Node>
)