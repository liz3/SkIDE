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

fun CodeArea.getCaretLine() =  this.caretSelectionBind.lineIndex.asInt
