package com.skide.gui.project

import com.skide.CoreManager
import com.skide.core.code.CodeArea
import com.skide.core.management.ExternalHandler
import com.skide.core.management.OpenProject
import com.skide.gui.GUIManager
import com.skide.gui.Menus
import com.skide.gui.MouseDragHandler
import com.skide.gui.Prompts
import com.skide.gui.controllers.CreateProjectGUIController
import com.skide.gui.controllers.GeneralSettingsGUIController
import com.skide.gui.controllers.ProjectGUIController
import com.skide.gui.settings.SettingsGUIHandler
import com.skide.include.EditorMode
import com.skide.include.OpenFileHolder
import com.skide.utils.OperatingSystemType
import com.skide.utils.getOS
import com.skide.utils.setIcon
import javafx.application.Platform
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.MouseButton
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import java.io.File
import java.util.*


class OpenProjectGuiManager(val openProject: OpenProject, val coreManager: CoreManager) {


    var mode = EditorMode.NORMAL
    val openFiles = HashMap<File, OpenFileHolder>()
    val settings = SettingsGui(coreManager, this)
    val window = GUIManager.getWindow("fxml/ProjectGui.fxml", openProject.project.name, false)
    lateinit var lowerTabPaneEventManager: LowerTabPaneEventManager
    val otherTabPanes = Vector<TabPane>()
    var paneHolderNode: Node = HBox()
    var draggedTab: Tab? = null
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

    var browserVisible = true
    var guiReady = {}
    var contextMenuVisible: ContextMenu? = null
    val mouseDragHandler = MouseDragHandler(controller.editorMainTabPane, this.openProjectGuiManager)

    val filesTab = {
        val tab = Tab()
        val iconView = ImageView(Image(javaClass.getResource("/images/files_main.png").toExternalForm()))
        tab.graphic = iconView
        val treeView = TreeView<String>()
        //set the root item
        treeView.selectionModel.selectedItemProperty().addListener { _, _, newValue ->


            if (newValue == null) return@addListener
            val selectedItem = newValue as TreeItem<String>

            if (selectedItem != treeView.root) {

                if (openProjectGuiManager.projectFiles.containsKey(selectedItem.value)) {

                    Thread {
                        openFile(openProjectGuiManager.projectFiles[selectedItem.value]!!)
                    }.start()
                }
            }
        }

        tab.content = treeView
        Pair(tab, treeView)
    }.invoke()
    val structureTab = {
        val tab = Tab()
        val iconView = ImageView(Image(javaClass.getResource("/images/file_skriptstructure.png").toExternalForm()))

        tab.graphic = iconView
        val treeView = TreeView<String>()
        tab.content = treeView
        tab.isDisable = true
        Pair(tab, treeView)
    }.invoke()


    fun setup() {
        replaceTemplateElements()
        registerBrowserEvents()
        registerEditorEvents()
        setupMainMenu()
        updateProjectFilesTreeView()
        mouseDragHandler.setup()
        guiReady()
        openProjectGuiManager.openProject.project.fileManager.lastOpen.forEach {
            openFile(openProjectGuiManager.openProject.project.fileManager.projectFiles[it]!!)
        }
    }


    fun openFile(f: File, isExternal: Boolean = false, cb:(OpenFileHolder) -> Unit = {}) {

        if (openProjectGuiManager.openFiles.containsKey(f)) {
            for ((file, holder) in openProjectGuiManager.openFiles) {
                if (file === f) {
                    holder.tabPane.selectionModel.select(holder.tab)
                    break
                }
            }
            return
        }
        CodeArea(coreManager) {

            val holder = OpenFileHolder(openProjectGuiManager.openProject, f, f.name, Tab(f.name), if (openProjectGuiManager.mode == EditorMode.NORMAL) controller.editorMainTabPane else openProjectGuiManager.otherTabPanes.firstElement(), BorderPane(), it, coreManager, isExternal = isExternal)

            it.openFileHolder = holder
            openProjectGuiManager.openFiles[f] = holder
            setupNewTabForDisplay(holder)

            cb(holder)
        }

    }

    fun openFile(f: File, tabPane: TabPane, isExternal: Boolean = false) {

        if (openProjectGuiManager.openFiles.containsKey(f)) {
            for ((file, holder) in openProjectGuiManager.openFiles) {
                if (file === f) {
                    holder.tabPane.selectionModel.select(holder.tab)
                    break

                }
            }

            return
        }

        CodeArea(coreManager) {
            val holder = OpenFileHolder(openProjectGuiManager.openProject, f, f.name, Tab(f.name), tabPane, BorderPane(), it, coreManager, isExternal = isExternal)
            it.openFileHolder = holder
            openProjectGuiManager.openFiles.put(f, holder)
            setupNewTabForDisplay(holder)
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

        val newProject = MenuItem("New Project")
        newProject.setOnAction {
            val window = GUIManager.getWindow("fxml/NewProjectGui.fxml", "Create new Project", false)
            window.controller as CreateProjectGUIController
            window.controller.initGui(coreManager, window)
            window.stage.isResizable = false

            window.stage.show()
        }

        val compileMenu = Menu("Compile")
        fileMenu.setOnShowing {
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
                    openProjectGuiManager.openProject.compiler.compile(openProjectGuiManager.openProject.project, it.value, openProjectGuiManager.lowerTabPaneEventManager.setupBuildLogTabForInput())
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

            SettingsGUIHandler(window.controller as GeneralSettingsGUIController, coreManager, window).init()

            window.stage.show()

        }
        val deployMenu = Menu("Deploy")
        openProjectGuiManager.openProject.project.fileManager.compileOptions.forEach { compOpt ->
            val item = Menu(compOpt.key)
            openProjectGuiManager.openProject.project.fileManager.hosts.forEach {
                val depItem = MenuItem(it.name)
                depItem.setOnAction { _ ->
                    openProjectGuiManager.openProject.deployer.depploy(compOpt.value, it)
                }
                item.items.add(depItem)
            }
            deployMenu.items.add(item)
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
        fileMenu.items.addAll(newProject, projectSettings, otherProjects, compileMenu, generalSettings, editServerConfMenu, deployMenu, closeItem)
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

        controller.browserUpperHBox.setOnScroll { ev ->
            if (browserVisible) {
                val pane = controller.mainLeftBorderPane
                val x = ev.deltaY

                if (x < 0) {
                    pane.prefWidth = pane.prefWidth - 6.0
                } else {
                    pane.prefWidth = pane.prefWidth + 6.0

                }
            }
        }
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
                            if (it.name.endsWith(".sk")) updateStructureTab(it)
                        }
            } else {
                controller.browserTabPane.selectionModel.select(0)
                structureTab.first.isDisable = true
            }
        }
    }
}