package com.skide.utils

import com.skide.core.code.CodeManager
import com.skide.include.Node
import com.skide.include.NodeType
import java.util.*
import java.util.regex.Pattern

object EditorUtils {

    fun getLineNode(line: Int, all: Vector<Node>): Node? {

        all.forEach {
            if (it.linenumber == line) return it

            it.childNodes.forEach { c ->
                val r = searchNode(line, c)
                if (r != null) return r
            }
        }

        return null
    }


    private fun searchNode(line: Int, node: Node): Node? {

        if (node.linenumber == line) return node

        node.childNodes.forEach { c ->
            val r = searchNode(line, c)
            if (r != null) return r
        }

        return null
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

    fun getRootOf(node: Node): Node {

        if (node.parent == null) return node
        var n = node
        while (n.parent != null) n = n.parent!!
        return n
    }

    fun filterByNodeType(type: NodeType, list: Vector<Node>, limiter: Node): Vector<Node> {

        val found = Vector<Node>()

        for (item in list) {
            val result = filterByNodeTypeIterator(type, item, limiter)

            for (resultItem in result) {
                if (resultItem.first) return found
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



class StringSearchResult(val start: Int, val end: Int, val str: String)

class CurrentStateInfo(val currentNode: Node, val actualCurrentString: String, val column: Int, val currentWord: String,
                       val beforeString: String, val afterString: String, val charBeforeCaret: String, val charAfterCaret: String, val inString: Boolean, val inBrace: Boolean)



fun String.search(what: String, ignoreCase: Boolean, regex: Boolean): List<StringSearchResult> {
    val found = java.util.ArrayList<StringSearchResult>()

    if (regex) {
        if (ignoreCase) {
            val matcher = Pattern.compile(what).matcher(this)
            while (matcher.find()) {
                found.add(StringSearchResult(matcher.start(), matcher.end(), matcher.group()))
            }
        } else {
            val matcher = Pattern.compile(what, Pattern.CASE_INSENSITIVE).matcher(this)
            while (matcher.find()) {
                found.add(StringSearchResult(matcher.start(), matcher.end(), matcher.group()))
            }
        }

    } else {
        if (ignoreCase) {
            val matcher = Pattern.compile(Pattern.quote(what)).matcher(this)
            while (matcher.find()) {
                found.add(StringSearchResult(matcher.start(), matcher.end(), matcher.group()))
            }
        } else {
            val matcher = Pattern.compile(Pattern.quote(what), Pattern.CASE_INSENSITIVE).matcher(this)
            while (matcher.find()) {
                found.add(StringSearchResult(matcher.start(), matcher.end(), matcher.group()))
            }
        }

    }
    return found
}