package com.skide.gui.project

import com.skide.CoreManager
import com.skide.gui.DragResizer
import com.skide.gui.controllers.ProjectGuiController
import com.terminalfx.TerminalBuilder
import javafx.application.Platform
import javafx.scene.control.Tab
import javafx.scene.control.TextArea

class LowerTabPaneEventManager(val ctrl: ProjectGuiController, val openProjectGuiManager: OpenProjectGuiManager, val coreManager: CoreManager) {

    var visible = true
    val rootPane = ctrl.mainLowerBorderPane
    val tabPane = ctrl.lowerTabPane
    val terminalBuilder = TerminalBuilder()
    val buildLogArea = TextArea()


    fun setupBuildLogTabForInput(): (String) -> Unit {

        buildLogArea.clear()
        tabPane.selectionModel.select(1)

        val recaller = { x: String ->

            Platform.runLater {
                buildLogArea.appendText("$x\n")
            }
        }

        return recaller
    }

    fun setup() {

        val buildLogTab = Tab("Build Log")
        buildLogTab.content = buildLogArea
        tabPane.tabs.add(buildLogTab)
        buildLogArea.isEditable = false

        DragResizer().makeResizable(ctrl.mainLowerBorderPane)
        terminalBuilder.terminalPath = openProjectGuiManager.openProject.project.folder.toPath()
        terminalBuilder.terminalConfig.backgroundColor = "#2B2B2B"
        terminalBuilder.terminalConfig.foregroundColor = "#dbe0dc"

        ctrl.lowerTabPaneToggleBtn.setOnAction {
            if (visible) {
                rootPane.prefHeight = 0.0
            } else {
                rootPane.prefHeight = 200.0
            }
            visible = !visible
        }
        ctrl.consoleAddBtn.setOnAction {

            val tab = terminalBuilder.newTerminal()
            tab.text = "Console"
            tab.isClosable = false
            ctrl.consoleTabArea.tabs.add(tab)


            ctrl.consoleTabArea.selectionModel.select(tab)
        }
        ctrl.consoleRemBtn.setOnAction {
            if (ctrl.consoleTabArea.tabs.size == 1) {
                ctrl.consoleTabArea.tabs.clear()
                val tab = terminalBuilder.newTerminal()
                tab.text = "Console"
                tab.isClosable = false
                ctrl.consoleTabArea.tabs.add(tab)
                ctrl.consoleTabArea.selectionModel.select(tab)
            } else {
                val curr = ctrl.consoleTabArea.selectionModel.selectedItem

                ctrl.consoleTabArea.tabs.remove(curr)
            }
        }

        val tab = terminalBuilder.newTerminal()
        tab.text = "Console"
        tab.isClosable = false
        ctrl.consoleTabArea.tabs.add(tab)


        ctrl.consoleTabArea.selectionModel.select(tab)

    }


}