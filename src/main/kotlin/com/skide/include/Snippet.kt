package com.skide.include

import java.util.*

class SnippetRule(val allowedTypes: Vector<NodeType>, var startsWith: Pair<Boolean, String>, var contains: Pair<Boolean, String>, var endsWith: Pair<Boolean, String>)

class Snippet(val id: Long, var name: String, var label: String, var insertText: String, var rootRule: SnippetRule, var parentRule: SnippetRule, var triggerReplaceSequence: Boolean) {
    override fun toString(): String {
        return name
    }
}

fun Array<NodeType>.toVector(): Vector<NodeType> {

    val vector = Vector<NodeType>()

    for (nodeType in this) {
        vector.add(nodeType)
    }
    return vector
}