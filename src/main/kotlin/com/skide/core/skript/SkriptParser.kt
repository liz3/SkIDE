package com.skide.core.skript

import com.skide.CoreManager
import com.skide.core.management.OpenProject
import com.skide.include.AddonItem
import com.skide.include.DocType
import com.skide.include.Node
import com.skide.include.NodeType
import java.util.*

class LineHolder(val line: String, val tabs: Int, val linenumber: Int)
class SkriptParser(val manager: OpenProject) {
    val events = Vector<AddonItem>()

    init {
        for (value in manager.addons.values) {
            for (addonItem in value) {
                if (addonItem.type == DocType.EVENT)
                    events.add(addonItem)
            }
        }
        for (addonItem in manager.coreManager.resourceManager.skript.versions["default"]!!) {
            if (addonItem.type == DocType.EVENT)
                events.add(addonItem)
        }
    }

    fun superParse(rawContent: String): Vector<Node> {

        val nodes = Vector<Node>()
        val lines = rawContent.split("\n")
        var currentLevel = 0
        var lastActive = Node(this, null, "", 0, 1)
        var count = 0
        for (line in lines) {
            count++

            val tabCount = getTabCount(line)
            if (tabCount == 0) {
                if (line.isBlank()) {
                    if (currentLevel > 0) {
                        val node = Node(this, lastActive.parent, line, currentLevel, count)
                        lastActive.parent!!.childNodes.addElement(node)
                        lastActive = node
                    } else {
                        nodes.addElement(Node(this, null, "", 0, count))
                    }
                    continue
                }
                val node = Node(this, null, line, 0, count)
                if (node.nodeType == NodeType.COMMENT) {
                    nodes.add(node)
                } else {
                    lastActive = node
                    nodes.add(lastActive)
                    currentLevel = 0
                }
                continue
            }
            if (tabCount > currentLevel) {
                val child = Node(this, lastActive, line, tabCount, count)
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
                val nodeAdd = Node(this, lastActive.parent, line, tabCount, count)
                lastActive.parent!!.childNodes.addElement(nodeAdd)
                lastActive = nodeAdd
                continue
            }
            if (tabCount == currentLevel) {
                val node = Node(this, lastActive.parent, line, tabCount, count)
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