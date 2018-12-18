package com.skide.core.code

import com.skide.include.MethodParameter
import com.skide.include.NodeType
import com.skide.utils.EditorUtils
import netscape.javascript.JSObject
import java.util.*

class ReferenceProvider(val manager: CodeManager) {


    private fun getObj(line: Int, col: Int, len: Int, model: Any): JSObject {
        val obj = manager.area.getObject()
        obj.setMember("uri", (model as JSObject).getMember("uri"))
        obj.setMember("range", manager.area.createObjectFromMap(hashMapOf(Pair("startLineNumber", line),
                Pair("endLineNumber", line), Pair("startColumn", col), Pair("endColumn", (col + len)))))
        return obj
    }

    fun findReference(model: Any, lineNumber: Int, word: String, array: JSObject): JSObject {
        var counter = 0
        val nodes = manager.parseResult
        val currentNode = EditorUtils.getLineNode(lineNumber, nodes)
        if (currentNode?.nodeType == NodeType.FUNCTION) {
            val name = currentNode.fields["name"] as String
            if (word == name) {
                val flatList = EditorUtils.flatList(nodes)
                for (node in flatList) {
                    if (node == currentNode) continue
                    if (node.nodeType != NodeType.COMMENT && node.nodeType != NodeType.COMMAND) {
                        if (node.getContent().replace(" ", "").contains("$name(")) {
                            val nLine = node.linenumber
                            val index = node.raw.indexOf(name)
                            array.setSlot(counter, getObj(nLine, index + 1, name.length, model))
                            counter++
                        }
                    }
                }
              /*
                if(manager.area.openFileHolder.coreManager.configManager.get("cross_auto_complete") == "true") {
                    for (crossNodeFile in manager.area.openFileHolder.openProject.crossNodes) {
                    }
                }
               */
            } else {
                val parameters = currentNode.fields["params"] as Vector<*>
                parameters.forEach {it as MethodParameter
                    if (word == it.name) {
                        val searched = "{_${it.name}}"
                        for (node in EditorUtils.flatList(EditorUtils.getRootOf(currentNode))) {
                            if (node == currentNode) continue
                            var index = node.raw.indexOf(searched)
                            while (index >= 0) {
                                array.setSlot(counter, getObj(node.linenumber, index  + 1, searched.length, model))
                                counter++
                                index = node.raw.indexOf(searched, index + 1)
                            }
                        }
                    }
                }
            }
        } else if (currentNode?.nodeType == NodeType.SET_VAR) {
            val name = currentNode.fields["name"] as String
            val visibility = currentNode.fields["visibility"] as String
            if (visibility == "local") {
                val flatList = EditorUtils.flatList(EditorUtils.getRootOf(currentNode))
                val searched = "{_$name}"
                for (node in flatList) {
                    if (node == currentNode) continue
                    var index = node.raw.indexOf(searched)
                    while (index >= 0) {
                        array.setSlot(counter, getObj(node.linenumber, index + 1, searched.length, model))
                        counter++
                        index = node.raw.indexOf(searched, index + 1)
                    }
                }
            } else {
                val flatList = EditorUtils.flatList(nodes)
                val searched = "{$name}"
                for (node in flatList) {
                    if (node == currentNode ||node.nodeType == NodeType.COMMENT) continue
                    var index = node.raw.indexOf(searched)
                    while (index >= 0) {
                        array.setSlot(counter, getObj(node.linenumber, index + 1, searched.length, model))
                        counter++
                        index = node.raw.indexOf(searched, index + 1)
                    }
                }
            }
        } else if(EditorUtils.getRootOf(currentNode!!).nodeType == NodeType.OPTIONS) {
            val searched = "{@$word}"
            for (node in EditorUtils.flatList(nodes)) {
                if(node.tabLevel == 0 || node == currentNode) continue
                var index = node.raw.indexOf(searched)
                while (index >= 0) {
                    array.setSlot(counter, getObj(node.linenumber, index + 1, searched.length, model))
                    counter++
                    index = node.raw.indexOf(searched, index + 1)
                }
            }
        }
        return array
    }
}