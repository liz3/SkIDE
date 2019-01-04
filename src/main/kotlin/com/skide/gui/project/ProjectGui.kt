package com.skide.gui.project

import com.skide.CoreManager
import com.skide.core.code.CodeArea
import com.skide.core.management.ExternalHandler
import com.skide.core.management.OpenProject
import com.skide.core.skript.SkriptParser
import com.skide.gui.*
import com.skide.gui.controllers.*
import com.skide.gui.settings.SettingsGUIHandler
import com.skide.include.EditorMode
import com.skide.include.NodeType
import com.skide.include.OpenFileHolder
import com.skide.utils.*
import javafx.application.Platform
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseButton
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle
import java.io.File
import java.util.*
import java.util.regex.Pattern


class OpenProjectGuiManager(val openProject: OpenProject, val coreManager: CoreManager) {


    var mode = EditorMode.NORMAL
    val openFiles = HashMap<File, OpenFileHolder>()
    val settings = SettingsGui(coreManager, this)
    val window = GUIManager.getWindow("fxml/ProjectGui.fxml", openProject.project.name, false, w = 1280.0, h = 720.0)
    lateinit var lowerTabPaneEventManager: LowerTabPaneEventManager
    val otherTabPanes = Vector<TabPane>()
    var paneHolderNode: Node = HBox()
    var draggedTab: Tab? = null
    var lastActive: OpenFileHolder? = null
    var dragDone = false
    var activeTab = Tab()


    fun startGui(): ProjectGuiEventListeners {
        val controller = window.controller as ProjectGUIController
        val eventManager = ProjectGuiEventListeners(this, controller, coreManager)
        eventManager.guiReady = {
            window.stage.show()
        }
        window.closeListener = {
            closeHook()
        }
        eventManager.setup()
        lowerTabPaneEventManager = LowerTabPaneEventManager(controller, this, coreManager)
        lowerTabPaneEventManager.setup()
        eventManager.filesTab.second.requestFocus()
        return eventManager
    }

    fun switchMode(mode: EditorMode) {
        if (mode == this.mode) return
        val modeBefore = this.mode
        if (mode == EditorMode.NORMAL) {
            if (modeBefore != EditorMode.NORMAL) {
                val mainTabPane = openProject.eventManager.controller.editorMainTabPane
                val root = openProject.eventManager.controller.mainCenterAnchorPane
                otherTabPanes.forEach {
                    mainTabPane.tabs.addAll(it.tabs)
                }
                otherTabPanes.clear()
                root.children.clear()
                root.children.add(mainTabPane)
                AnchorPane.setTopAnchor(mainTabPane, 0.0)
                AnchorPane.setRightAnchor(mainTabPane, 0.0)
                AnchorPane.setBottomAnchor(mainTabPane, 0.0)
                AnchorPane.setLeftAnchor(mainTabPane, 0.0)
                this.mode = EditorMode.NORMAL
            }
        }
        if (mode == EditorMode.SIDE_SPLIT) {
            otherTabPanes.clear()
            val box = HBox()
            box.layoutBoundsProperty().addListener { _, _, _ ->
                val total = box.width
                val panesHeight = total / otherTabPanes.size
                box.children.forEach {
                    it as TabPane
                    it.setPrefSize(panesHeight, box.height)
                }
            }

            AnchorPane.setTopAnchor(box, 0.0)
            AnchorPane.setRightAnchor(box, 0.0)
            AnchorPane.setBottomAnchor(box, 0.0)
            AnchorPane.setLeftAnchor(box, 0.0)
            val mainTabPane = openProject.eventManager.controller.editorMainTabPane
            val root = openProject.eventManager.controller.mainCenterAnchorPane
            val basePane = TabPane()
            MouseDragHandler(basePane, this).setup()
            val vtabs = Vector<Tab>()
            mainTabPane.tabs.forEach {
                vtabs.add(it)
            }
            mainTabPane.tabs.clear()
            root.children.clear()
            root.children.add(box)
            box.children.add(basePane)

            otherTabPanes.addElement(basePane)
            this.mode = EditorMode.SIDE_SPLIT
            paneHolderNode = box
            vtabs.forEach {
                basePane.tabs.add(it)
            }
        }
        if (mode == EditorMode.DOWN_SPLIT) {
            otherTabPanes.clear()
            val box = VBox()
            box.layoutBoundsProperty().addListener { _, _, _ ->
                val total = box.height
                val panesHeight = total / otherTabPanes.size
                box.children.forEach {
                    it as TabPane
                    it.setPrefSize(box.width, panesHeight)
                }
            }
            AnchorPane.setTopAnchor(box, 0.0)
            AnchorPane.setRightAnchor(box, 0.0)
            AnchorPane.setBottomAnchor(box, 0.0)
            AnchorPane.setLeftAnchor(box, 0.0)
            val mainTabPane = openProject.eventManager.controller.editorMainTabPane
            val root = openProject.eventManager.controller.mainCenterAnchorPane
            val basePane = TabPane()
            MouseDragHandler(basePane, this).setup()
            val vtabs = Vector<Tab>()
            mainTabPane.tabs.forEach {
                vtabs.add(it)
            }
            mainTabPane.tabs.clear()
            box.children.add(basePane)
            root.children.clear()
            root.children.add(box)
            otherTabPanes.addElement(basePane)
            this.mode = EditorMode.DOWN_SPLIT
            paneHolderNode = box
            vtabs.forEach {
                basePane.tabs.add(it)
            }
        }
    }

    fun addTabPane(tab: Tab) {
        if (mode == EditorMode.NORMAL) return
        otherTabPanes.forEach {
            if (it.tabs.contains(tab)) it.tabs.remove(tab)
        }
        val tabPane = TabPane()
        MouseDragHandler(tabPane, this).setup()
        if (mode == EditorMode.SIDE_SPLIT) {
            tabPane.tabs.add(tab)
            val box = this.paneHolderNode as HBox
            otherTabPanes.addElement(tabPane)
            box.children.add(tabPane)
            tabPane.selectionModel.selectedItemProperty().addListener { _, _, _ ->
                if (coreManager.configManager.get("cross_auto_complete") == "true")
                    coreManager.projectManager.openProjects.forEach {
                        it.updateCrossNodes()
                    }

                if (tabPane.tabs.size == 0) {
                    box.children.remove(tabPane)
                    otherTabPanes.remove(tabPane)
                    val total = box.width
                    val panesHeight = total / otherTabPanes.size
                    box.children.forEach {
                        it as TabPane
                        it.setPrefSize(panesHeight, box.height)
                    }
                }
                if (otherTabPanes.size == 1) {
                    val mainTabPane = openProject.eventManager.controller.editorMainTabPane
                    val root = openProject.eventManager.controller.mainCenterAnchorPane
                    otherTabPanes.forEach {
                        mainTabPane.tabs.addAll(it.tabs)
                    }
                    otherTabPanes.clear()
                    root.children.clear()
                    root.children.add(mainTabPane)
                    AnchorPane.setTopAnchor(mainTabPane, 0.0)
                    AnchorPane.setRightAnchor(mainTabPane, 0.0)
                    AnchorPane.setBottomAnchor(mainTabPane, 0.0)
                    AnchorPane.setLeftAnchor(mainTabPane, 0.0)
                    this.mode = EditorMode.NORMAL
                }
            }
            val total = box.width
            val panesHeight = total / otherTabPanes.size
            box.children.forEach {
                it as TabPane
                it.setPrefSize(panesHeight, box.height)
            }

        }
        if (mode == EditorMode.DOWN_SPLIT) {
            Platform.runLater {
                tabPane.tabs.add(tab)
                val box = this.paneHolderNode as VBox
                otherTabPanes.addElement(tabPane)
                box.children.add(tabPane)
                tabPane.selectionModel.selectedItemProperty().addListener { _, _, _ ->
                    if (coreManager.configManager.get("cross_auto_complete") == "true") {
                        coreManager.projectManager.openProjects.forEach {
                            it.updateCrossNodes()
                        }
                    }
                    if (tabPane.tabs.size == 0) {
                        box.children.remove(tabPane)
                        otherTabPanes.remove(tabPane)

                        val total = box.height
                        val panesHeight = total / otherTabPanes.size
                        box.children.forEach {
                            it as TabPane
                            it.setPrefSize(box.width, panesHeight)
                        }
                    }
                    if (otherTabPanes.size == 1) {
                        val mainTabPane = openProject.eventManager.controller.editorMainTabPane
                        val root = openProject.eventManager.controller.mainCenterAnchorPane
                        otherTabPanes.forEach {
                            mainTabPane.tabs.addAll(it.tabs)
                        }
                        otherTabPanes.clear()
                        root.children.clear()
                        root.children.add(mainTabPane)
                        AnchorPane.setTopAnchor(mainTabPane, 0.0)
                        AnchorPane.setRightAnchor(mainTabPane, 0.0)
                        AnchorPane.setBottomAnchor(mainTabPane, 0.0)
                        AnchorPane.setLeftAnchor(mainTabPane, 0.0)
                        this.mode = EditorMode.NORMAL
                    }
                }
                val total = box.height
                val panesHeight = total / otherTabPanes.size
                box.children.forEach {
                    it as TabPane
                    it.setPrefSize(box.width, panesHeight)
                }
            }
        }
    }


    fun closeHook() {
        openProject.project.fileManager.lastOpen.clear()
        openFiles.values.forEach {
            it.manager.saveCode()
            openProject.project.fileManager.lastOpen.addElement(it.f.name)
        }
        openFiles.clear()
        openProject.project.fileManager.rewriteConfig()
        if (openProject.runConfs.size != 0) {
            Thread {
                val am = openProject.runConfs.size
                openProject.runConfs.forEach {
                    it.value.srv.kill()
                }
                Platform.runLater {
                    Prompts.infoCheck("Stopped Server", "Running servers had to be stopped", "Sk-IDE stopped $am server", Alert.AlertType.INFORMATION)
                }
            }.start()
        }
        coreManager.projectManager.openProjects.remove(this.openProject)
    }

    val projectFiles = openProject.project.fileManager.projectFiles
}

class ProjectGuiEventListeners(private val openProjectGuiManager: OpenProjectGuiManager, val controller: ProjectGUIController, val coreManager: CoreManager) {

    private var isPreviewActive = false
    var browserVisible = true
    private lateinit var previewPanel: Parent
    var guiReady = {}
    private var contextMenuVisible: ContextMenu? = null
    private val mouseDragHandler = MouseDragHandler(controller.editorMainTabPane, this.openProjectGuiManager)
    private val bufferedFiles = Vector<File>()

    val filesTab = {
        val tab = Tab()
        val iconView = ImageView(Image(javaClass.getResource("/images/files_main.png").toExternalForm()))
        tab.graphic = iconView
        val treeView = TreeView<String>()

        treeView.setOnMouseClicked {
            if (it.button == MouseButton.PRIMARY && it.clickCount == 2) {
                val newValue = treeView.selectionModel.selectedItem
                if (newValue != null) {

                    val selectedItem = newValue as TreeItem<String>
                    if (selectedItem != treeView.root) {
                        if (openProjectGuiManager.projectFiles.containsKey(selectedItem.value)) {
                            Thread {
                                openFile(openProjectGuiManager.projectFiles[selectedItem.value]!!)
                            }.start()
                        }
                    }
                }
            }
        }
        //set the root item
        tab.content = treeView
        Pair(tab, treeView)
    }.invoke()
    private val structureTab = {
        val tab = Tab()
        val iconView = ImageView(Image(javaClass.getResource("/images/file_skriptstructure.png").toExternalForm()))

        tab.graphic = iconView
        val treeView = TreeView<String>()
        tab.content = treeView
        tab.isDisable = true
        Pair(tab, treeView)
    }.invoke()


    fun setupPreview() {
        if (isPreviewActive) return
        isPreviewActive = true
        controller.mainCenterAnchorPane.children.remove(controller.editorMainTabPane)
        if (!this::previewPanel.isInitialized) {
            previewPanel = GUIManager.getScene("fxml/PreviewPanel.fxml").first
            mouseDragHandler.registerPreviewPane(previewPanel as VBox)
        }

        AnchorPane.setTopAnchor(previewPanel, 0.0)
        AnchorPane.setRightAnchor(previewPanel, 0.0)
        AnchorPane.setBottomAnchor(previewPanel, 0.0)
        AnchorPane.setLeftAnchor(previewPanel, 0.0)
        controller.mainCenterAnchorPane.children.add(previewPanel)
    }

    fun disablePreview() {
        if (!isPreviewActive) return
        isPreviewActive = false
        if (this::previewPanel.isInitialized) {
            controller.mainCenterAnchorPane.children.remove(previewPanel)
            controller.mainCenterAnchorPane.children.add(controller.editorMainTabPane)
        }

    }

    fun setup() {
        replaceTemplateElements()
        registerBrowserEvents()
        registerEditorEvents()
        setupMainMenu()
        updateProjectFilesTreeView()
        mouseDragHandler.setup()
        guiReady()
        if (openProjectGuiManager.openProject.project.fileManager.lastOpen.size == 0) {
            setupPreview()
        }
        openProjectGuiManager.openProject.project.fileManager.lastOpen.forEach {
            Platform.runLater {
                openFile(openProjectGuiManager.openProject.project.fileManager.projectFiles[it]!!)

            }
        }
    }


    fun openFile(f: File, isExternal: Boolean = false, cb: (OpenFileHolder) -> Unit = {}) {
        if (bufferedFiles.size > 0) return
        bufferedFiles.addElement(f)
        if (openProjectGuiManager.openFiles.containsKey(f)) {
            for ((file, holder) in openProjectGuiManager.openFiles) {
                if (file === f) {
                    holder.tabPane.selectionModel.select(holder.tab)
                    break
                }
            }
            bufferedFiles.remove(f)
            return
        }

        Platform.runLater {
            disablePreview()
            CodeArea(coreManager, f) {
                try {
                    val holder = OpenFileHolder(openProjectGuiManager.openProject, f, f.name, Tab(f.name), if (openProjectGuiManager.mode == EditorMode.NORMAL) controller.editorMainTabPane else openProjectGuiManager.otherTabPanes.firstElement(), BorderPane(), it, coreManager, isExternal = isExternal)
                    it.openFileHolder = holder
                    it.codeManager = holder.codeManager
                    openProjectGuiManager.openFiles[f] = holder
                    setupNewTabForDisplay(holder)
                    cb(holder)
                    bufferedFiles.remove(f)
                }catch (e:Exception) {
                    e.printStackTrace()
                }

            }

        }
    }

    fun openFile(f: File, tabPane: TabPane, isExternal: Boolean = false) {
        if (bufferedFiles.size > 0) return
        bufferedFiles.addElement(f)
        if (openProjectGuiManager.openFiles.containsKey(f)) {
            for ((file, holder) in openProjectGuiManager.openFiles) {
                if (file === f) {
                    holder.tabPane.selectionModel.select(holder.tab)
                    break
                }
            }
            bufferedFiles.remove(f)

            return
        }

        Platform.runLater {
            disablePreview()
            CodeArea(coreManager, f) {
                val holder = OpenFileHolder(openProjectGuiManager.openProject, f, f.name, Tab(f.name), tabPane, BorderPane(), it, coreManager, isExternal = isExternal)
                it.openFileHolder = holder
                it.codeManager = holder.codeManager
                openProjectGuiManager.openFiles[f] = holder
                setupNewTabForDisplay(holder)
                bufferedFiles.remove(f)

            }

        }
    }

    fun updateProjectFilesTreeView() {

        val rootItem = TreeItem<String>(openProjectGuiManager.openProject.project.name)
        filesTab.second.root = rootItem

        for ((name, _) in openProjectGuiManager.projectFiles) {
            val item = TreeItem<String>(name)
            if (name.endsWith(".sk")) {
                val openIcon = Image(javaClass.getResource("/images/sk.png").toExternalForm())
                val openView = ImageView(openIcon)
                openView.fitWidth = 15.0
                openView.fitHeight = 15.0
                item.graphic = openView
            }
            if (name.endsWith(".yml")) {
                val openIcon = Image(javaClass.getResource("/images/yaml.png").toExternalForm())
                val openView = ImageView(openIcon)
                openView.fitWidth = 25.0
                openView.fitHeight = 15.0
                item.graphic = openView
            }
            rootItem.children.add(item)
        }

        rootItem.isExpanded = true

    }

    private fun setupNewTabForDisplay(holder: OpenFileHolder) {

        Platform.runLater {
            holder.tab.isClosable = true
            holder.borderPane.bottom = holder.currentStackBox
            holder.borderPane.center = holder.area.view
            holder.currentStackBox.prefHeight = 35.0
            holder.tab.selectedProperty().addListener { _, _, newValue ->
                if (newValue) {
                    openProjectGuiManager.activeTab = holder.tab
                }
            }
            holder.tab.content = holder.borderPane
            holder.tab.contextMenu = Menus.getMenuForRootPane(holder)
            if (holder.name.endsWith(".sk")) holder.codeManager.setup(holder) else ExternalHandler(holder)
            registerEventsForNewFile(holder)
            holder.tabPane.tabs.add(holder.tab)
            holder.tabPane.selectionModel.select(holder.tab)
            if (holder.name.endsWith(".sk")) updateStructureTab(holder)
        }

    }


    fun registerEventsForNewFile(holder: OpenFileHolder) {

        holder.tab.setOnCloseRequest {
            holder.manager.saveCode()
            openProjectGuiManager.openFiles.remove(holder.f)
            System.gc()
            if (openProjectGuiManager.openFiles.size == 0) {
                controller.browserTabPane.selectionModel.select(0)
                structureTab.first.isDisable = true
                setupPreview()
            }
        }
    }

    private fun replaceTemplateElements() {

        val templateTab = controller.templateTab
        controller.editorMainTabPane.tabs.remove(templateTab)
        setupBrowser()


    }

    private fun updateStructureTab(holder: OpenFileHolder) {

        if (structureTab.first.isDisabled) structureTab.first.isDisable = false

        Platform.runLater {
            structureTab.second.root = holder.codeManager.rootStructureItem
        }
    }


    private fun setupBrowser() {

        filesTab.first.onSelectionChangedProperty().addListener { _, _, _ ->

            //TODO
        }

        filesTab.second.setOnMouseReleased { ev ->
            if (contextMenuVisible == null) {
                if (ev.button == MouseButton.SECONDARY) {
                    if (filesTab.second.selectionModel.selectedItem == null) return@setOnMouseReleased
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
        controller.browserTabPane.style = "-fx-tab-min-width:5px;"
        controller.browserTabPane.tabs.addAll(filesTab.first, structureTab.first)
        controller.browserTabPane.id = "sideTabArea"
        controller.activeSideLabel.text = "Files"
        controller.browserTabPane.selectionModel.selectedItemProperty().addListener { _, _, newTab ->
            if (newTab == filesTab.first)
                controller.activeSideLabel.text = "Files"
            else
                controller.activeSideLabel.text = "Structure"
        }
    }

    private fun setupMainMenu() {

        if (getOS() == OperatingSystemType.MAC_OS) controller.mainBenuBar.useSystemMenuBarProperty().set(true)
        val fileMenu = controller.mainBenuBar.menus[0]
        val editMenu = controller.mainBenuBar.menus[1]
        val closeItem = fileMenu.items.first()
        fileMenu.items.remove(closeItem)

        controller.mainBenuBar.menus[2].items[0].setOnAction {
            GUIManager.showAbout()
        }


        val skUnity = MenuItem("Log in to SkUnity")
        skUnity.setOnAction {
            if (coreManager.skUnity.login()) controller.mainBenuBar.menus[2].items.remove(skUnity)
        }
        controller.mainBenuBar.menus[2].items.add(skUnity)

        editMenu.items.add(simpleMenuItem("Find") {
            openProjectGuiManager.openFiles.values.forEach {
                if (it.tab === openProjectGuiManager.activeTab)
                    it.area.triggerAction("actions.find")

            }
        })
        editMenu.items.add(simpleMenuItem("Find/Replace") {
            openProjectGuiManager.openFiles.values.forEach {
                if (it.tab === openProjectGuiManager.activeTab)
                    it.area.triggerAction("editor.action.startFindReplaceAction")

            }
        })
        editMenu.items.add(simpleMenuItem("Undo") {
            openProjectGuiManager.openFiles.values.forEach {
                if (it.tab === openProjectGuiManager.activeTab)
                    it.area.triggerAction("undo")

            }
        })
        editMenu.items.add(simpleMenuItem("Redo") {
            openProjectGuiManager.openFiles.values.forEach {
                if (it.tab === openProjectGuiManager.activeTab)
                    it.area.triggerAction("redo")

            }
        })

        val otherProjects = Menu("Other projects")
        closeItem.setOnAction {
            openProjectGuiManager.window.close()
            openProjectGuiManager.closeHook()
        }
        val closeProjectItem = MenuItem("Close Project")
        closeProjectItem.setOnAction {
            openProjectGuiManager.closeHook()
            openProjectGuiManager.window.close()
            if (coreManager.projectManager.openProjects.size == 0) {
                val window = GUIManager.getWindow("fxml/StartGui.fxml", "Sk-IDE", false, Stage())
                window.stage.isResizable = false
                (window.controller as StartGUIController).initGui(coreManager, window, false)
                window.stage.isResizable = false
                if (getOS() == OperatingSystemType.LINUX) window.stage.initStyle(StageStyle.UTILITY)
                window.stage.show()
            }
        }
        val newProject = MenuItem("New Project")
        newProject.setOnAction {
            val window = GUIManager.getWindow("fxml/NewProjectGui.fxml", "Create new Project", false)
            window.controller as CreateProjectGUIController
            window.controller.initGui(coreManager, window)
            window.stage.isResizable = false

            window.stage.show()
        }
        val importProject = MenuItem("Import Project")
        importProject.setOnAction {
            val window = GUIManager.getWindow("fxml/ImportProjectGui.fxml", "Import project", false)
            window.controller as ImportProjectGUIController
            window.controller.initGui(coreManager, window)
            window.stage.isResizable = false
            window.stage.show()
        }
        val newSkriptFile = MenuItem("New Skript File")
        importProject.setOnAction {
            val name = Prompts.textPrompt("New Skript File", "Enter File name Here")
            if (name.isNotEmpty()) openProjectGuiManager.openProject.createNewFile(name)
        }
        val newFile = MenuItem("New File")
        importProject.setOnAction {
            val name = Prompts.textPrompt("New File", "Enter File name Here")
            if (name.isNotEmpty()) openProjectGuiManager.openProject.createNewFile(name)
        }

        val deployMenu = Menu("Deploy")

        val compileMenu = Menu("Compile")
        fileMenu.setOnShowing {
            deployMenu.items.clear()
            openProjectGuiManager.openProject.project.fileManager.compileOptions.forEach { compOpt ->
                val item = Menu(compOpt.key)
                openProjectGuiManager.openProject.project.fileManager.hosts.forEach { h ->
                    val depItem = MenuItem(h.name)
                    depItem.setOnAction {
                        openProjectGuiManager.openProject.deployer.depploy(compOpt.value, h)
                    }
                    item.items.add(depItem)
                }
                deployMenu.items.add(item)
            }

            otherProjects.items.clear()
            coreManager.configManager.projects.values.forEach {

                val open = coreManager.projectManager.openProjects.any { openProject -> it.name == openProject.project.name }
                if (!open) {
                    val item = MenuItem(it.name)
                    val pr = it
                    item.setOnAction {
                        coreManager.projectManager.openProject(pr)
                    }
                    otherProjects.items.add(item)
                }
            }

            compileMenu.items.clear()

            openProjectGuiManager.openProject.project.fileManager.compileOptions.forEach {
                val item = MenuItem(it.key)
                item.setOnAction { _ ->
                    openProjectGuiManager.openFiles.forEach { f ->
                        f.value.manager.saveCode()
                    }
                    openProjectGuiManager.openProject.compiler.compile(openProjectGuiManager.openProject, it.value, openProjectGuiManager.lowerTabPaneEventManager.setupBuildLogTabForInput())
                }
                compileMenu.items.add(item)
            }
        }
        val projectSettings = MenuItem("Project Settings")
        projectSettings.setOnAction {
            openProjectGuiManager.settings.show()
        }
        val generalSettings = MenuItem("General Settings")
        generalSettings.setOnAction {

            val window = GUIManager.getWindow("fxml/GeneralSettingsGui.fxml", "Settings", false)
            window.stage.isResizable = false
            window.stage.initModality(Modality.WINDOW_MODAL)
            if (getOS() == OperatingSystemType.LINUX) window.stage.initStyle(StageStyle.UTILITY)
            window.stage.initOwner(openProjectGuiManager.window.stage)
            SettingsGUIHandler(window.controller as GeneralSettingsGUIController, coreManager, window).init()
            window.stage.show()

        }

        val editServerConfMenu = Menu("Edit server Configuration")
        coreManager.serverManager.servers.forEach {
            val file = File(it.value.configuration.folder, "server.properties")
            if (file.exists()) {
                val tItem = MenuItem(it.value.configuration.name)
                tItem.setOnAction {
                    openProjectGuiManager.openProject.eventManager.openFile(file, true)
                }
                editServerConfMenu.items.add(tItem)
            }
        }
        val newMenu = Menu("New")

        newMenu.items.addAll(newProject, importProject, newSkriptFile, newFile)
        fileMenu.items.addAll(newMenu, projectSettings, otherProjects, compileMenu, generalSettings, editServerConfMenu, deployMenu, closeProjectItem, closeItem)
    }

    private fun simpleMenuItem(name: String, action: () -> Unit): MenuItem {
        val item = MenuItem(name)
        item.setOnAction {
            action()
        }

        return item
    }

    private fun registerBrowserEvents() {


    }

    private fun registerEditorEvents() {

        openProjectGuiManager.window.scene.setOnKeyPressed {
            if (it.verifyKeyCombo() && it.code == KeyCode.N) {
                val name = Prompts.textPrompt("New File", "Enter File name Here")
                if (name.isNotEmpty()) openProjectGuiManager.openProject.createNewFile(name)
            }

            if (it.verifyKeyCombo() && it.code == KeyCode.P) {
                val files = openProjectGuiManager.projectFiles
                val functions = Vector<Triple<File, com.skide.include.Node, SearchPopUpItem>>()
                val events = Vector<Triple<File, com.skide.include.Node, SearchPopUpItem>>()
                val vars = Vector<Triple<File, com.skide.include.Node, SearchPopUpItem>>()

                val items = Vector<SearchPopUpItem>()
                val lastActive = openProjectGuiManager.lastActive


                val box = SearchPopUp(openProjectGuiManager.window.stage) { text ->
                    items.clear()
                    if (text.startsWith("@") && text.length > 1) {
                        for (function in functions) {
                            val name = function.second.fields["name"] as String
                            if (name.contains(text.substring(1), true)) {
                                items.add(function.third)
                            }
                            if (items.size > 125) break
                        }
                        for (event in events) {
                            val name = event.second.fields["name"] as String
                            if (name.contains(text.substring(1), true)) {
                                items.add(event.third)
                            }
                            if (items.size > 125) break

                        }
                        for (variable in vars) {
                            val name = variable.second.fields["name"] as String
                            if (name.contains(text.substring(1), true)) {
                                items.add(variable.third)
                            }
                            if (items.size > 125) break

                        }
                    } else if (text.startsWith(":")) {
                        if (lastActive != null) {
                            val matcher = Pattern.compile("\\d+").matcher(text)
                            if (matcher.find()) {
                                val num = matcher.group().toInt()
                                items.add(SearchPopUpItem("Go to line $num", lastActive.f.name) {
                                    lastActive.area.view.requestFocus()
                                    lastActive.area.moveLineToCenter(num)
                                    lastActive.area.setSelection(num, 1, num, lastActive.area.getColumnLineAmount(num))
                                })
                            }
                        }
                    } else {
                        for (f in files.values) {
                            if (f.name.contains(text, true)) {
                                if (openProjectGuiManager.openFiles.containsKey(f)) {
                                    items.add(SearchPopUpItem("Open ${f.name}", f.name) {
                                        val holder = openProjectGuiManager.openFiles[f]
                                        val tab = holder!!.tab
                                        tab.tabPane.selectionModel.select(tab)
                                        Platform.runLater {
                                            holder.area.focusEditor()

                                        }
                                    })
                                } else {
                                    items.add(SearchPopUpItem("Open ${f.name}", f.name) {
                                        openFile(f, false) {
                                            Platform.runLater {
                                                it.area.focusEditor()
                                            }
                                        }
                                    })
                                }
                            }
                        }
                    }

                    items
                }

                val parser = SkriptParser(openProjectGuiManager.openProject)

                Thread {
                    Thread.sleep(250)
                    for (f in files.values) {
                        if (openProjectGuiManager.openFiles.containsKey(f)) {
                            val holder = openProjectGuiManager.openFiles[f]
                            for (node in EditorUtils.filterByNodeType(NodeType.FUNCTION, holder!!.codeManager.parseResult)) {
                                if (node.fields["visibility"] == "local") continue
                                val name = node.fields["name"] as String
                                functions.add(Triple(f, node, SearchPopUpItem(name, "Function - ${f.name}") {
                                    if (openProjectGuiManager.openFiles.containsKey(f)) {
                                        val childHolder = openProjectGuiManager.openFiles[f]
                                        val tab = childHolder!!.tab
                                        tab.tabPane.selectionModel.select(tab)
                                        Platform.runLater {
                                            childHolder.area.moveLineToCenter(node.linenumber)
                                            childHolder.area.setSelection(node.linenumber, 1, node.linenumber, childHolder.area.getColumnLineAmount(node.linenumber))
                                        }
                                    } else {
                                        openFile(f, false) { it ->
                                            Platform.runLater {
                                                it.area.moveLineToCenter(node.linenumber)
                                                it.area.setSelection(node.linenumber, 1, node.linenumber, it.area.getColumnLineAmount(node.linenumber))

                                            }
                                        }
                                    }
                                }))
                            }
                            for (node in EditorUtils.filterByNodeType(NodeType.EVENT, holder.codeManager.parseResult)) {
                                val name = node.fields["name"] as String
                                events.add(Triple(f, node, SearchPopUpItem(name, "Event - ${f.name}") {
                                    if (openProjectGuiManager.openFiles.containsKey(f)) {
                                        val childHolder = openProjectGuiManager.openFiles[f]
                                        val tab = childHolder!!.tab
                                        tab.tabPane.selectionModel.select(tab)
                                        Platform.runLater {
                                            childHolder.area.moveLineToCenter(node.linenumber)
                                            childHolder.area.setSelection(node.linenumber, 1, node.linenumber, childHolder.area.getColumnLineAmount(node.linenumber))
                                        }
                                    } else {
                                        openFile(f, false) { it ->
                                            Platform.runLater {
                                                it.area.moveLineToCenter(node.linenumber)
                                                it.area.setSelection(node.linenumber, 1, node.linenumber, it.area.getColumnLineAmount(node.linenumber))

                                            }
                                        }
                                    }
                                }))
                            }
                            for (node in EditorUtils.filterByNodeType(NodeType.SET_VAR, holder.codeManager.parseResult)) {
                                val name = node.fields["name"] as String
                                vars.add(Triple(f, node, SearchPopUpItem(if (node.fields["visibility"] == "local")
                                    "{_$name}" else "{$name}", "Variable - ${f.name}") {
                                    if (openProjectGuiManager.openFiles.containsKey(f)) {
                                        val childHolder = openProjectGuiManager.openFiles[f]
                                        val tab = childHolder!!.tab
                                        tab.tabPane.selectionModel.select(tab)
                                        Platform.runLater {
                                            childHolder.area.moveLineToCenter(node.linenumber)
                                            childHolder.area.setSelection(node.linenumber, 1, node.linenumber, childHolder.area.getColumnLineAmount(node.linenumber))
                                        }
                                    } else {
                                        openFile(f, false) { it ->
                                            Platform.runLater {
                                                it.area.moveLineToCenter(node.linenumber)
                                                it.area.setSelection(node.linenumber, 1, node.linenumber, it.area.getColumnLineAmount(node.linenumber))

                                            }
                                        }
                                    }
                                }))
                            }
                        } else {
                            val result = parser.superParse(readFile(f).second)
                            for (node in EditorUtils.filterByNodeType(NodeType.FUNCTION, result)) {
                                if (node.fields["visibility"] == "local") continue
                                val name = node.fields["name"] as String
                                functions.add(Triple(f, node, SearchPopUpItem(name, "Function - ${f.name}") {
                                    if (openProjectGuiManager.openFiles.containsKey(f)) {
                                        val holder = openProjectGuiManager.openFiles[f]
                                        val tab = holder!!.tab
                                        tab.tabPane.selectionModel.select(tab)
                                        Platform.runLater {
                                            holder.area.moveLineToCenter(node.linenumber)
                                            holder.area.setSelection(node.linenumber, 1, node.linenumber, holder.area.getColumnLineAmount(node.linenumber))
                                        }
                                    } else {
                                        openFile(f, false) { it ->
                                            Platform.runLater {
                                                it.area.moveLineToCenter(node.linenumber)
                                                it.area.setSelection(node.linenumber, 1, node.linenumber, it.area.getColumnLineAmount(node.linenumber))

                                            }
                                        }
                                    }
                                }))
                            }
                            for (node in EditorUtils.filterByNodeType(NodeType.EVENT, result)) {
                                val name = node.fields["name"] as String
                                events.add(Triple(f, node, SearchPopUpItem(name, "Event - ${f.name}") {
                                    if (openProjectGuiManager.openFiles.containsKey(f)) {
                                        val holder = openProjectGuiManager.openFiles[f]
                                        val tab = holder!!.tab
                                        tab.tabPane.selectionModel.select(tab)
                                        Platform.runLater {
                                            holder.area.moveLineToCenter(node.linenumber)
                                            holder.area.setSelection(node.linenumber, 1, node.linenumber, holder.area.getColumnLineAmount(node.linenumber))
                                        }
                                    } else {
                                        openFile(f, false) { it ->
                                            Platform.runLater {
                                                it.area.moveLineToCenter(node.linenumber)
                                                it.area.setSelection(node.linenumber, 1, node.linenumber, it.area.getColumnLineAmount(node.linenumber))

                                            }
                                        }
                                    }
                                }))
                            }
                            for (node in EditorUtils.filterByNodeType(NodeType.SET_VAR, result)) {
                                val name = node.fields["name"] as String
                                vars.add(Triple(f, node, SearchPopUpItem("{$name}", "Variable - ${f.name}") {
                                    if (openProjectGuiManager.openFiles.containsKey(f)) {
                                        val holder = openProjectGuiManager.openFiles[f]
                                        val tab = holder!!.tab
                                        tab.tabPane.selectionModel.select(tab)
                                        Platform.runLater {
                                            holder.area.moveLineToCenter(node.linenumber)
                                            holder.area.setSelection(node.linenumber, 1, node.linenumber, holder.area.getColumnLineAmount(node.linenumber))
                                        }
                                    } else {
                                        openFile(f, false) { it ->
                                            Platform.runLater {
                                                it.area.moveLineToCenter(node.linenumber)
                                                it.area.setSelection(node.linenumber, 1, node.linenumber, it.area.getColumnLineAmount(node.linenumber))

                                            }
                                        }
                                    }
                                }))
                            }
                        }
                    }
                    box.doneIndexing()
                }.start()
            }
        }
        DragResizerLeft().makeResizable(filesTab.second, controller.mainLeftBorderPane)
        DragResizerLeft().makeResizable(structureTab.second, controller.mainLeftBorderPane)
        controller.toggleTreeViewButton.setIcon("login")
        controller.toggleTreeViewButton.setOnAction {
            val tabPane = controller.browserTabPane
            val pane = controller.mainLeftBorderPane
            if (browserVisible) {
                controller.toggleTreeViewButton.setIcon("exit")
                browserVisible = false
                pane.center = null
                pane.maxWidth = 25.0
            } else {
                browserVisible = true
                controller.toggleTreeViewButton.setIcon("login")
                pane.maxWidth = 1500.0
                pane.prefWidth = 200.0
                pane.center = tabPane
            }
        }
        structureTab.second.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            if (newValue != null) {

                val item = newValue as TreeItem<String>

                val tab = controller.editorMainTabPane.selectionModel.selectedItem

                openProjectGuiManager.openFiles.values
                        .filter { it.tab == tab }
                        .filter { it.name.endsWith(".sk") }
                        .forEach { it.codeManager.gotoItem(item) }
            }
        }

        controller.editorMainTabPane.selectionModel.selectedItemProperty().addListener { _, _, _ ->

            if (controller.editorMainTabPane.selectionModel.selectedItem != null) {

                val tab = controller.editorMainTabPane.selectionModel.selectedItem
                openProjectGuiManager.openFiles.values
                        .filter { it.tab == tab }
                        .forEach {
                            if (it.name.endsWith(".sk")) {
                                Platform.runLater {
                                    updateStructureTab(it)
                                    it.codeManager.parseResult = it.codeManager.parseStructure(true)
                                }
                            }
                        }
            } else {
                controller.browserTabPane.selectionModel.select(0)
                structureTab.first.isDisable = true
            }
        }
    }
}