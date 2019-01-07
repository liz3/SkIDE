package com.skide.core.code.autocomplete

import com.skide.Info
import com.skide.core.code.CodeManager
import com.skide.core.code.DefinitionFinderResult
import com.skide.gui.GUIManager
import com.skide.gui.controllers.GenerateCommandController
import com.skide.include.*
import com.skide.utils.EditorUtils
import netscape.javascript.JSObject
import java.util.*
import kotlin.collections.HashMap
import kotlin.system.measureTimeMillis

class AutoCompleteCompute(val manager: CodeManager, val project: OpenFileHolder) {

    val area = manager.area
    private val addonSupported = project.openProject.addons
    private val keyWordsGen = getKeyWords()
    private val inspectorItems = getSuppressors()
    fun createCommand() {

        val window = GUIManager.getWindow("fxml/GenerateCommand.fxml", "Generate command", true)
        val generate = window.controller as GenerateCommandController
        generate.cancelButton.setOnAction {
            GUIManager.closeGui(window.id)
        }
        generate.createButton.setOnAction {

            val line = area.getCurrentLine()
            area.replaceContentInRange(line, 1, line, area.getColumnLineAmount(line), generate.generateString())
            GUIManager.closeGui(window.id)
        }
    }

    fun showLocalAutoComplete(array: JSObject) {


           val nodes = area.openFileHolder.codeManager.parseResult
           val currentLine = area.getCurrentLine()
           val currentColumn = area.getCurrentColumn()
           val lineContent = area.getLineContent(currentLine)
           val node = EditorUtils.getLineNode(currentLine, nodes) ?: return
           val parent = EditorUtils.getRootOf(node)
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

           //Croos file Auto-complete
           if (project.coreManager.configManager.get("cross_auto_complete") == "true") {
               for ((path, internalNodes) in manager.crossNodes) {
                   if (path == project.f) continue
                   internalNodes.forEach { it ->
                       if (it.nodeType == NodeType.FUNCTION && it.fields.contains("ready")) {
                           val name = it.fields["name"] as String

                           val returnType = it.fields["return"] as String
                           var paramsStr = ""
                           var insertParams = ""
                           (it.fields["params"] as Vector<*>).forEach {
                               it as MethodParameter
                               paramsStr += ",${it.name} ${it.type}"
                               insertParams += ",${it.name}"
                           }
                           if (paramsStr != "") paramsStr = paramsStr.substring(1)
                           if (insertParams != "") insertParams = insertParams.substring(1)
                           val con = "$name($paramsStr)"

                           val insert = "$name($insertParams)"
                           addSuggestionToObject(AutoCompleteItem(area, con, CompletionType.FUNCTION, insert, "$returnType - ${path.name}"), array, count)
                           count++
                       } else if (it.nodeType == NodeType.SET_VAR && !it.fields.containsKey("invalid")) {
                           if (it.fields.contains("from_option")) {
                               val insertText = "{@${it.fields["name"]}}}"
                               if (!varsToAdd.containsKey(insertText))
                                   varsToAdd[insertText] = AutoCompleteItem(area,  (it.fields["name"] as String), CompletionType.VARIABLE, insertText, "Option - ${path.name}")
                           } else if (it.fields["visibility"] == "global") {
                               val insert = "{${it.fields["name"]}}"
                               if (!varsToAdd.containsKey(insert))
                                   varsToAdd[insert] = AutoCompleteItem(area, it.fields["name"] as String, CompletionType.VARIABLE, insert, "Variable - ${path.name}")
                           }
                       }
                   }
                   EditorUtils.filterByNodeType(NodeType.OPTIONS, internalNodes).forEach {
                       for (child in it.childNodes)
                           if (child.getContent().isNotEmpty() && child.getContent().isNotBlank() && child.nodeType != NodeType.COMMENT) {
                               val name = child.getContent().split(":").first()
                               val word = "{@$name}"
                               if (!varsToAdd.containsKey(word))
                                   varsToAdd[word] = AutoCompleteItem(area, name, CompletionType.VARIABLE, word, "Option - ${path.name}")
                           }
                   }
               }
           }

           //Check parent
           if (parent.nodeType == NodeType.FUNCTION && parent.fields.contains("ready")) {
               val params = parent.fields["params"] as Vector<*>
               params.forEach {
                   it as MethodParameter
                   val insert = "{_" + it.name + "}"
                   if (!varsToAdd.containsKey(insert))
                       varsToAdd[insert] = AutoCompleteItem(area, it.name, CompletionType.VARIABLE, insert, it.type)
               }
           }

           //Loop through this files variable nodes
           vars.forEach {
               if (it.nodeType == NodeType.SET_VAR && !it.fields.containsKey("invalid")) {
                   if (it.fields.contains("from_option")) {
                       val insertText = "{@${it.fields["name"]}}"
                       if (!varsToAdd.containsKey(insertText))
                           varsToAdd[insertText] = AutoCompleteItem(area, (it.fields["name"] as String), CompletionType.VARIABLE, insertText, "Option")
                   } else if (it.fields["visibility"] == "global") {
                       val insert = "{${it.fields["name"]}}"
                       if (!varsToAdd.containsKey(insert))
                           varsToAdd[insert] = AutoCompleteItem(area, it.fields["name"] as String, CompletionType.VARIABLE, insert, "Global Variable")

                   } else if (EditorUtils.getRootOf(it) == parent) {
                       val insert = "{_${it.fields["name"]}}"
                       if (!varsToAdd.containsKey(insert))
                           varsToAdd[insert] = AutoCompleteItem(area, it.fields["name"] as String, CompletionType.VARIABLE, insert, "Local variable")
                   }
               }
           }
           EditorUtils.filterByNodeType(NodeType.OPTIONS, nodes).forEach {
               for (child in it.childNodes)
                   if (child.getContent().isNotEmpty() && child.getContent().isNotBlank() && child.nodeType != NodeType.COMMENT) {
                       val name = child.getContent().split(":").first()
                       val word = "{@$name}"
                       if (!varsToAdd.containsKey(word))
                           varsToAdd[word] = AutoCompleteItem(area, name, CompletionType.VARIABLE, word, "Option")
                   }
           }

           //Add functions
           nodes.forEach {
               if (it.nodeType == NodeType.FUNCTION && it.fields.contains("ready")) {
                   val name = it.fields["name"] as String
                   val returnType = it.fields["return"] as String
                   var paramsStr = ""
                   var insertParams = ""

                   (it.fields["params"] as Vector<*>).forEach { param ->
                       param as MethodParameter
                       paramsStr += ",${param.name} ${param.type}"
                       insertParams += ",${param.name}"
                   }
                   if (paramsStr != "") paramsStr = paramsStr.substring(1)
                   if (insertParams != "") insertParams = insertParams.substring(1)
                   val con = "$name($paramsStr)"
                   val insert = "$name($insertParams)"

                   addSuggestionToObject(AutoCompleteItem(area, con, CompletionType.FUNCTION, insert, returnType), array, count)
                   count++
               }
           }


           //add generic keywords
           keyWordsGen.forEach {
               addSuggestionToObject(it, array, count)
               count++
           }

           if (parent.nodeType == NodeType.EVENT)
               if (parent.fields["event"] != null) {
                   val ev = parent.fields["event"] as AddonItem
                   if (ev.eventValues != "") {
                       ev.eventValues.split(",").forEach {
                           val value = it.trim()
                           addSuggestionToObject(AutoCompleteItem(area, value, CompletionType.KEYWORD, value, "event value"), array, count)
                           count++
                       }
                   }
               }

           //Add all nodes in one move
           varsToAdd.values.forEach {
               addSuggestionToObject(it, array, count)
               count++
           }

           if (!manager.sequenceReplaceHandler.computing) {

               if(project.coreManager.configManager.get("auto_complete_addon") == "true") {
                   addonSupported.values.forEach { addon ->
                       addon.forEach { item ->
                           if (item.type != DocType.EVENT) {
                               val name = item.name
                               var adder = (if (item.pattern == "") item.name.toLowerCase() else item.pattern).replace("\n", "").replace("\r","")
                               if (item.type == DocType.CONDITION) if (!lineContent.contains("if ")) adder = "if $adder"
                               if (item.type == DocType.CONDITION) adder += ":"
                               addSuggestionToObject(AutoCompleteItem(area, name, CompletionType.MODULE, adder, commandId = "general_auto_complete_finish", detail = "${item.type} - ${item.addon.name}"), array, count)
                               count++
                           }
                       }
                   }

               }
               for (snippet in project.coreManager.snippetManager.snippets) {
                   if(node.parent == null) continue
                   if(!snippet.rootRule.allowedTypes.contains(parent.nodeType)) continue
                   if(!snippet.parentRule.allowedTypes.contains(node.parent.nodeType)) continue

                   if(snippet.rootRule.startsWith.first &&
                           !parent.getContent().startsWith(snippet.rootRule.startsWith.second))continue
                   if(snippet.rootRule.contains.first &&
                           !parent.getContent().contains(snippet.rootRule.contains.second))continue
                   if(snippet.rootRule.endsWith.first &&
                           !parent.getContent().endsWith(snippet.rootRule.endsWith.second))continue

                   if(snippet.parentRule.startsWith.first &&
                           !node.parent.getContent().startsWith(snippet.parentRule.startsWith.second))continue
                   if(snippet.parentRule.contains.first &&
                           !node.parent.getContent().contains(snippet.parentRule.contains.second))continue
                   if(snippet.parentRule.endsWith.first &&
                           !node.parent.getContent().endsWith(snippet.parentRule.endsWith.second))continue


                   if(!snippet.triggerReplaceSequence)
                       addSuggestionToObject(AutoCompleteItem(area, snippet.label, CompletionType.SNIPPET, snippet.insertText), array, count)
                   else
                       addSuggestionToObject(AutoCompleteItem(area, snippet.label, CompletionType.SNIPPET, snippet.insertText, commandId = "general_auto_complete_finish", detail = snippet.name), array, count)
                   count++
               }
           }

           varsToAdd.clear()
    }

    private fun getKeyWords(): Vector<AutoCompleteItem> {
        val vector = Vector<AutoCompleteItem>()
        arrayOf("set", "if", "stop", "loop", "return", "function", "options", "true", "false", "else", "else if", "trigger", "on", "while", "is").forEach {
            vector.add(AutoCompleteItem(area, it, CompletionType.KEYWORD, it, "Generic Keyword"))
        }
        return vector
    }

    private fun getSuppressors(): Vector<AutoCompleteItem> {
        val vector = Vector<AutoCompleteItem>()
        vector.add(AutoCompleteItem(area, "@skide:ignore-case", CompletionType.SNIPPET, "#@skide:ignore-case", "SkIDE Inspections", "Variables can ignore cases, when thats enabled, this will tell the error checker to ignore that"))
        vector.add(AutoCompleteItem(area, "@skide:ignore-missing-functions", CompletionType.SNIPPET, "#@skide:ignore-missing-functions", "SkIDE Inspections", "Ignores missing functions"))
        vector.add(AutoCompleteItem(area, "@skide:ignore-all", CompletionType.SNIPPET, "#@skide:ignore-all", "SkIDE Inspections", "Disables all inspections"))
        return vector
    }

    fun showGlobalAutoComplete(array: JSObject) {

        var count = 0
        array.setSlot(count, AutoCompleteItem(area, "function", CompletionType.FUNCTION, "function () :: :", "Generates a function", "This will create a function").createObject(area.getObject()))
        count++
        array.setSlot(count, AutoCompleteItem(area, "Command", CompletionType.CONSTRUCTOR, "", "Generates a Command", "This will open a Window to create a ", commandId = "create_command").createObject(area.getObject()))
        count++
        val hasOn = area.getLineContent(area.getCurrentLine()).startsWith("on")
        area.openFileHolder.openProject.addons.values.forEach {
            it.forEach { ev ->
                if (ev.type == DocType.EVENT) {
                    array.setSlot(count, AutoCompleteItem(area, "EVENT: ${ev.name} (${ev.addon.name})", CompletionType.METHOD, {
                        var text = ev.pattern
                        if (text.isEmpty()) text = ev.name
                        text = text.replace("[on]", if (hasOn) "" else "on").replace("\n", "")

                        "$text:"
                    }.invoke(), commandId = "general_auto_complete_finish").createObject(area.getObject()))

                    count++
                }
            }
        }
        inspectorItems.forEach {
            addSuggestionToObject(it, array, count)
            count++
        }
        for (snippet in project.coreManager.snippetManager.snippets) {
            if(!snippet.rootRule.allowedTypes.contains(NodeType.ROOT)) continue
            addSuggestionToObject(AutoCompleteItem(area, snippet.label, CompletionType.SNIPPET, snippet.insertText, snippet.name), array, count)
            count++
        }
    }
}