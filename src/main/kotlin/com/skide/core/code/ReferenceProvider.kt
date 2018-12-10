package com.skide.core.code

import com.skide.include.NodeType
import com.skide.utils.EditorUtils
import netscape.javascript.JSObject

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
            val flatList = EditorUtils.flatList(nodes)

            val name = currentNode.fields["name"] as String

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
        }
        if (currentNode?.nodeType == NodeType.SET_VAR) {
            val name = currentNode.fields["name"] as String
            val visibility = currentNode.fields["visibility"] as String
            if (visibility == "local") {
                val flatList = EditorUtils.flatList(EditorUtils.getRootOf(currentNode))

                for (node in flatList) {
                    if (node == currentNode) continue


                    if (node.getContent().contains("{_$name")) {
                        array.setSlot(counter, getObj(node.linenumber, node.raw.indexOf("{_$name") + 1, name.length + 3, model))
                        counter++
                    }

                }

            } else {

            }


        }

        return array
    }

}