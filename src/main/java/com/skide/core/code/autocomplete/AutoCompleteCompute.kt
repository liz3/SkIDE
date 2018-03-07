package com.skide.core.code.autocomplete

import com.skide.core.code.CodeManager
import com.skide.gui.Prompts
import com.skide.include.MethodParameter
import com.skide.include.Node
import com.skide.include.NodeType
import com.skide.include.OpenFileHolder
import com.skide.utils.EditorUtils
import com.skide.utils.getCaretLine
import javafx.application.Platform
import javafx.geometry.Bounds
import javafx.scene.control.ListView
import javafx.scene.input.KeyCode
import javafx.stage.Popup
import org.reactfx.EventStreams
import org.reactfx.EventStreams.nonNullValuesOf
import org.reactfx.value.Var
import java.util.*
import kotlin.collections.HashMap

class ListHolderItem(val name: String, val caller: () -> Unit, val description: String = "") {

    override fun toString(): String {
        return name
    }
}

class AutoCompleteCompute(val manager: CodeManager, val project: OpenFileHolder) {

    val area = manager.area


    val removed = Vector<ListHolderItem>()
    val fillList = ListView<ListHolderItem>()
    val popUp = Popup()

    var currentLine = 0
    var lineBefore = 0

    var wasJustCalled = false
    var colPos = 0
    var coldPosOld = 0

    init {


        setupContextPopup()
        registerEventListener()

        currentLine = area.getCaretLine()
        lineBefore = currentLine
    }

    private fun setupContextPopup() {
        val caretXOffset = 0.0
        val caretYOffset = 0.0
        val caretBounds = nonNullValuesOf(area.caretBoundsProperty())
        val caretPopupSub = EventStreams.combine<Optional<Bounds>, Boolean>(caretBounds, Var.newSimpleVar(true).values())
                .subscribe { tuple3 ->
                    val opt = tuple3._1
                    if (opt.isPresent) {
                        val b = opt.get()
                        popUp.x = b.maxX + caretXOffset
                        popUp.y = b.maxY + caretYOffset
                    } else {
                        popUp.hide()
                    }
                }

        fillList.setPrefSize(280.0, 200.0)
        popUp.content.add(fillList)
        caretPopupSub.and(caretBounds.subscribe({ }))
        area.requestFollowCaret()


        fillList.setOnMouseClicked { e ->
            if (e.clickCount == 2) {
                if (fillList.selectionModel.selectedItem != null) {
                    val value = fillList.selectionModel.selectedItem as ListHolderItem
                    value.caller.invoke()

                }
            }
        }

        fillList.setOnKeyPressed { ev ->
            if (ev.code == KeyCode.ESCAPE) {
                popUp.hide()

            }


        }


    }

    private fun addItem(label: String, caller: () -> Unit) = fillList.items.add(ListHolderItem(label, caller))


    private fun registerEventListener() {


        area.caretPositionProperty().addListener { observable, oldValue, newValue ->

            lineBefore = currentLine
            currentLine = area.getCaretLine()
            coldPosOld = colPos
            colPos = area.caretColumn
            if (lineBefore != currentLine) {
                onLineChange()
            } else {
                onColumnChange()
            }

        }
        area.textProperty().addListener { observable, oldValue, newValue ->

        }
    }

    private fun onColumnChange() {

        if (colPos == (coldPosOld + 1)) {
            if (wasJustCalled) {
                wasJustCalled = false
            } else {

                showLocalAutoComplete(true)

            }
        }
        if (colPos == (coldPosOld - 1)) {
            if (popUp.isShowing) {
                showLocalAutoComplete(true)
            }
        }
    }


    fun showLocalAutoComplete(movedRight: Boolean) {

        val currentNode = EditorUtils.getLineNode(currentLine, manager.parseResult)
        val actualCurrentString = area.paragraphs[currentLine - 1].text
        val column = area.caretColumn
        var currentWord = ""
        var beforeStr = ""
        var afterStr = ""
        var charBeforeCaret = {
            if (column == 0) {
                ""
            } else {
                actualCurrentString[column - 1].toString()
            }
        }.invoke()
        var charAfterCaret = {
            if (column == actualCurrentString.length) {
                ""
            } else {
                actualCurrentString[column].toString()
            }
        }.invoke()

        if (charBeforeCaret != "") {
            var beforeStr = ""
            var afterStr = ""

            var count = column
            while (count > 0 && actualCurrentString[count - 1].toString() != " " && actualCurrentString[count - 1].toString() != "\n") {

                count--
                beforeStr = actualCurrentString[count].toString() + beforeStr
            }
            count = column - 1
            while (count < actualCurrentString.length - 1 && actualCurrentString[count].toString() != " " && actualCurrentString[count].toString() != "\n") {
                count++
                afterStr += actualCurrentString[count].toString()
            }

            beforeStr = beforeStr.replace("\t", "").replace(" ", "")
            afterStr = afterStr.replace("\t", "").replace(" ", "")
            currentWord = beforeStr + afterStr
        } else {
            println("lol")
        }
        println(currentWord)

        if ((column - 2) >= 0) {

            if (actualCurrentString[column - 2] == ' ' && charBeforeCaret == " ") {
                if (popUp.isShowing) {
                    removed.clear()
                    popUp.hide()
                    fillList.items.clear()
                }
                println("WTTTTTTTTTTT")
                return
            }
        }


        if (popUp.isShowing) {
            /*
             if (currentWord == "") {
                 removed.clear()
                 popUp.hide()
                 fillList.items.clear()
                 return
             }
             */

            val toRemove = Vector<ListHolderItem>()

            for (item in fillList.items) {
                if (!item.name.startsWith(currentWord, true)) {
                    println("Removed " + item.name + " : " + currentWord)
                    toRemove.add(item)
                }
            }
            toRemove.forEach {
                println("removed " + it.name + " on re-scan")
                fillList.items.remove(it)
                removed.add(it)
            }
            toRemove.clear()
            for (item in removed) {
                if (item.name.startsWith(currentWord, true)) {
                    toRemove.add(item)
                }
            }

            toRemove.forEach {
                removed.remove(it)
                fillList.items.add(it)
            }
            fillList.refresh()
            println(fillList.items.size.toString() + " on re-scan")


            return
        } else {


            manager.parseResult = manager.parseStructure()
            fillList.items.clear()
            removed.clear()
            println("ADDING ELEMENTS===================")
            val toAdd = HashMap<String, Pair<NodeType, () -> Unit>>()
            val root = EditorUtils.getRootOf(currentNode!!)

            val vars = EditorUtils.filterByNodeType(NodeType.SET_VAR, manager.parseResult, currentNode)

            if (root.nodeType == NodeType.FUNCTION) {
                val params = root.fields["params"] as Vector<MethodParameter>

                params.forEach {
                    toAdd.put("_" + it.name + " :" + it.type, Pair(NodeType.SET_VAR, {
                        area.replaceText(area.caretPosition, area.caretPosition, "{" + it.name + "}")
                    }))
                }
            }

            if (root.nodeType == NodeType.EVENT || root.nodeType == NodeType.COMMAND) {
                toAdd.put("player:Player", Pair(NodeType.SET_VAR, {
                    area.replaceText(area.caretPosition, area.caretPosition, "player")
                }))
            }

            manager.parseResult.forEach {
                if (it.nodeType == NodeType.FUNCTION) {
                    val name = it.fields["name"] as String
                    val returnType = it.fields["return"] as String
                    var paramsStr = ""
                    var insertParams = ""
                    (it.fields["params"] as Vector<MethodParameter>).forEach {
                        paramsStr += ",${it.name}:${it.type}"
                        insertParams += ",${it.name}"
                    }
                    paramsStr = paramsStr.substring(1)
                    insertParams = insertParams.substring(1)
                    val con = "$name($paramsStr):$returnType"

                    val insert = "$name($insertParams)"
                    toAdd.put(con, Pair(NodeType.FUNCTION, {
                        area.replaceText(area.caretPosition, area.caretPosition, insert)
                    }))

                }
            }


            if (root.nodeType == NodeType.FUNCTION) {

                vars.forEach {
                    if (it.fields["visibility"] == "global") {
                        addItem("VAR " + it.fields["name"], {
                            area.replaceText(area.caretPosition, area.caretPosition, "{" + it.fields["name"] + "}")
                        })
                    }
                }

            } else {
                vars.forEach {
                    toAdd.put((it.fields["name"] as String) + ":Unkown", Pair(NodeType.SET_VAR, {
                        area.replaceText(area.caretPosition, area.caretPosition, "{" + it.fields["name"] + "}")
                    }))

                }
            }


            if (movedRight) {

                toAdd.forEach {
                    if (it.key.startsWith(beforeStr, true))
                        addItem(it.key, it.value.second)
                }
            } else {
                toAdd.forEach {
                    addItem(it.key, it.value.second)
                }

                if (fillList.items.size == 0) {
                    popUp.hide()
                    return
                }
            }


            val toRemove = Vector<ListHolderItem>()

            for (item in fillList.items) {
                if (!item.name.startsWith(currentWord, true)) {
                    println("Removed(create) " + item.name + " : " + currentWord)
                    toRemove.add(item)
                }
            }
            toRemove.forEach {
                println("removed " + it.name + " on create")
                fillList.items.remove(it)
                removed.add(it)
            }
            toRemove.clear()
            for (item in removed) {
                if (item.name.startsWith(currentWord, true)) {
                    toRemove.add(item)
                }
            }

            toRemove.forEach {
                removed.remove(it)
                fillList.items.add(it)
            }
            fillList.refresh()

        }
        popUp.show(project.openProject.guiHandler.window.stage)

    }

    fun showGlobalAutoGenerate(node: Node) {

        manager.parseResult = manager.parseStructure()
        val vars = Arrays.asList("test", "test")

        fillList.items.clear()
        removed.clear()

        addItem("Generate Command") {
            popUp.hide()
            val textPrompt = Prompts.textPrompt("Command Name", "Please type command name");
            val permission = Prompts.textPrompt("Command Permission", "Please type command permission");
            area.replaceText(area.caretPosition, area.caretPosition, "command /" + textPrompt + ":\n  permission: " + permission + "\n  trigger:\n    send \"hi\" to player")
            area.moveTo(area.caretPosition - 2)

            //  area.selectRange(area.caretPosition, area.caretPosition + 4)

        }
        addItem("Generate Event") {

            //  area.selectRange(area.caretPosition, area.caretPosition + 4)
            fillList.items.clear()
            for (s in Arrays.asList("Join", "Quit")) {
                addItem("On " + s, {
                    popUp.hide()
                    area.replaceText(area.caretPosition, area.caretPosition, "on " + s.toLowerCase() + ":\n")
                    area.moveTo(area.caretPosition - 1)

                })
            }


        }
        
        manager.parseResult.forEach {
            if (it.nodeType == NodeType.FUNCTION) {
                val name = it.fields["name"] as String
                val returnType = it.fields["return"] as String
                var paramsStr = ""
                var insertParams = ""
                (it.fields["params"] as Vector<MethodParameter>).forEach {
                    paramsStr += ",${it.name}:${it.type}"
                    insertParams += ",${it.name}"
                }
                paramsStr = paramsStr.substring(1)
                insertParams = insertParams.substring(1)
                val con = "FUNC: $name($paramsStr):$returnType"

                val insert = "$name($insertParams)"
                addItem(con, {
                    area.replaceText(area.caretPosition, area.caretPosition, insert)
                })

            }
        }



        popUp.show(project.openProject.guiHandler.window.stage)
    }

    fun showGlobalAutoComplete(node: Node) {

        manager.parseResult = manager.parseStructure()
        val vars = EditorUtils.filterByNodeType(NodeType.SET_VAR, manager.parseResult, node)

        fillList.items.clear()
        removed.clear()

        addItem("Command") {
            area.replaceText(area.caretPosition, area.caretPosition, "command / :")
            area.moveTo(area.caretPosition - 2)
            //  area.selectRange(area.caretPosition, area.caretPosition + 4)

        }
        addItem("Event") {
            area.replaceText(area.caretPosition, area.caretPosition, "on :")
            area.moveTo(area.caretPosition - 1)
            //  area.selectRange(area.caretPosition, area.caretPosition + 4)

        }
        addItem("Function") {
            area.replaceText(area.caretPosition, area.caretPosition, "function () :: :")
            area.moveTo(area.caretPosition - 7)
            //  area.selectRange(area.caretPosition, area.caretPosition + 4)

        }

        manager.parseResult.forEach {
            if (it.nodeType == NodeType.FUNCTION) {
                val name = it.fields["name"] as String
                val returnType = it.fields["return"] as String
                var paramsStr = ""
                var insertParams = ""
                (it.fields["params"] as Vector<MethodParameter>).forEach {
                    paramsStr += ",${it.name}:${it.type}"
                    insertParams += ",${it.name}"
                }
                paramsStr = paramsStr.substring(1)
                insertParams = insertParams.substring(1)
                val con = "FUNC: $name($paramsStr):$returnType"

                val insert = "$name($insertParams)"
                addItem(con, {
                    area.replaceText(area.caretPosition, area.caretPosition, insert)
                })

            }
        }

        vars.forEach {
            if (it.fields["visibility"] == "global") {
                addItem("VAR " + it.fields["name"], {
                    area.replaceText(area.caretPosition, area.caretPosition, "{" + it.fields["name"] + "}")
                })
            }
        }


        popUp.show(project.openProject.guiHandler.window.stage)
    }


    private fun onLineChange() {


        val parseResult = manager.parseStructure()
        manager.parseResult = parseResult

        val old = EditorUtils.getLineNode(lineBefore, parseResult)
        val current = EditorUtils.getLineNode(currentLine, parseResult)

        if (old != null && current != null) {

            if (old.linenumber == current.linenumber - 1) {
                // println("moved one down")
                if (current.nodeType == NodeType.UNDEFINED) {
                    val tabCount = old.tabLevel - 1
                    var str = ""
                    for (x in 0..tabCount) {
                        str += "\t"
                    }

                    if (old.nodeType != NodeType.EXECUTION && old.nodeType != NodeType.UNDEFINED && old.nodeType != NodeType.COMMENT && old.nodeType != NodeType.SET_VAR) str += "\t"
                    area.replaceText(area.caretPosition, area.caretPosition, str)
                    if (current.tabLevel == 0 && str.isEmpty()) Platform.runLater {
                        showGlobalAutoComplete(current)
                    } else {
                        wasJustCalled = true
                    }
                }
                return
            }
            if (old.linenumber == current.linenumber + 1) {
                // println("moved one up")


            }
        } else {

        }


    }


}