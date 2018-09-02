package com.skide.core.code.autocomplete

import com.skide.core.code.CodeManager
import com.skide.gui.GUIManager
import com.skide.gui.controllers.GenerateCommandController
import com.skide.include.DocType
import com.skide.include.MethodParameter
import com.skide.include.NodeType
import com.skide.include.OpenFileHolder
import com.skide.utils.EditorUtils
import netscape.javascript.JSObject
import java.util.*
import kotlin.collections.HashMap

class AutoCompleteCompute(val manager: CodeManager, val project: OpenFileHolder) {

    val area = manager.area
    val addonSupported = project.openProject.addons

    fun createCommand() {

        val window = GUIManager.getWindow("fxml/GenerateCommand.fxml", "Generate command", true)
        val generate = window.controller as GenerateCommandController
        generate.cancelButton.setOnAction {
            GUIManager.closeGui(window.id)
        }
        generate.createButton.setOnAction {
            val line = area.getCurrentLine()
            area.replaceContentInRange(line, 1, line, area.getColumnLineAmount(line), "command /" + generate.commandNameField.text + ":\n\tdescription: " + generate.descriptionField.text + "\n" + "\tpermission: " + generate.permissionField.text + "\n\ttrigger:\n\t\tsend \"hi\" to player")
            GUIManager.closeGui(window.id)
        }
    }

    fun showLocalAutoComplete(array: JSObject) {

        if(manager.sequenceReplaceHandler.computing) return
        val nodes = area.openFileHolder.codeManager.parseStructure()

        val currentLine = area.getCurrentLine()
        val currentColumn = area.getCurrentColumn()
        val lineContent = area.getLineContent(currentLine)
        val node = EditorUtils.getLineNode(currentLine, nodes)
        val parent = EditorUtils.getRootOf(node!!)
        var count = 0
        val currentWord = area.getWordUntilPosition(currentLine, currentColumn)
        val before = area.getContentRange(currentLine, currentLine, 1, currentWord.startColumn)
        val vars = EditorUtils.filterByNodeType(NodeType.SET_VAR, nodes)

        if (before.endsWith(":") && node.nodeType == NodeType.FUNCTION) {
            addonSupported.values.forEach { addon ->
                addon.forEach { item ->
                    if (item.type == DocType.TYPE) {
                        addSuggestionToObject(AutoCompleteItem(area, "${item.name}:${item.type} - ${item.addon.name}", CompletionType.CLASS, item.name), array, count)
                        count++
                    }
                }
            }

            return
        }
        val varsToAdd = HashMap<String, AutoCompleteItem>()
        if (project.coreManager.configManager.get("cross_auto_complete") == "true") {
            for ((path, nodes) in manager.crossNodes) {
                nodes.forEach {
                    if (it.nodeType == NodeType.FUNCTION && it.fields.contains("ready")) {
                        val name = it.fields["name"] as String

                        val returnType = it.fields["return"] as String
                        var paramsStr = ""
                        var insertParams = ""
                        (it.fields["params"] as Vector<*>).forEach {
                            it as MethodParameter
                            paramsStr += ",${it.name}:${it.type}"
                            insertParams += ",${it.name}"
                        }
                        if (paramsStr != "") paramsStr = paramsStr.substring(1)
                        if (insertParams != "") insertParams = insertParams.substring(1)
                        val con = "$name($paramsStr):$returnType - $path"

                        val insert = "$name($insertParams)"
                        addSuggestionToObject(AutoCompleteItem(area, con, CompletionType.FUNCTION, insert), array, count)
                        count++
                    } else if (it.nodeType == NodeType.SET_VAR && !it.fields.containsKey("invalid")) {
                        if (it.fields.contains("from_option")) {
                            val insertText = "{{@" + it.fields["name"] + "}::PATH}"
                            if (!varsToAdd.containsKey(insertText))
                                varsToAdd[insertText] = AutoCompleteItem(area, (it.fields["name"] as String) + " [from option] - $path", CompletionType.VARIABLE, insertText)
                        } else if (it.fields["visibility"] == "global") {
                            val insert = "{" + it.fields["name"] + "}"
                            if (!varsToAdd.containsKey(insert))
                                varsToAdd[insert] = AutoCompleteItem(area, it.fields["name"] as String + " - $path", CompletionType.VARIABLE, insert)
                        }
                    }
                }
            }
        }
        if (parent.nodeType == NodeType.FUNCTION && parent.fields.contains("ready")) {
            val params = parent.fields["params"] as Vector<*>

            params.forEach {
                it as MethodParameter

                val insert = "{_" + it.name + "}"

                if(!varsToAdd.containsKey(insert)) {
                    varsToAdd[insert] = AutoCompleteItem(area, it.name + " :" + it.type + " (local)", CompletionType.VARIABLE, insert)
                }
            }

        }
        vars.forEach {
            if (it.nodeType == NodeType.SET_VAR && !it.fields.containsKey("invalid")) {
                if (it.fields.contains("from_option")) {
                    val insertText = "{{@" + it.fields["name"] + "}::PATH}"
                    if (!varsToAdd.containsKey(insertText))
                        varsToAdd[insertText] = AutoCompleteItem(area, (it.fields["name"] as String) + " [from option]", CompletionType.VARIABLE, insertText)
                } else if (it.fields["visibility"] == "global") {
                    val insert = "{" + it.fields["name"] + "}"
                    if (!varsToAdd.containsKey(insert))
                        varsToAdd[insert] = AutoCompleteItem(area, it.fields["name"] as String, CompletionType.VARIABLE, insert)

                } else if (EditorUtils.getRootOf(it) == parent) {
                    val insert = "{_" + it.fields["name"] + "}"
                    if (!varsToAdd.containsKey(insert))
                        varsToAdd[insert] = AutoCompleteItem(area, it.fields["name"] as String, CompletionType.VARIABLE, insert)
                }


            }
        }
        nodes.forEach {
            if (it.nodeType == NodeType.FUNCTION && it.fields.contains("ready")) {
                val name = it.fields["name"] as String
                val returnType = it.fields["return"] as String
                var paramsStr = ""
                var insertParams = ""

                (it.fields["params"] as Vector<*>).forEach {
                    it as MethodParameter
                    paramsStr += ",${it.name}:${it.type}"
                    insertParams += ",${it.name}"
                }

                if (paramsStr != "") paramsStr = paramsStr.substring(1)
                if (insertParams != "") insertParams = insertParams.substring(1)
                val con = "$name($paramsStr):$returnType"
                val insert = "$name($insertParams)"

                addSuggestionToObject(AutoCompleteItem(area, con, CompletionType.FUNCTION, insert), array, count)
                count++
            }
        }
        addonSupported.values.forEach { addon ->
            addon.forEach { item ->
                val name = "${item.name}:${item.type} - ${item.addon.name}"
                var adder = (if (item.pattern == "") item.name.toLowerCase() else item.pattern).replace("\n", "")
                if (item.type == DocType.CONDITION) if (!lineContent.contains("if ")) adder = "if $adder:"

                addSuggestionToObject(AutoCompleteItem(area, name, CompletionType.MODULE, adder, commandId = "general_auto_complete_finish"), array, count)
                count++
            }
        }
        varsToAdd.values.forEach {
            addSuggestionToObject(it, array, count)
            count++
        }

    }

    fun showGlobalAutoComplete(array: JSObject) {

        var count = 0

        array.setSlot(count, AutoCompleteItem(area, "function", CompletionType.FUNCTION, "function () :: :", "Generates a function", "This will create a function").createObject(area.getObject()))
        count++
        array.setSlot(count, AutoCompleteItem(area, "Command", CompletionType.CONSTRUCTOR, "", "Generates a Command", "This will open a Window to create a ", commandId = "create_command").createObject(area.getObject()))
        count++

        area.openFileHolder.openProject.addons.values.forEach {
            it.forEach { ev ->
                if (ev.type == DocType.EVENT) {
                    array.setSlot(count, AutoCompleteItem(area, "EVENT: ${ev.name} (${ev.addon.name})", CompletionType.METHOD, {
                        var text = ev.pattern
                        if (text.isEmpty()) text = ev.name
                        text = text.replace("[on]", "on").replace("\n", "")

                        "on $text"
                    }.invoke(), commandId = "general_auto_complete_finish").createObject(area.getObject()))

                    count++
                }
            }
        }

    }
}