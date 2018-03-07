package com.skide.utils

import com.skide.include.Node
import com.skide.include.NodeType
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


    fun filterByNodeType(type: NodeType, list: Vector<Node>): Vector<Node> {

        val found = Vector<Node>()

        list.forEach {
            filterByNodeTypeIterator(type, it).forEach { item ->
                found.addElement(item)
            }
        }

        return found
    }

    fun getRootOf(node:Node): Node {

        if(node.parent == null) return node
        var n = node
        while (n.parent != null) n = n.parent!!
        return n
    }

    fun filterByNodeType(type: NodeType, list: Vector<Node>, limiter:Node): Vector<Node> {

        val found = Vector<Node>()

        for(item in list) {
            val result = filterByNodeTypeIterator(type, item, limiter)

            for(resultItem in result) {
                if(resultItem.first) return found
                found.addElement(resultItem.second)
            }
        }

        return found
    }


    private fun filterByNodeTypeIterator(type: NodeType, node: Node, limiter: Node): Vector<Pair<Boolean, Node>> {

        val found = Vector<Pair<Boolean, Node>>()

        if (node !== limiter) {
            if (node.nodeType == type) found.add(Pair(false, node))

            for (child in node.childNodes) {
                val result = filterByNodeTypeIterator(type, child, limiter)
                for (r in result) {
                    found.add(r)
                    if (r.first) return found
                }
            }
        } else {
            found.add(Pair(true, node))
        }

        return found
    }

    private fun filterByNodeTypeIterator(type: NodeType, node: Node): Vector<Node> {

        val found = Vector<Node>()

        if (node.nodeType == type) {
            found.addElement(node)
        }
        node.childNodes.forEach {
            val result = filterByNodeTypeIterator(type, it)
            found += result
        }


        return found
    }
}

fun CodeArea.getCaretLine() = this.caretSelectionBind.paragraphIndex + 1
