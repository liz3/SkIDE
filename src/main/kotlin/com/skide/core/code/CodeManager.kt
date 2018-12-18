package com.skide.core.code

import com.skide.Info
import com.skide.core.code.autocomplete.AutoCompleteCompute
import com.skide.core.code.autocomplete.ReplaceSequence
import com.skide.core.skript.SkriptParser
import com.skide.include.Node
import com.skide.include.NodeType
import com.skide.include.OpenFileHolder
import com.skide.utils.EditorUtils
import com.skide.utils.readFile
import javafx.application.Platform
import javafx.scene.control.TreeItem
import org.controlsfx.control.BreadCrumbBar
import java.io.File
import java.util.*
import kotlin.collections.HashMap


class CodeManager {

    var isSetup = false
    lateinit var rootStructureItem: TreeItem<String>
    lateinit var area: CodeArea
    lateinit var content: String
    lateinit var autoComplete: AutoCompleteCompute
    lateinit var parseResult: Vector<Node>
    lateinit var crossNodes: HashMap<File, Vector<Node>>
    lateinit var definitonFinder: DefinitionsFinder
    lateinit var referenceProvider: ReferenceProvider
    lateinit var sequenceReplaceHandler: ReplaceSequence
    lateinit var tooltipHandler: TooltipHandler
    lateinit var errorProvider: ErrorProvider
    lateinit var hBox: BreadCrumbBar<Node>
    var gotoActivated = false


    private lateinit var parser: SkriptParser
    val ignored = HashMap<Int, String>()


    fun setup(project: OpenFileHolder) {

        area = project.area
        errorProvider = ErrorProvider(this)
        parser = SkriptParser(project.openProject)
        rootStructureItem = TreeItem(project.name)
        content = readFile(project.f).second

        tooltipHandler = TooltipHandler(this, project)
        definitonFinder = DefinitionsFinder(this)
        referenceProvider = ReferenceProvider(this)
        if (project.coreManager.configManager.get("highlighting") == "true") {
        }
        sequenceReplaceHandler = ReplaceSequence(this)
        hBox = project.currentStackBox
        if (project.coreManager.configManager.get("cross_auto_complete") == "true") {
            crossNodes = project.openProject.crossNodes
        }
        if (this::content.isInitialized && this::rootStructureItem.isInitialized)
            parseResult = parseStructure()
        autoComplete = AutoCompleteCompute(this, project)
        area.text = content
        area.view.focusedProperty().addListener { _, _, newValue ->
            if (!newValue) {
                project.manager.saveCode()
                project.openProject.runConfs.forEach {
                    if (it.value.runner === project) {
                        it.value.srv.setSkriptFile(project.name, area.text)
                    }
                }
            }
        }
        isSetup = true
    }

    fun gotoItem(item: TreeItem<String>) {
        if (item == rootStructureItem) return
        gotoActivated = true
        val lineSearched = item.value.split(" ")[0].toInt()
        Platform.runLater {
            val length = area.getLineContent(lineSearched).length
            area.editor.call("revealLineInCenter", lineSearched)
            area.editor.call("setSelection", area.createObjectFromMap(hashMapOf(Pair("startLineNumber", lineSearched),
                    Pair("endLineNumber", lineSearched), Pair("startColumn", 0), Pair("endColumn", length + 1))))
            Platform.runLater {
                gotoActivated = false
            }
        }
    }

    fun parseStructure(update: Boolean = true): Vector<Node> {

        val parseResult = if (update)
            parser.superParse(area.text)
        else
            this.parseResult
        rootStructureItem.children.clear()
        Platform.runLater {
            val stack = Vector<Node>()
            var currNode: Node? = EditorUtils.getLineNode(area.getCurrentLine(), parseResult) ?: return@runLater
            stack.addElement(currNode)
            while (currNode != null) {
                if (!stack.contains(currNode)) stack.add(currNode)
                if (currNode.parent == null) break
                currNode = currNode.parent!!
            }

            stack.reverse()
            var item = TreeItem<Node>(stack.first())
            stack.removeAt(0)
            stack.forEach {
                val next = TreeItem<Node>(it)
                item.children.add(next)
                item = next
            }
            hBox.selectedCrumb = item

        }
        parseResult.forEach {
            if (it.nodeType != NodeType.UNDEFINED) addNodeToItemTree(rootStructureItem, it)
        }

        if (update)
            Thread {
                errorProvider.runChecks(parseResult)
            }.start()


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
            if (node.nodeType == NodeType.FUNCTION_CALL) {
                name += ": ${node.fields["name"]}"
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