package com.skide.core.code.autocomplete

import com.skide.core.code.CodeManager
import com.skide.gui.GuiManager
import com.skide.gui.controllers.GenerateCommandController
import com.skide.include.*
import com.skide.utils.CurrentStateInfo
import com.skide.utils.EditorUtils
import com.skide.utils.getCaretLine
import com.skide.utils.getInfo
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.geometry.Bounds
import javafx.scene.control.ListView
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseEvent
import javafx.stage.Popup
import org.reactfx.EventStreams
import org.reactfx.EventStreams.nonNullValuesOf
import org.reactfx.value.Var
import java.util.*
import kotlin.collections.HashMap


class ListHolderItem(val name: String, val caller: (info: CurrentStateInfo) -> Unit, val description: String = "") {

    override fun toString(): String {
        return name
    }
}

class AutoCompleteCompute(val manager: CodeManager, val project: OpenFileHolder) {

    val area = manager.area

    var stopped = false

    var globalCompleteVisible = false

    val addonSupported = project.openProject.addons
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
                        if (!globalCompleteVisible) hideList()
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
                    value.caller.invoke(area.getInfo(manager))

                }
            }
        }

        fillList.setOnKeyPressed { ev ->
            if (ev.code == KeyCode.ESCAPE) {
                hideList()

            }
            if (ev.code == KeyCode.ENTER) {
                ev.consume()
                if (fillList.selectionModel.selectedItem != null) {
                    val value = fillList.selectionModel.selectedItem as ListHolderItem
                    value.caller.invoke(area.getInfo(manager))
                    hideList()
                }
            }


        }


    }


    private fun addItem(label: String, caller: (info: CurrentStateInfo) -> Unit) = fillList.items.add(ListHolderItem(label, caller))


    private fun registerEventListener() {

        area.caretPositionProperty().addListener { _, _, _ ->


            if (manager.mousePressed || stopped) {
                println("Returning")
                return@addListener
            }
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
        /*
         area.textProperty().addListener { observable, oldValue, newValue ->

         }
         */
    }

    private fun onColumnChange() {

        if (colPos == (coldPosOld + 1)) {
            if (wasJustCalled) {
                wasJustCalled = false
            } else {

                if (area.caretColumn == 0) {
                    val node = EditorUtils.getLineNode(area.getCaretLine(), manager.parseResult)
                    if (node?.tabLevel == 0) showGlobalAutoComplete(node)

                } else {

                    val curr = EditorUtils.getLineNode(currentLine, manager.parseResult)
                    if (globalCompleteVisible) {
                        showGlobalAutoComplete(curr!!)
                        return
                    }
                    if (curr?.content != "") showLocalAutoComplete(true) else manager.parseResult = manager.parseStructure()
                }


            }
        }
        if (colPos == (coldPosOld - 1)) {
            if (popUp.isShowing) {
                showLocalAutoComplete(true)
            }
        }
    }


    fun hideList() {
        globalCompleteVisible = false
        if (popUp.isShowing) {
            removed.clear()
            popUp.hide()
            fillList.items.clear()
            Platform.runLater {
                area.requestFocus()
            }
        }
    }

    fun showLocalAutoComplete(movedRight: Boolean) {


        println(currentLine)
        val currentInfo = area.getInfo(manager)

        if (globalCompleteVisible) {
            showGlobalAutoComplete(currentInfo.currentNode)
            return
        }

        if ((currentInfo.column - 2) >= 0) {

            if (currentInfo.actualCurrentString[currentInfo.column - 2] == ' ' && currentInfo.charBeforeCaret == " ") {
                hideList()
                return
            }
        }


        if (popUp.isShowing) {


            val replaced = getWordSearchReplace(currentInfo.currentWord, currentInfo)

            println("Char is ${currentInfo.charAfterCaret}")

            val toRemove = Vector<ListHolderItem>()

            fillList.items.filterNotTo(toRemove) { it.name.startsWith(replaced, true) }
            toRemove.forEach {
                fillList.items.remove(it)
                removed.add(it)
            }
            toRemove.clear()
            removed.filterTo(toRemove) { it.name.startsWith(replaced, true) }

            toRemove.forEach {
                removed.remove(it)
                fillList.items.add(it)
            }
            fillList.refresh()

            if (fillList.items.size == 0) {
                hideList()

            }

            return
        } else {
            println("Before is: " + currentInfo.beforeString)
            if (currentInfo.inString) return
            if (currentInfo.currentWord.endsWith("\"")) return
            if (currentInfo.currentWord.endsWith("{")) return
            if (currentInfo.currentNode.nodeType == NodeType.COMMENT) return
            if (currentInfo.currentWord.endsWith(":") ||
                    currentInfo.beforeString.endsWith(")") || currentInfo.beforeString.endsWith("(")) return
            manager.parseResult = manager.parseStructure()
            fillList.items.clear()
            removed.clear()
            val toAdd = HashMap<String, Pair<NodeType, (info: CurrentStateInfo) -> Unit>>()
            val root = EditorUtils.getRootOf(currentInfo.currentNode)


            val vars = EditorUtils.filterByNodeType(NodeType.SET_VAR, manager.parseResult, currentInfo.currentNode)

            if (root.nodeType == NodeType.FUNCTION && root.fields.contains("ready")) {
                val params = root.fields["params"] as Vector<*>

                params.forEach {
                    it as MethodParameter
                    toAdd["_" + it.name + " :" + it.type] = Pair(NodeType.SET_VAR, { info ->
                        area.replaceText(area.caretPosition - info.beforeString.length, area.caretPosition, "{_" + it.name + "}")
                    })
                }
            }
            if (root.nodeType == NodeType.EVENT || root.nodeType == NodeType.COMMAND) {
                toAdd["player:Player"] = Pair(NodeType.SET_VAR, { info ->
                    area.replaceText(area.caretPosition - info.beforeString.length, area.caretPosition, "player")
                })
            }
            manager.parseResult.forEach {
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
                    toAdd.put(con, Pair(NodeType.FUNCTION, { inf ->
                        area.replaceText(area.caretPosition - inf.beforeString.length, area.caretPosition, insert)
                    }))
                }
            }


            vars.forEach {
                if (!it.fields.containsKey("invalid")) {

                    if (it.fields.contains("from_option")) {
                        val found = fillList.items.any { c -> c.name == ((it.fields["name"] as String) + " [from option]") }
                        if (!found) {
                            addItem((it.fields["name"] as String) + " [from option]", { info ->
                                area.replaceText(area.caretPosition - info.beforeString.length, area.caretPosition, "{{@" + it.fields["name"] + "}::PATH}")

                                //TODO add path items as
                            })
                        }
                    } else {
                        val found = fillList.items.any { c -> c.name == (it.fields["name"] as String) }
                        if (!found) {
                            addItem({
                                if (it.fields["visibility"] == "global") {
                                    ""
                                } else {
                                    "_"
                                }
                            }.invoke() + it.fields["name"] as String, { info ->
                                if (it.fields["visibility"] == "global") {
                                    area.replaceText(area.caretPosition - info.beforeString.length, area.caretPosition, "{" + it.fields["name"] + "}")

                                } else {
                                    area.replaceText(area.caretPosition - info.beforeString.length, area.caretPosition, "{_" + it.fields["name"] + "}")
                                }
                            })
                        }
                    }
                }
            }

            addonSupported.values.forEach {
                it.forEach { item ->
                    if (item.type != DocType.EVENT) {

                        if (currentInfo.currentNode.nodeType == NodeType.FUNCTION && !currentInfo.inBrace && currentInfo.charAfterCaret == ":") {

                            if (item.type == DocType.TYPE) {
                                addItem("${item.name}:${item.type} - ${item.addon.name}", { currInfo ->
                                    var toRem = currInfo.beforeString
                                    toRem = toRem.replace(":", "")
                                    val adder = item.name
                                    area.replaceText(area.caretPosition - toRem.length, area.caretPosition, adder)
                                    manager.parseResult = manager.parseStructure()
                                })
                            }
                        } else if (currentInfo.currentNode.nodeType == NodeType.FUNCTION && currentInfo.inBrace) {
                            if (item.type == DocType.TYPE) {
                                addItem("${item.name}:${item.type} - ${item.addon.name}", { currInfo ->
                                    var toRem = currInfo.beforeString
                                    toRem = toRem.replace(")", "")
                                    if (toRem.contains(":")) {
                                        toRem = toRem.split(":")[1]
                                    }
                                    val adder = item.name
                                    area.replaceText(area.caretPosition - toRem.length, area.caretPosition, adder)
                                    manager.parseResult = manager.parseStructure()
                                })
                            }

                        } else {
                            addItem("${item.name}:${item.type} - ${item.addon.name}", { currInfo ->
                                val toRem = currInfo.beforeString.length
                                var adder = (if (item.pattern == "") item.name.toLowerCase() else item.pattern).replace("\n", "")
                                if (item.type == DocType.CONDITION) if (!currInfo.actualCurrentString.contains("if ")) adder = "if $adder"
                                adder += ":"
                                area.replaceText(area.caretPosition - toRem, area.caretPosition, adder)
                                manager.parseResult = manager.parseStructure()

                                manager.sequenceReplaceHandler.compute(area.getInfo(manager))
                            })
                        }


                    }
                }
            }
            if (movedRight) {
                toAdd.forEach {
                    if (it.key.startsWith(currentInfo.beforeString, true))
                        addItem(it.key, it.value.second)
                }
            } else {
                toAdd.forEach {
                    addItem(it.key, it.value.second)
                }
                if (fillList.items.size == 0) {
                    hideList()
                    return
                }
            }
            val toRemove = Vector<ListHolderItem>()
            fillList.items.filterNotTo(toRemove) { it.name.startsWith(currentInfo.currentWord, true) }
            toRemove.forEach {
                fillList.items.remove(it)
                removed.add(it)
            }
            toRemove.clear()
            removed.filterTo(toRemove) { it.name.startsWith(currentInfo.currentWord, true) }

            toRemove.forEach {
                removed.remove(it)
                fillList.items.add(it)
            }
            fillList.refresh()

        }
        if(fillList.items.size == 0) return
        popUp.show(project.openProject.guiHandler.window.stage)
        fillList.selectionModel.select(0)

    }


    fun showGlobalAutoComplete(node: Node) {


        if (!popUp.isShowing) {
            area.replaceText(area.caretPosition, area.caretPosition + node.raw.length, "")

            manager.parseResult = manager.parseStructure()
            //   if (area.getInfo(manager, currentLine).inString) return

            val vars = EditorUtils.filterByNodeType(NodeType.SET_VAR, manager.parseResult, node)

            fillList.items.clear()
            removed.clear()

            addItem("Function") {
                area.replaceText(area.caretPosition, area.caretPosition, "function () :: :")
                area.moveTo(area.caretPosition - 7)
            }

            addItem("Generate Command") {
                popUp.hide()

                val window = GuiManager.getWindow("GenerateCommand.fxml", "Generate command", true)
                val generate: GenerateCommandController = window.controller as GenerateCommandController;
                generate.createButton.onMouseClicked = EventHandler<MouseEvent> { _ ->
                    run {
                        area.replaceText(area.caretPosition, area.caretPosition, "command /" + generate.commandNameField.text + ":\n\tdescription: " + generate.descriptionField.text + "\n" + "\tpermission: " + generate.permissionField.text + "\n\ttrigger:\n\t\tsend \"hi\" to player")

                        GuiManager.closeGui(window.id);
                    }
                }

            }

            addItem("Generate Event") {


                fillList.items.clear()
                removed.clear()
                for (s in Arrays.asList("Join", "Quit")) {
                    addItem("On $s", {
                        hideList()
                        area.moveTo(area.caretPosition - it.actualCurrentString.length)
                        area.replaceText(area.caretPosition, area.caretPosition + it.actualCurrentString.length, "on " + s.toLowerCase() + ":\n\t")
                        area.moveTo(area.caretPosition - 1)
                    })
                }
                addonSupported.values.forEach {
                    it.forEach { item ->
                        if (item.type == DocType.EVENT) {

                            addItem(item.name + " Addon: ${item.addon.name}", { info ->
                                hideList()
                                area.replaceText(area.caretPosition - info.actualCurrentString.length, area.caretPosition, {
                                    var text = item.pattern
                                    if (text.isEmpty()) text = item.name
                                    text
                                }.invoke().replace("[on]", "on") + ":\n")
                                println(item)
                            })
                        }
                    }
                }
            }

            globalCompleteVisible = true
            popUp.show(project.openProject.guiHandler.window.stage)
            fillList.selectionModel.select(0)

        } else {

            val currentInfo = area.getInfo(manager)

            val toRemove = Vector<ListHolderItem>()

            fillList.items.filterNotTo(toRemove) { it.name.startsWith(currentInfo.actualCurrentString, true) }
            toRemove.forEach {
                fillList.items.remove(it)
                removed.add(it)
            }
            toRemove.clear()
            removed.filterTo(toRemove) { it.name.startsWith(currentInfo.actualCurrentString, true) }

            toRemove.forEach {
                removed.remove(it)
                fillList.items.add(it)
            }
            fillList.refresh()

            if (fillList.items.size == 0) {
                hideList()

            }

        }
    }


    private fun getWordSearchReplace(replaced: String, currentInfo: CurrentStateInfo): String {

        var replaced = replaced

        if (currentInfo.currentNode.nodeType == NodeType.FUNCTION && currentInfo.inBrace) {
            replaced = replaced.replace(")", "")
            if (replaced.contains(":")) {
                replaced = replaced.split(":")[1]
            }
        } else if (currentInfo.currentNode.nodeType == NodeType.FUNCTION && !currentInfo.inBrace && currentInfo.charAfterCaret == ":") {
            replaced = replaced.replace(":", "")
        }

        return replaced
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
                }
                return
            }
            if (old.linenumber == current.linenumber + 1) {
                if (popUp.isShowing) {
                    hideList()
                }
            }
        }
    }
}