package com.skriptide.core.code

import com.skriptide.core.skript.SkriptParser
import com.skriptide.include.Node
import com.skriptide.include.NodeType
import com.skriptide.include.OpenFileHolder
import com.skriptide.utils.readFile
import javafx.application.Platform
import javafx.scene.control.TreeItem
import org.fxmisc.richtext.CodeArea

class CodeManager {
    var rootStructureItem = TreeItem<String>("")
    private var area = CodeArea()
    private var content = ""
    fun setup(project: OpenFileHolder) {

        rootStructureItem = TreeItem(project.name)
        content = readFile(project.f).second
        area = project.area
        area.appendText(content)
        parseStructure(content, rootStructureItem)

    }

    fun gotoItem(item: TreeItem<String>) {

        if (item == rootStructureItem) return
        val lineSearched = item.value.split(" ")[0].toInt()
        val split = content.split("\n")
        val count = (0 until lineSearched-1).sumBy { split[it].length + 1 }

        Platform.runLater {
            area.requestFocus()
            area.moveTo(count)
            //TODO still needs some adjustment
            area.scrollYToPixel((lineSearched * 14.95))
        }
    }

    fun parseStructure(content: String, rootStructureItem: TreeItem<String>) {

        rootStructureItem.children.clear()
        val result = SkriptParser().superParse(content)



        result.forEach {
            add(rootStructureItem, it)
        }
    }

    private fun add(parent: TreeItem<String>, node: Node) {


        val name = {
            var name = (node.linenumber).toString() + " " + node.nodeType.toString()

            if (node.nodeType == NodeType.COMMAND) {
                name += ": " + node.fields["name"]
            }
            if (node.nodeType == NodeType.EVENT) {
                name += ": " + node.fields["name"]
            }
            if (node.nodeType == NodeType.FUNCTION) {
                name += ": " + node.fields["name"] + " :" + node.fields["return"]
            }

            name
        }.invoke()

        val item = TreeItem<String>(name)

        node.childNodes.forEach {
            add(item, it)
        }

        if (node.nodeType != NodeType.COMMENT) parent.children.add(item)
    }

}