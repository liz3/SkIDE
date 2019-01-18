package com.skide.utils

import com.skide.include.Node
import com.skide.include.NodeType
import java.util.*
import java.util.regex.Pattern

enum class EventRequirementItemType {
    STRING, VARIABLE
}

class EventRequirementItem(val type: EventRequirementItemType, val value: Any)

class EventRequirement(val raw: String, val items: Vector<EventRequirementItem> = Vector())

object EditorUtils {

    fun fullFillsEventRequirements(reqs: EventRequirement, raw: String): Boolean {
        var lastIndex = 0
        for (req in reqs.items) {
            if (req.type == EventRequirementItemType.STRING) {
                if (raw.indexOf(req.value as String, lastIndex) == -1) return false
                lastIndex = raw.indexOf(req.value, lastIndex) + req.value.length - 1
            }
            if (req.type == EventRequirementItemType.VARIABLE) {
                val values = req.value as Vector<*>
                var found = false
                for (value in values) {
                    value as String
                    if (raw.indexOf(value, lastIndex) != -1) {
                        found = true
                        lastIndex = raw.indexOf(value, lastIndex) + value.length - 1
                        break
                    }
                }
                if (!found) return false
            }
        }
        return true
    }

    private fun extractOptionals(root: String): String {
        var cpy = root
        val optional = Pattern.compile("\\[[^\\[\\]]*\\]")
        while (true) {
            val matches = Vector<String>()
            val matcher = optional.matcher(cpy)
            if (!matcher.find()) break
            matches.addElement(matcher.group())
            while (matcher.find()) {
                matches.addElement(matcher.group())
            }
            var x = cpy
            for (match in matches) {
                x = x.replace(match, " ")
            }
            cpy = x
        }
        return cpy
    }

    private fun eventRequirement(root: String): EventRequirement {
        var noFounds = false
        var cpy = root
        val item = EventRequirement(root)
        val group = Pattern.compile("\\(([^)]+)\\)")
        val superGroup = Vector<String>()
        var isSuperGroup = false
        while (true) {
            val matcher = group.matcher(cpy)
            if (!matcher.find()) {
                if (item.items.size == 0) {
                    item.items.add(EventRequirementItem(EventRequirementItemType.STRING, cpy.trim()))
                    noFounds = true
                }
                break
            }
            val start = matcher.start()
            val end = matcher.end()
            if (start != 0 && cpy.substring(0, start).isNotBlank()) {
                if (!isSuperGroup)
                    item.items.addElement(EventRequirementItem(EventRequirementItemType.STRING,
                            cpy.substring(0, start).trim().replace("|", "")))
                else
                    superGroup.add(cpy.substring(0, start).trim().replace("|", ""))
            }
            cpy = cpy.substring(end)
            val text = matcher.group()
            if (text.startsWith("((")) {
                if (!isSuperGroup) isSuperGroup = true
                text.replace("(", "").replace(")", "").split("|").forEach {
                    superGroup.add(if (it.isBlank()) it else it.trim())
                }
            } else {
                if (isSuperGroup) {
                    val last = superGroup.last()
                    superGroup.remove(last)
                    text.replace("(", "").replace(")", "").split("|").forEach {
                        superGroup.add(if (it.isBlank()) "$last$it" else "$last${it.trim()}")
                    }
                    val nList = Vector<String>()
                    nList += superGroup
                    item.items.addElement(EventRequirementItem(EventRequirementItemType.VARIABLE, nList))
                    superGroup.clear()
                    if (cpy.first() == ')') isSuperGroup = false
                } else {
                    val vec = Vector<String>()
                    text.replace("(", "").replace(")", "").split("|").forEach {
                        vec.add(if (it.isBlank()) it else it.trim())
                    }
                    item.items.addElement(EventRequirementItem(EventRequirementItemType.VARIABLE, vec))
                }
            }
        }
        if (cpy.isNotEmpty() && cpy.isNotBlank() && !noFounds)
            cpy.replace("(", "").replace(")", "").trim().split(" ").forEach {
                if (it.isNotEmpty() && it.isNotBlank())
                    if (!it.startsWith("|"))
                        item.items.add(EventRequirementItem(EventRequirementItemType.STRING, it.trim()))
                    else
                        @Suppress("UNCHECKED_CAST")
                        if (item.items.lastElement().type == EventRequirementItemType.VARIABLE)
                            (item.items.lastElement().value as Vector<String>).addElement(it.substring(1))
            }
        return item
    }


    fun extractNeededPartsFromEvent(root: String) = eventRequirement(extractOptionals(root))


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

    fun flatList(nodes: Vector<Node>): Vector<Node> {
        val list = Vector<Node>()

        nodes.forEach {
            list.add(it)
            flatListLoader(it.childNodes, list)
        }

        return list
    }

    fun flatList(node: Node): Vector<Node> {
        val list = Vector<Node>()

        list.add(node)
        node.childNodes.forEach {
            list.add(it)
            flatListLoader(it.childNodes, list)
        }

        return list
    }

    private fun flatListLoader(node: Vector<Node>, list: Vector<Node>) {
        node.forEach {
            list.add(it)
            flatListLoader(it.childNodes, list)
        }
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

    fun filterByNodeType(type: NodeType, node: Node): Vector<Node> {

        val found = Vector<Node>()
        nodeByTypeIterator(type, node, found)
        return found
    }

    private fun nodeByTypeIterator(type: NodeType, node: Node, list: Vector<Node>) {
        node.childNodes.forEach {
            nodeByTypeIterator(type, it, list)
        }
        if (node.nodeType == type && !list.contains(node)) list.addElement(node)

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