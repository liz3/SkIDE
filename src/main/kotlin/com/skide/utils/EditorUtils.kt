package com.skide.utils

import com.skide.core.code.CodeManager
import com.skide.include.Node
import com.skide.include.NodeType
import org.fxmisc.richtext.CodeArea
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

        if(node.linenumber == line) return node

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

fun CodeArea.getCaretLine() = this.caretSelectionBind.paragraphIndex + 1

class StringSearchResult(val start: Int, val end: Int, val str: String)

class CurrentStateInfo(val currentNode: Node, val actualCurrentString: String, val column: Int, val currentWord: String,
                       val beforeString: String, val afterString: String, val charBeforeCaret: String, val charAfterCaret: String, val inString: Boolean)

fun CodeArea.getInfo(manager: CodeManager, currentLine: Int): CurrentStateInfo {



    val currentNode = EditorUtils.getLineNode(currentLine, manager.parseResult)
    val actualCurrentString = if(currentLine == 0) this.paragraphs[currentLine].text else this.paragraphs[currentLine - 1].text
    val column = this.caretColumn
    var currentWord = ""
    var beforeStr = ""
    var inString = false
    var afterStr = ""
    val charBeforeCaret = {
        if (column == 0) {
            ""
        } else {
            actualCurrentString[column - 1].toString()
        }
    }.invoke()
    val charAfterCaret = {
        if (column == actualCurrentString.length) {
            ""
        } else {
            actualCurrentString[column].toString()
        }
    }.invoke()

    for (x in 0 until actualCurrentString.length) {
        if (x == column) break
        val c = actualCurrentString[x]
        if (c == '"') inString = !inString
    }
    if (charBeforeCaret != "") {
        var count = column
        while (count > 0 && actualCurrentString[count - 1].toString() != " " && actualCurrentString[count - 1].toString() != "\n") {

            count--
            beforeStr = actualCurrentString[count].toString() + beforeStr
        }
        count = column - 1
        while (count < actualCurrentString.length - 1 && actualCurrentString[count].toString() != " " && actualCurrentString[count].toString() != "\n") {
            count++
            afterStr += actualCurrentString[count].toString()
        }

        beforeStr = beforeStr.replace("\t", "").replace(" ", "")
        afterStr = afterStr.replace("\t", "").replace(" ", "")
        currentWord = beforeStr + afterStr


    }

    return CurrentStateInfo(currentNode!!, actualCurrentString, column, currentWord, beforeStr, afterStr, charBeforeCaret, charAfterCaret, inString)
}

fun String.search(what: String, ignoreCase: Boolean, regex: Boolean): List<StringSearchResult> {
    val found = java.util.ArrayList<StringSearchResult>()

    if (regex) {
        if (!ignoreCase) {
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
        if (!ignoreCase) {
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