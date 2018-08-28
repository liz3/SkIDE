package com.skide.core.code.autocomplete

import com.skide.core.code.CodeManager
import com.skide.gui.GUIManager
import com.skide.gui.controllers.GenerateCommandController
import com.skide.include.DocType
import com.skide.include.OpenFileHolder
import netscape.javascript.JSObject

class AutoCompleteCompute(val manager: CodeManager, val project: OpenFileHolder) {

    val area = manager.area

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
    fun showGlobalAutoComplete(array:JSObject) {

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
                        text = text.replace("[on]", "on").replace("\n", "") + ":"
                        if (!text.startsWith("on ")) text = "on $text"
                        text
                    }.invoke(), commandId = "general_auto_complete_finish").createObject(area.getObject()))

                    count++
                }
            }
        }

    }
}