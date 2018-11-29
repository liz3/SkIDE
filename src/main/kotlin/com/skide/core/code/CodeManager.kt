package com.skide.core.code

import com.skide.core.code.autocomplete.AutoCompleteCompute
import com.skide.core.code.autocomplete.ReplaceSequence
import com.skide.core.code.highlighting.Highlighting
import com.skide.core.skript.SkriptParser
import com.skide.include.Node
import com.skide.include.NodeType
import com.skide.include.OpenFileHolder
import com.skide.skriptinsight.model.InspectionResultElement
import com.skide.utils.EditorUtils
import com.skide.utils.readFile
import javafx.application.Platform
import javafx.scene.control.TreeItem
import org.controlsfx.control.BreadCrumbBar
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
    lateinit var crossNodes: HashMap<String, Vector<Node>>
    lateinit var findHandler: FindHandler
    lateinit var replaceHandler: ReplaceHandler
    lateinit var definitonFinder: DefinitionsFinder
    lateinit var sequenceReplaceHandler: ReplaceSequence
    lateinit var tooltipHandler: TooltipHandler
    lateinit var hBox: BreadCrumbBar<Node>


    private val parser = SkriptParser()
    var marked = ConcurrentHashMap<Int, InspectionResultElement>()
    val ignored = HashMap<Int, String>()


    fun setup(project: OpenFileHolder) {


        rootStructureItem = TreeItem(project.name)
        content = readFile(project.f).second
        area = project.area
        findHandler = FindHandler(this, project)
        replaceHandler = ReplaceHandler(this, project)
        tooltipHandler = TooltipHandler(this, project)
        definitonFinder = DefinitionsFinder(this)
        if (project.coreManager.configManager.get("highlighting") == "true") {
            highlighter = Highlighting(this)

        }
        sequenceReplaceHandler = ReplaceSequence(this)
        hBox = project.currentStackBox


        if (project.coreManager.configManager.get("cross_auto_complete") == "true") {
            crossNodes = HashMap()
            loadCrossFileAutoComplete(project)
        }

        if (this::content.isInitialized && this::rootStructureItem.isInitialized) parseResult = parseStructure()
        autoComplete = AutoCompleteCompute(this, project)

        area.text = content

        area.view.focusedProperty().addListener { _, _, newValue ->

            if(!newValue) {
                project.manager.saveCode()
                project.openProject.runConfs.forEach {
                    if (it.value.runner === project) {
                        it.value.srv.setSkriptFile(project.name, area.text)
                    }
                }
            }
        }

        parseStructure()

    }

    private fun loadCrossFileAutoComplete(project: OpenFileHolder) {

        project.openProject.project.fileManager.projectFiles.values.forEach { f ->

            if (f.name.endsWith(".sk")) {

                if (project.openProject.guiHandler.openFiles.containsKey(f)) {
                    val openHolder = project.openProject.guiHandler.openFiles[f]
                    if (openHolder!!.codeManager != this) updateCrossFileAutoComplete(openHolder.f.name, openHolder.area.text)
                } else {
                    updateCrossFileAutoComplete(f.name, readFile(f).second)
                }
            }

        }

    }

    fun updateCrossFileAutoComplete(f: String, text: String) {
        if (!f.endsWith(".sk")) return
        val result = SkriptParser().superParse(text)
        if (!crossNodes.containsKey(f))
            crossNodes[f] = Vector()
        else
            crossNodes[f]!!.clear()

        result.forEach {
            if (it.nodeType == NodeType.FUNCTION) {
                crossNodes[f]!!.add(it)
            }
        }
        val vars = EditorUtils.filterByNodeType(NodeType.SET_VAR, result)
        vars.forEach {
            if (it.nodeType == NodeType.SET_VAR && it.fields["visibility"] == "global") {
                crossNodes[f]!!.add(it)
            }
        }


    }

    fun gotoItem(item: TreeItem<String>) {
        if (item == rootStructureItem) return
        val lineSearched = item.value.split(" ")[0].toInt()
        Platform.runLater {
            val length = area.getLineContent(lineSearched).length
            area.editor.call("revealLineInCenter", lineSearched)
            area.editor.call("setSelection", area.createObjectFromMap(hashMapOf(Pair("startLineNumber", lineSearched),
                    Pair("endLineNumber", lineSearched), Pair("startColumn", 0), Pair("endColumn", length + 1))))
        }
    }

    fun parseStructure(): Vector<Node> {

        rootStructureItem.children.clear()
        val parseResult = parser.superParse(area.text)
        Platform.runLater {
            val stack = Vector<Node>()
            var currNode: Node? = EditorUtils.getLineNode(area.getCurrentLine(), parseResult) ?: return@runLater

            stack.addElement(currNode)

            while (currNode != null) {
                if(!stack.contains(currNode))stack.add(currNode)
                if(currNode.parent == null) break
                currNode = currNode.parent!!
            }

            stack.reverse()

            val root = TreeItem<Node>(Node(null, area.openFileHolder.name, 0, 1))
            var item = root

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
            if(node.nodeType == NodeType.FUNCTION_CALL) {
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