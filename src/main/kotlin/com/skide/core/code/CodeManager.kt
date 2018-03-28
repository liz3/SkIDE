package com.skide.core.code

import com.skide.core.code.autocomplete.AutoCompleteCompute
import com.skide.core.code.autocomplete.ReplaceSequence
import com.skide.core.code.highlighting.Highlighting
import com.skide.core.skript.SkriptParser
import com.skide.gui.Menus
import com.skide.include.Node
import com.skide.include.NodeType
import com.skide.include.OpenFileHolder
import com.skide.utils.*
import javafx.application.Platform
import javafx.scene.control.Button
import javafx.scene.control.ContextMenu
import javafx.scene.control.TreeItem
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.HBox
import org.fxmisc.richtext.CodeArea

import java.util.*


class CodeManager {

    lateinit var rootStructureItem: TreeItem<String>
    lateinit var area: CodeArea
    lateinit var content: String
    lateinit var autoComplete: AutoCompleteCompute
    lateinit var highlighter: Highlighting
    lateinit var parseResult: Vector<Node>
    lateinit var findHandler: FindHandler
    lateinit var replaceHandler: ReplaceHandler
    lateinit var sequenceReplaceHandler: ReplaceSequence
    lateinit var hBox: HBox
    private val parser = SkriptParser()


    var contextMenu: ContextMenu? = null

    var mousePressed = false
    var cmdMacDown = false

    var lastPos = 0

    fun setup(project: OpenFileHolder) {

        rootStructureItem = TreeItem(project.name)
        content = readFile(project.f).second
        area = project.area
        findHandler = FindHandler(this, project)
        replaceHandler = ReplaceHandler(this, project)
        if (project.coreManager.configManager.get("highlighting") == "true") {
            highlighter = Highlighting(this)
            highlighter.computeHighlighting()
        }
        sequenceReplaceHandler = ReplaceSequence(this)
        hBox = project.currentStackBox


        ChangeWatcher(area, 2500, {

            println("stopped")

        }).start()

        area.appendText(content)
        if (this::content.isInitialized && this::rootStructureItem.isInitialized) parseResult = parseStructure()
        autoComplete = AutoCompleteCompute(this, project)

        registerEvents(project)

        area.moveTo(0)

    }

    private fun registerEvents(project: OpenFileHolder) {

        area.focusedProperty().addListener { _, _, newValue ->

            if (!newValue) {
                project.saveCode()
                project.openProject.runConfs.forEach {
                    if (it.value.runner === project) {
                        it.value.srv.setSkriptFile(project.name, area.text)
                    }
                }
            }
        }


        area.setOnKeyReleased {

            if(it.code == KeyCode.COMMAND) cmdMacDown = false
        }
        area.setOnKeyPressed { ev ->

            if(ev.code == KeyCode.COMMAND) cmdMacDown = true

            if (ev.code == KeyCode.TAB) {
                if (sequenceReplaceHandler.computing) {
                    ev.consume()
                    area.replaceText(area.caretPosition - 1, area.caretPosition, "")
                    sequenceReplaceHandler.fire()

                    return@setOnKeyPressed
                }
            }
            if (ev.code == KeyCode.ESCAPE) {

                if (autoComplete.popUp.isShowing) autoComplete.hideList()
                if (sequenceReplaceHandler.computing) sequenceReplaceHandler.cancel()
            }

            if (ev.code != KeyCode.SHIFT && ev.code != KeyCode.CONTROL && ev.code != KeyCode.ALT && ev.code != KeyCode.ALT_GRAPH) {

                var computed = true
                val startPos = if ((area.caretPosition - 1) == -1) 0 else area.caretPosition

                if (project.coreManager.configManager.get("bracket_cut") != null) {
                    val str = project.coreManager.configManager.get("bracket_cut") as String
                    val split = str.split("+")
                    split.forEach {
                        if (it == "CONTROL") {
                            if (!ev.isControlDown) {
                                computed = false
                                return@forEach
                            }
                        } else if (it == "ALT") {
                            if (!ev.isAltDown) {
                                computed = false
                                return@forEach
                            }
                        } else if (it == "SHIFT") {
                            if (!ev.isShiftDown) {
                                computed = false
                                return@forEach
                            }
                        } else {
                            if (ev.isControlDown && !str.contains("CONTROL+")) {
                                computed = false
                                return@forEach
                            }
                            if (ev.isShiftDown && !str.contains("SHIFT+")) {
                                computed = false
                                return@forEach
                            }
                            if (ev.isAltDown && !str.contains("ALT+")) {
                                computed = false
                                return@forEach
                            }
                            if (ev.code.toString() != it) {
                                computed = false
                            }
                        }
                    }
                } else {
                    computed = false
                }
                if (computed) {
                    ev.consume()
                    area.replaceText(startPos, area.caretPosition, "]")
                    area.moveTo(area.caretPosition - 1)
                    return@setOnKeyPressed
                }
                computed = true
                if (project.coreManager.configManager.get("curly_cut") != null) {
                    val str = (project.coreManager.configManager.get("curly_cut") as String)
                    val split = str.split("+")
                    split.forEach {
                        if (it == "CONTROL") {
                            if (!ev.isControlDown) {
                                computed = false
                                return@forEach
                            }
                        } else if (it == "ALT") {
                            if (!ev.isAltDown) {
                                computed = false
                                return@forEach
                            }
                        } else if (it == "SHIFT") {
                            if (!ev.isShiftDown) {
                                computed = false
                                return@forEach
                            }
                        } else {
                            if (ev.isControlDown && !str.contains("CONTROL+")) {
                                computed = false
                                return@forEach
                            }
                            if (ev.isShiftDown && !str.contains("SHIFT+")) {
                                computed = false
                                return@forEach
                            }
                            if (ev.isAltDown && !str.contains("ALT+")) {
                                computed = false
                                return@forEach
                            }
                            if (ev.code.toString() != it) {
                                computed = false
                            }
                        }
                    }
                } else {
                    computed = false
                }
                if (computed) {
                    ev.consume()
                    area.replaceText(startPos, area.caretPosition, "}")
                    area.moveTo(area.caretPosition - 1)
                    return@setOnKeyPressed
                }
                computed = true
                if (project.coreManager.configManager.get("paren_cut") != null) {
                    val str = (project.coreManager.configManager.get("paren_cut") as String)
                    val split = str.split("+")
                    split.forEach {
                        if (it == "CONTROL") {
                            if (!ev.isControlDown) {
                                computed = false
                                return@forEach
                            }
                        } else if (it == "ALT") {
                            if (!ev.isAltDown) {
                                computed = false
                                return@forEach
                            }
                        } else if (it == "SHIFT") {
                            if (!ev.isShiftDown) {
                                computed = false
                                return@forEach
                            }
                        } else {
                            if (ev.isControlDown && !str.contains("CONTROL+")) {
                                computed = false
                                return@forEach
                            }
                            if (ev.isShiftDown && !str.contains("SHIFT+")) {
                                computed = false
                                return@forEach
                            }
                            if (ev.isAltDown && !str.contains("ALT+")) {
                                computed = false
                                return@forEach
                            }
                            if (ev.code.toString() != it) {
                                computed = false
                            }
                        }
                    }
                } else {
                    computed = false
                }
                if (computed) {
                    ev.consume()
                    area.replaceText(startPos, area.caretPosition, ")")
                    area.moveTo(area.caretPosition - 1)
                    if (autoComplete.popUp.isShowing) {
                        autoComplete.removed.clear()
                        autoComplete.popUp.hide()
                        autoComplete.fillList.items.clear()
                    }
                    return@setOnKeyPressed
                }
                computed = true
                if (project.coreManager.configManager.get("quote_cut") != null) {
                    val str = (project.coreManager.configManager.get("quote_cut") as String)
                    val split = str.split("+")
                    split.forEach {
                        if (it == "CONTROL") {
                            if (!ev.isControlDown) {
                                computed = false
                                return@forEach
                            }
                        } else if (it == "ALT") {
                            if (!ev.isAltDown) {
                                computed = false
                                return@forEach
                            }
                        } else if (it == "SHIFT") {
                            if (!ev.isShiftDown) {
                                computed = false
                                return@forEach
                            }
                        } else {
                            if (ev.isControlDown && !str.contains("CONTROL+")) {
                                computed = false
                                return@forEach
                            }
                            if (ev.isShiftDown && !str.contains("SHIFT+")) {
                                computed = false
                                return@forEach
                            }
                            if (ev.isAltDown && !str.contains("ALT+")) {
                                computed = false
                                return@forEach
                            }
                            if (ev.code.toString() != it) {
                                computed = false
                            }
                        }
                    }
                } else {
                    computed = false
                }
                if (computed) {
                    ev.consume()
                    area.replaceText(startPos, area.caretPosition, "\"")
                    area.moveTo(area.caretPosition - 1)
                    return@setOnKeyPressed
                }
                computed = true
            }

            if(cmdMacDown && getOS() == OperatingSystemType.MAC_OS) {
                if (ev.code == KeyCode.C) area.copy()
                if (ev.code == KeyCode.Z) {

                    if(getLocale().contains("de")) area.redo() else area.undo()
                }

                if (ev.code == KeyCode.Y) {
                    if(getLocale().contains("de")) area.undo() else area.redo()

                }
                if (ev.code == KeyCode.V) area.paste()
                if (ev.code == KeyCode.F) findHandler.switchGui()
                if (ev.code == KeyCode.R) replaceHandler.switchGui()

                return@setOnKeyPressed
            }

            if (ev.isControlDown) {

                if (ev.code == KeyCode.SLASH) {
                    if (!autoComplete.popUp.isShowing) {
                        area.replaceSelection("#$content")
                    }
                }
                if (ev.code == KeyCode.C) {
                    area.copy()
                }

                if (ev.code == KeyCode.F) {
                    findHandler.switchGui()
                }
                if (ev.code == KeyCode.R) {
                    replaceHandler.switchGui()
                }
                if (ev.code == KeyCode.SPACE) {
                    if (!autoComplete.popUp.isShowing) {
                        parseResult = parseStructure()
                        val node = EditorUtils.getLineNode(area.getCaretLine(), parseResult)

                        if (node != null) {

                            if (node.raw.replace("\t", "").isEmpty() && area.caretColumn == 0)
                                autoComplete.showGlobalAutoComplete(node)
                            else
                                autoComplete.showLocalAutoComplete(false)

                        } else {

                        }
                    }
                }
            }
        }

        area.setOnMousePressed { ev ->

            if (lastPos == 0) lastPos = area.caretPosition
            mousePressed = true

            if (ev.isSecondaryButtonDown) {

                if (contextMenu == null) {
                    contextMenu = Menus.getMenuForArea(this, ev.screenX, ev.screenY)
                } else {
                    contextMenu!!.hide()
                    contextMenu = null
                }

            } else {
                if (contextMenu != null) {
                    contextMenu!!.hide()
                    contextMenu = null
                }
            }

        }
        area.setOnMouseReleased {
            Platform.runLater {
                mousePressed = false
            }
        }

        area.setOnMouseClicked { ev ->
            if (sequenceReplaceHandler.computing) {
                sequenceReplaceHandler.cancel()
                return@setOnMouseClicked

            }
            if (autoComplete.popUp.isShowing) {
                autoComplete.popUp.hide()
                return@setOnMouseClicked
            }


            if (ev.clickCount == 2) {
                area.selectWord()
                return@setOnMouseClicked
            }

            if (ev.clickCount == 3) {
                if(area.text.length < lastPos) return@setOnMouseClicked
                area.moveTo(lastPos)
                area.selectLine()
                lastPos = 0


            }

        }
    }


    fun gotoItem(item: TreeItem<String>) {

        if (item == rootStructureItem) return
        val lineSearched = item.value.split(" ")[0].toInt()
        val split = area.text.split("\n")
        val count = (0 until lineSearched - 1).sumBy { split[it].length + 1 }

        Platform.runLater {
            area.requestFocus()
            area.moveTo(count)
            //TODO still needs some adjustment
            area.scrollYToPixel((lineSearched * 14.95))
            area.selectLine()
        }
    }

    fun parseStructure(): Vector<Node> {

        rootStructureItem.children.clear()
        val parseResult = parser.superParse(area.text)

        Platform.runLater {
            val stack = Vector<Node>()
            var currNode: Node? = EditorUtils.getLineNode(area.getCaretLine(), parseResult) ?: return@runLater
            stack.addElement(currNode)

            while (currNode!!.parent != null) {
                stack.add(currNode.parent)
                currNode = currNode.parent!!
            }

            stack.reverse()

            hBox.children.clear()
            stack.forEach { node ->
                val b = Button(node.content)
                b.setPrefSize(80.0, 23.0)
                b.setOnAction {

                    val index = area.text.indexOf(node.raw);
                    if(index == -1) return@setOnAction
                    area.moveTo(index)
                    area.selectLine()
                    area.scrollYToPixel(node.linenumber * 14.95)

                }

                hBox.children.add(b)
            }
        }
        parseResult.forEach {
            if (it.nodeType != NodeType.UNDEFINED) addNodeToItemTree(rootStructureItem, it)
        }

        return parseResult
    }

    private fun addNodeToItemTree(parent: TreeItem<String>, node: Node) {


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
            addNodeToItemTree(item, it)
        }

        if (node.nodeType != NodeType.COMMENT) parent.children.add(item)
    }

}