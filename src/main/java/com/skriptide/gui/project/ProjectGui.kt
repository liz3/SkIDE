package com.skriptide.gui.project

import com.skriptide.core.management.OpenProject
import com.skriptide.gui.GuiManager
import com.skriptide.gui.Menus
import com.skriptide.gui.controllers.ProjectGuiController
import com.skriptide.include.OpenFileHolder
import javafx.application.Platform
import javafx.scene.control.*
import javafx.scene.input.MouseButton
import javafx.scene.layout.BorderPane
import org.fxmisc.richtext.CodeArea
import org.fxmisc.richtext.LineNumberFactory
import java.io.File


class OpenProjectGuiManager(val openProject: OpenProject) {

    val openFiles = HashMap<File, OpenFileHolder>()

    fun startGui(): ProjectGuiEventListeners {

        val window = GuiManager.getWindow("ProjectGui.fxml", openProject.project.name, false)
        val controller = window.controller as ProjectGuiController
        val eventManager = ProjectGuiEventListeners(this, controller)
        eventManager.guiReady = {
            window.stage.show()
        }
        eventManager.setup()

        return eventManager
    }

    val projectFiles = openProject.project.fileManager.projectFiles
}

class ProjectGuiEventListeners(private val openProjectGuiManager: OpenProjectGuiManager, private val controller: ProjectGuiController) {


    var guiReady = {}

    var contextMenuVisible: ContextMenu? = null


    val filesTab = {
        val tab = Tab("Project files")
        val treeView = TreeView<String>()
        //set the root item
        treeView.selectionModel.selectedItemProperty().addListener { observable, oldValue, newValue ->


            if(newValue == null) return@addListener
            val selectedItem = newValue as TreeItem<String>

            if (selectedItem != treeView.root) {

                if (openProjectGuiManager.projectFiles.containsKey(selectedItem.value)) {
                    openFile(openProjectGuiManager.projectFiles[selectedItem.value]!!)
                }
            }
        }

        tab.content = treeView
        Pair(tab, treeView)
    }.invoke()
    val structureTab = {
        val tab = Tab("Skript Structure")
        val treeView = TreeView<String>()


        tab.content = treeView
        tab.isDisable = true
        Pair(tab, treeView)
    }.invoke()


    fun setup() {


        replaceTemplateElements()
        registerBrowserEvents()
        registerEditorEvents()


        updateProjectFilesTreeView()
        guiReady()
    }

    private fun openFile(f: File) {

        if (openProjectGuiManager.openFiles.containsKey(f)) {

            for ((file, holder) in openProjectGuiManager.openFiles) {

                if (file == f) {
                    holder.tabPane.selectionModel.select(holder.tab)
                    break
                }
            }

            return
        }
        val holder = OpenFileHolder(f, f.name, Tab(f.name), controller.editorMainTabPane!!, BorderPane(), CodeArea())
        openProjectGuiManager.openFiles.put(f, holder)
        setupNewTabForDisplay(holder)
    }

    fun updateProjectFilesTreeView() {

        val rootItem = TreeItem<String>(openProjectGuiManager.openProject.project.name)
        filesTab.second.root = rootItem

        for ((name, _) in openProjectGuiManager.projectFiles) {
            val item = TreeItem<String>(name)
            rootItem.children.add(item)
        }

        rootItem.isExpanded = true

    }

    private fun setupNewTabForDisplay(holder: OpenFileHolder) {

        Platform.runLater {

            holder.tab.isClosable = true
            holder.borderPane.center = holder.area
            holder.tab.content = holder.borderPane
            holder.area.paragraphGraphicFactory = LineNumberFactory.get(holder.area)
            //setup the code management
            holder.codeManager.setup()
            registerEventsForNewFile(holder)
            holder.tabPane.tabs.add(holder.tab)



            holder.tabPane.selectionModel.select(holder.tab)
            updateStructureTab(holder)
        }

    }

    private fun registerEventsForNewFile(holder: OpenFileHolder) {

        holder.tab.setOnCloseRequest {
            holder.saveCode()
            openProjectGuiManager.openFiles.remove(holder.f)
            System.gc()
        }
    }

    private fun replaceTemplateElements() {

        val templateTab = controller.templateTab
        controller.editorMainTabPane!!.tabs.remove(templateTab)
        setupBrowser()


    }

    private fun updateStructureTab(holder: OpenFileHolder) {

        if(structureTab.first.isDisabled) structureTab.first.isDisable = false

        structureTab.second.root = holder.codeManager.rootStructureItem
    }

    private fun setupBrowser() {

        filesTab.first.onSelectionChangedProperty().addListener { observable, oldValue, newValue ->

            //TODO
        }

        filesTab.second.setOnMouseReleased { ev ->
            if (contextMenuVisible == null) {
                if (ev.button == MouseButton.SECONDARY) {
                    if(filesTab.second.selectionModel.selectedItem == null) return@setOnMouseReleased
                    val selectedItem = filesTab.second.selectionModel.selectedItem as TreeItem<String>



                    contextMenuVisible = if (selectedItem == filesTab.second.root) Menus.getMenuForRootProject(openProjectGuiManager.openProject, filesTab.second, ev.screenX, ev.screenY)
                    else
                        Menus.getMenuForProjectFile(openProjectGuiManager.openProject, openProjectGuiManager.projectFiles[selectedItem.value]!!, filesTab.second, ev.screenX, ev.screenY)


                }
            } else {
                contextMenuVisible!!.hide()
                contextMenuVisible = null
            }

        }

        controller.browserTabPane!!.tabs.addAll(filesTab.first, structureTab.first)



    }

    private fun registerBrowserEvents() {


    }

    private fun registerEditorEvents() {

        controller.editorMainTabPane!!.selectionModelProperty().addListener { observable, oldValue, newValue ->

            if(controller.editorMainTabPane!!.selectionModel.selectedItem != null) {
                val tab = controller.editorMainTabPane!!.selectionModel.selectedItem

                openProjectGuiManager.openFiles.values
                        .filter { it.tab == tab }
                        .forEach { updateStructureTab(it) }
            }

        }

    }


}