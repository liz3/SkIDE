package com.skide.core.code

import com.skide.core.code.autocomplete.AutoCompleteCompute
import com.skide.core.code.highlighting.Highlighting
import com.skide.core.skript.SkriptParser
import com.skide.include.Node
import com.skide.include.NodeType
import com.skide.include.OpenFileHolder
import com.skide.utils.EditorUtils
import com.skide.utils.getCaretLine
import com.skide.utils.readFile
import javafx.application.Platform
import javafx.scene.control.TreeItem
import javafx.scene.input.KeyCode
import org.fxmisc.richtext.CodeArea
import java.util.*
import org.fxmisc.wellbehaved.event.Nodes
import org.fxmisc.wellbehaved.event.EventPattern
import org.fxmisc.wellbehaved.event.InputMap


class CodeManager {

    lateinit var rootStructureItem: TreeItem<String>
    lateinit var area: CodeArea
    lateinit var content: String
    lateinit var autoComplete: AutoCompleteCompute
    lateinit var highlighter:Highlighting
    lateinit var parseResult: Vector<Node>

    private val parser = SkriptParser()


    fun setup(project: OpenFileHolder) {



        rootStructureItem = TreeItem(project.name)
        content = readFile(project.f).second
        area = project.area
        highlighter = Highlighting(this)
        if(this::highlighter.isInitialized) highlighter.computeHighlighting()

/*
        val im = InputMap.consume(
                EventPattern.keyTyped("\t"),
                { area.replaceSelection("    ") }
        )
        Nodes.addInputMap(area, im)
 */
        area.appendText(content)
        if (this::content.isInitialized && this::rootStructureItem.isInitialized) parseResult = parseStructure()
        autoComplete = AutoCompleteCompute(this, project)

        registerEvents()
    }

    private fun registerEvents() {

        area.setOnKeyPressed { ev ->
            if(ev.code == KeyCode.ESCAPE) {

                if(autoComplete.popUp.isShowing) autoComplete.popUp.hide()
            }

            if(ev.isControlDown) {
                if (ev.code == KeyCode.SPACE) {
                    if(!autoComplete.popUp.isShowing) {
                        parseResult = parseStructure()
                        val node = EditorUtils.getLineNode(area.getCaretLine(), parseResult)

                        if(node != null) {
                            if(node.tabLevel == 0)
                                autoComplete.showGlobalAutoComplete(node)
                            else
                                autoComplete.showLocalAutoComplete(false)

                        }
                    }
                }
            }
        }
        area.setOnMouseClicked { ev ->
            if(autoComplete.popUp.isShowing) autoComplete.popUp.hide()


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

        parseResult.forEach {
            addNodeToItemTree(rootStructureItem, it)
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