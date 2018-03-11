package com.skide.core.skript

import com.skide.include.Node
import java.util.*

class SkriptParser {

    fun superParse(content: String): Vector<Node> {

        var toSkip = 0
        val nodes = Vector<Node>()

        val added = Vector<Int>()

        val split = content.split("\n").toMutableList()




        println(split.size)

        for (x in 0 until split.size) {
            if (toSkip != 0) {
                toSkip--
                continue
            }
            val current = split[x]

            if (getTabCount(current) == 0) {
                val node = Node(null, split[x], 0, x + 1)
                toSkip += parse(x, node, split, added) - 1
                nodes.addElement(node)
            } else {
              if(!added.contains(x))  {
                  added.add(x)
                  nodes.addElement(Node(null, split[x], getTabCount(current), x + 1))
              }
            }
        }
        println(added.size)
        return nodes
    }

    private fun parse(index: Int, parent: Node, split: List<String>, added: Vector<Int>): Int {

        var looped = 0
        var toSkip = 0
        var before = Node(parent, "", 0, 0)
        for (x in index + 1 until split.size) {
            looped++
            if (toSkip != 0) {
                toSkip--
                continue
            }
            val current = split[x]
            val currentTabCount = getTabCount(current)
            if (added.contains(x)) continue
            if (currentTabCount == (parent.tabLevel + 1)) {
                before = Node(parent, current, currentTabCount, x + 1)
                parent.childNodes.add(before)
                added.add(x)
                continue
            }
            if (currentTabCount > (parent.tabLevel + 1)) {
                val node = Node(parent, current, currentTabCount, x + 1)
                toSkip += parse(x, node, split, added) - 1
                before.childNodes.add(node)
                added.add(x)
                continue
            }
            if (currentTabCount < (parent.tabLevel + 1)) {
                if(current.replace("\t", " ").isBlank()) {
                    if(x < split.size -1) {
                        if(getTabCount(split[x + 1]) > currentTabCount) {
                            before = Node(parent, current, currentTabCount, x + 1)
                            parent.childNodes.add(before)
                            added.add(x)
                            continue
                        }
                    }
                }
                break
            }
        }
        return looped
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