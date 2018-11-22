package com.skide.core.skript

import com.skide.include.Node
import java.util.*

class LineHolder(val line: String, val tabs: Int, val linenumber: Int)
class SkriptParser {

    fun superParse(rawContent: String): Vector<Node> {

        val nodes = Vector<Node>()
        val lines = rawContent.split("\n")
        var currentLevel = 0
        var lastActive = Node(null, "", 0, 1)
        var count = 0
        for (line in lines) {
            count++

            val tabCount = getTabCount(line)
            if (tabCount == 0) {
                if (line.isBlank()) {
                    if (currentLevel > 0) {
                        val node = Node(lastActive.parent, line, currentLevel, count)
                        lastActive.parent!!.childNodes.addElement(node)
                        lastActive = node
                    } else {
                        nodes.addElement(Node(null, "", 0, count))
                    }
                    continue
                }
                lastActive = Node(null, line, 0, count)
                nodes.add(lastActive)
                currentLevel = 0
                continue
            }
            if (tabCount > currentLevel) {
                val child = Node(lastActive, line, tabCount, count)
                currentLevel++
                lastActive.childNodes.addElement(child)
                lastActive = child
                continue
            }
            if (tabCount < currentLevel) {
                while (currentLevel != tabCount) {
                    currentLevel--
                    lastActive = lastActive.parent!!
                }
                val nodeAdd = Node(lastActive.parent, line, tabCount, count)
                lastActive.parent!!.childNodes.addElement(nodeAdd)
                lastActive = nodeAdd
                continue
            }
            if (tabCount == currentLevel) {
                val node = Node(lastActive.parent, line, tabCount, count)
                lastActive.parent!!.childNodes.addElement(node)
                lastActive = node
            }

        }

        return nodes
    }

    private fun getTabCount(str: String): Int {
        var count = 0
        var cp = str
        return if (str.startsWith("    ")) {
            while (cp.startsWith("    ")) {
                count++
                cp = cp.substring(4)
            }
            count
        } else {
            while (cp.startsWith("\t")) {
                count++
                cp = cp.substring(1)
            }
            count
        }
    }


}