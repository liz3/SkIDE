package com.skriptide.core.code

import com.skriptide.core.skript.SkriptParser
import com.skriptide.include.Node
import com.skriptide.include.NodeType
import com.skriptide.include.OpenFileHolder
import com.skriptide.utils.readFile
import javafx.scene.control.TreeItem

class CodeManager(val project: OpenFileHolder) {

    val rootStructureItem = TreeItem<String>(project.name)
    val content = readFile(project.f).second

    fun setup() {
        val area = project.area
        area.appendText(content)
        parseStructure(content)
    }

    fun parseStructure(content: String) {

        rootStructureItem.children.clear()
        val result = SkriptParser().superParse(content)



        result.forEach {
            add(rootStructureItem, it)
        }
    }

   private fun add(parent: TreeItem<String>, node: Node) {


       val name = {
           var name = (node.linenumber).toString() + " " +node.nodeType.toString()

           if(node.nodeType == NodeType.COMMAND) {
               name += ": " + node.fields["name"]
           }
           if(node.nodeType == NodeType.EVENT) {
               name += ": " + node.fields["name"]
           }
           if(node.nodeType == NodeType.FUNCTION) {
               name += ": " + node.fields["name"] + " :" + node.fields["return"]
           }

           name
       }.invoke()

       val item = TreeItem<String>(name)

       node.childNodes.forEach {
           add(item, it)
       }

       if(node.nodeType != NodeType.COMMENT) parent.children.add(item)
    }

}