package com.skide.core.code

import com.skide.core.code.autocomplete.AutoCompleteCompute
import com.skide.core.code.autocomplete.ReplaceSequence
import com.skide.core.code.highlighting.Highlighting
import com.skide.core.skript.SkriptParser
import com.skide.gui.Menus
import com.skide.include.Node
import com.skide.include.NodeType
import com.skide.include.OpenFileHolder
import com.skide.skriptinsight.client.utils.InsightConstants
import com.skide.skriptinsight.model.InspectionResultElement
import com.skide.utils.*
import javafx.application.Platform
import javafx.scene.control.Button
import javafx.scene.control.ContextMenu
import javafx.scene.control.TreeItem
import javafx.scene.input.KeyCode
import javafx.scene.layout.HBox
import org.fxmisc.richtext.CodeArea
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.HashMap




class CodeManager {



    lateinit var rootStructureItem: TreeItem<String>
    lateinit var area: CodeArea
    lateinit var content: String
    lateinit var autoComplete: AutoCompleteCompute
    lateinit var highlighter: Highlighting
    lateinit var parseResult: Vector<Node>
    lateinit var crossNodes:HashMap<String, Vector<Node>>
    lateinit var findHandler: FindHandler
    lateinit var replaceHandler: ReplaceHandler
    lateinit var sequenceReplaceHandler: ReplaceSequence
    lateinit var tooltipHandler: TooltipHandler
    lateinit var hBox: HBox
    private val parser = SkriptParser()
    val marked = ConcurrentHashMap<Int, InspectionResultElement>()
    private var inspectionsDisabled = false

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
        tooltipHandler = TooltipHandler(this, project)
        if (project.coreManager.configManager.get("highlighting") == "true") {
            highlighter = Highlighting(this)
            highlighter.computeHighlighting()
        }
        sequenceReplaceHandler = ReplaceSequence(this)
        hBox = project.currentStackBox

        ChangeWatcher(area, 500, {
           if(project.coreManager.configManager.get("cross_auto_complete") == "true") {
               project.openProject.guiHandler.openFiles.values.forEach {
                  if(it.f.name != project.f.name) it.codeManager.updateCrossFileAutoComplete(project.f.name, area.text)
               }
           }

           if(!inspectionsDisabled) {

               println("Running inspections with SkriptInsight!")
               project.coreManager.insightClient.inspectScriptInAnotherThread(area.text, this)
           }


        }).start()
        if(project.coreManager.configManager.get("cross_auto_complete") == "true") {
            crossNodes = HashMap()
            loadCrossFileAutoComplete(project)
        }
        area.appendText(content)
        if (this::content.isInitialized && this::rootStructureItem.isInitialized) parseResult = parseStructure()
        autoComplete = AutoCompleteCompute(this, project)

        registerEvents(project)
        setupToolTip()


        area.moveTo(0)

    }
    private fun loadCrossFileAutoComplete(project: OpenFileHolder) {

        project.openProject.project.fileManager.projectFiles.values.forEach {f ->

            if(f.name.endsWith(".sk")) {

                if(project.openProject.guiHandler.openFiles.containsKey(f)) {
                    val openHolder = project.openProject.guiHandler.openFiles[f]
                    if(openHolder!!.codeManager != this) updateCrossFileAutoComplete(openHolder.f.name, openHolder.area.text)
                } else {
                    updateCrossFileAutoComplete(f.name, readFile(f).second)
                }
            }

        }

    }
    fun updateCrossFileAutoComplete(f:String, text:String) {
        if(!f.endsWith(".sk")) return
        val result = SkriptParser().superParse(text)
        if(!crossNodes.containsKey(f))
            crossNodes[f] = Vector()
        else
            crossNodes[f]!!.clear()

        result.forEach {
            if(it.nodeType == NodeType.FUNCTION) {
                crossNodes[f]!!.add(it)
            }
        }
        val vars = EditorUtils.filterByNodeType(NodeType.SET_VAR, result)
        vars.forEach {
            if(it.nodeType == NodeType.SET_VAR && it.fields["visibility"] == "global") { crossNodes[f]!!.add(it) }
        }


    }

    fun setupToolTip() {

        if(this::tooltipHandler.isInitialized) tooltipHandler.setup()

    }
    private fun registerEvents(project: OpenFileHolder) {

        area.focusedProperty().addListener { _, _, newValue ->

            if (!newValue) {
                autoComplete.hideList()
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

                ////////////////

                if (project.coreManager.configManager.get("fx_cut") != null) {
                    val str = project.coreManager.configManager.get("fx_cut") as String
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
                    val lForIndex =  area.getCaretLine() - 1

                    if(marked.containsKey(lForIndex)) {

                        Platform.runLater {
                            autoComplete.hideList()

                            autoComplete.addItem("Fix") {

                                val fixedInspection = marked[lForIndex]?.fixedInspection
                                val startPosition = area.getAbsolutePosition(lForIndex, if (fixedInspection?.length == 0 && lForIndex != 0) -1 else 0)
                                val endPosition = area.getAbsolutePosition(lForIndex + 1, if (lForIndex == 0) 0 else -1)

                                area.replaceText(startPosition, endPosition, fixedInspection)
                                marked.remove(lForIndex)
                                highlighter.runHighlighting()

                            }
                            autoComplete.addItem("Ignore for once") {

                                val typeFullName = project.coreManager.insightClient.getInspectionFromClass(marked[lForIndex]?.inspectionClass).typeName;
                                val typeClassName = typeFullName.substring(typeFullName.lastIndexOf('.') + 1)
                                val startPosition = area.getAbsolutePosition(lForIndex, 0)
                                val currentLine = area.getParagraph(lForIndex)
                                val whitespaces = currentLine.text.takeWhile(Char::isWhitespace)


                                area.insertText(startPosition, whitespaces + "#" + InsightConstants.Misc.DISABLE_ONCE_INSPECTION_PREFIX + typeClassName + System.lineSeparator())

                                marked.remove(lForIndex)
                                highlighter.runHighlighting()
                            }
                            autoComplete.addItem("Disable Inspections for current Session") {
                                inspectionsDisabled = true
                                marked.clear()
                            }
                            autoComplete.addItem("Disable inspections for project") {
                                //TODO
                            }


                            if (project.isExluded) {
                                autoComplete.popUp.show(project.externStage)

                            } else {
                                autoComplete.popUp.show(project.openProject.guiHandler.window.stage)
                            }
                            autoComplete.fillList.selectionModel.select(0)

                        }
                    }

                    return@setOnKeyPressed
                }
                computed = true

                ///////////////

                if (project.coreManager.configManager.get("ac_cut") != null) {
                    val str = project.coreManager.configManager.get("ac_cut") as String
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
                    if (!autoComplete.popUp.isShowing) {
                        val paragraph = area.paragraphs[area.getCaretLine() - 1]
                        if(paragraph.text.isEmpty()) {
                            autoComplete.showGlobalAutoComplete(EditorUtils.getLineNode(area.getCaretLine(), parseResult)!!)
                        } else {
                            if (paragraph.text.isBlank() && area.caretColumn == 0)
                                autoComplete.showGlobalAutoComplete(EditorUtils.getLineNode(area.getCaretLine(), parseResult)!!)
                            else
                                autoComplete.showLocalAutoComplete(false)
                        }



                    }
                    return@setOnKeyPressed
                }
                computed = true


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
            area.showParagraphAtTop(lineSearched)
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