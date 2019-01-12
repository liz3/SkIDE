package com.skide.core.code

import com.skide.include.MethodParameter
import com.skide.include.NodeType
import com.skide.utils.EditorUtils
import java.util.*

class DefinitionFinderResult(val success: Boolean, val line: Int = -1, val column: Int = -1, val fName: String = "")

class DefinitionsFinder(val manager: CodeManager) {

    val area = manager.area

    fun search(lineNumber: Int, word: String): DefinitionFinderResult {
        val nodeStructure = manager.parseStructure()
        val line = EditorUtils.getLineNode(lineNumber, nodeStructure) ?: return DefinitionFinderResult(false)
        if (line.nodeType == NodeType.FUNCTION_CALL) {
            val name = line.fields["name"]
            EditorUtils.filterByNodeType(NodeType.FUNCTION, nodeStructure).forEach {
                if (it.fields["name"] == name) {
                    return DefinitionFinderResult(true, it.linenumber, 1)
                }
            }
            if (area.coreManager.configManager.get("cross_auto_complete") == "true") {
                manager.crossNodes.forEach { entry ->
                    EditorUtils.filterByNodeType(NodeType.FUNCTION, entry.value).forEach {
                        if (it.fields["name"] == name) {
                            return DefinitionFinderResult(true, it.linenumber, 1, entry.key.name)
                        }
                    }
                }
            }
        }
        if (line.nodeType == NodeType.IF_STATEMENT || line.nodeType == NodeType.STATEMENT || line.nodeType == NodeType.LOOP || line.nodeType == NodeType.SET_VAR || line.nodeType == NodeType.IF_STATEMENT) {
            val raw = line.raw
            if (raw.indexOf(word) >= 1) {
                if ((raw[raw.indexOf(word) - 1] == '{') || (raw[raw.indexOf(word) - 1] == '@') || (raw[raw.indexOf(word) - 1] == ':' && raw.contains("{"))) {

                    when {
                        raw[raw.indexOf(word) - 1] == '@' -> EditorUtils.filterByNodeType(NodeType.OPTIONS, nodeStructure).forEach {
                            for (child in it.childNodes) {
                                if (child.getContent().isNotEmpty() && child.getContent().isNotBlank()) {
                                    if (child.getContent().split(":").first().contains(word)) {
                                        return DefinitionFinderResult(true, child.linenumber, child.raw.indexOf(word) + 1)
                                    }
                                }

                            }
                        }
                        word.startsWith("_") -> {
                            val root = EditorUtils.getRootOf(line)

                            if (root.nodeType == NodeType.FUNCTION) {
                                val params = root.fields["params"] as Vector<*>
                                params.forEach {
                                    it as MethodParameter
                                    if (it.name == word.substring(1)) {
                                        return DefinitionFinderResult(true, root.linenumber, root.raw.indexOf(word.substring(1)) + 1)
                                    }
                                }
                            }
                            EditorUtils.filterByNodeType(NodeType.SET_VAR, root).forEach {

                                if ((it.fields["name"] as String).contains(word.substring(1)) && it.fields["visibility"] == "local") {

                                    return DefinitionFinderResult(true, it.linenumber, it.raw.indexOf(word) + 1)
                                }
                            }
                        }
                        else -> {
                            EditorUtils.filterByNodeType(NodeType.SET_VAR, nodeStructure).forEach {
                                if ((it.fields["name"] as String).contains(word))
                                    return DefinitionFinderResult(true, it.linenumber, it.raw.indexOf(word) + 1)
                            }
                            if (area.coreManager.configManager.get("cross_auto_complete") == "true") {
                                manager.crossNodes.forEach { entry ->
                                    EditorUtils.filterByNodeType(NodeType.SET_VAR, entry.value).forEach {
                                        if ((it.fields["name"] as String).contains(word)) {
                                            return DefinitionFinderResult(true, it.linenumber, it.raw.indexOf(word) + 1, entry.key.name)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (raw[raw.indexOf(word) + word.length] == '(') {
                    EditorUtils.filterByNodeType(NodeType.FUNCTION, nodeStructure).forEach {
                        if (it.fields["name"] == word) {

                            return DefinitionFinderResult(true, it.linenumber, 1)
                        }
                    }
                }
            }
        }
        return DefinitionFinderResult(false)
    }

}