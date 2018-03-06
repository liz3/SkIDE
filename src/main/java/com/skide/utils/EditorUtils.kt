package com.skide.utils

import com.skide.include.Node
import org.fxmisc.richtext.CodeArea
import java.util.*

object EditorUtils {

    fun getLineNode(line: Int, all: Vector<Node>): Node? {

        return all
                .map { searchNode(line, it) }
                .firstOrNull { it != null }
    }

    private fun searchNode(line: Int, node: Node): Node? {
        if (node.linenumber == line) return node
        return node.childNodes
                .map { searchNode(line, it) }
                .firstOrNull { it != null }
    }

}

fun CodeArea.getCaretLine(): Int {
    var count = -1
    var lines = 1
    val split = this.text.toCharArray()
    while (count != this.caretPosition) {
        count++
        if (count == split.size) break
        if (split[count] == '\n') lines++
    }
    if (count < split.size && split[count] == '\n') lines--
    return lines
}