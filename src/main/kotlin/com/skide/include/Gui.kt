package com.skide.include

import com.skide.CoreManager
import com.skide.core.code.CodeArea
import com.skide.core.code.CodeManager
import com.skide.core.management.OpenProject
import com.skide.gui.GUIManager
import com.skide.gui.MouseDragHandler
import com.skide.utils.writeFile
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.image.Image
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.web.WebView
import javafx.stage.Stage
import java.io.File

class ActiveWindow(val stage: Stage, val scene: Scene, val loader: FXMLLoader, val controller: Any, val id: Int) {
    fun close() = GUIManager.closeGui(id)

    var closeListener = {}


    init {
        stage.setOnCloseRequest {
            closeListener()
        }

    }
}

class OpenFileHolder(val openProject: OpenProject, val f: File, val name: String, val tab: Tab, val tabPane: TabPane, var borderPane: BorderPane, val area: CodeArea, val coreManager: CoreManager, val codeManager: CodeManager = CodeManager(), val isExternal: Boolean = false) {

    val currentStackBox = HBox()
    var isExluded = false
    lateinit var externStage: Stage


    fun saveCode() {
        Thread {
            Platform.runLater {
            writeFile(area.text.toByteArray(), f)
                if(tab.text.endsWith("*")) tab.text = tab.text.substring(0, tab.text.length - 1)
            }
        }.start()
    }

    fun toggleExclude() {

        if (isExluded) {

            (externStage.scene.root as TabPane).tabs.forEach {
                it.onCloseRequest.handle(null)
            }
            isExluded = false
            externStage.close()

        } else {

            externStage = Stage()
            val tabPane = TabPane()
            tabPane.selectionModel.selectedItemProperty().addListener { _, _, newValue ->

                if(newValue == null) {
                    isExluded = false
                    externStage.close()
                }
            }
            MouseDragHandler(tabPane, openProject.guiHandler).setup()
            this.tabPane.tabs.remove(this.tab)
            tabPane.tabs.add(this.tab)
            externStage.title = f.name
            externStage.icons.add(Image(javaClass.getResource("/icon.png").toExternalForm()))
            externStage.scene = Scene(tabPane, 800.0, 600.0)
            externStage.scene.stylesheets.add(coreManager.configManager.getCssPath("Reset.css"))
            if (openProject.coreManager.configManager.get("theme") == "Dark") {
                externStage.scene.stylesheets.add(coreManager.configManager.getCssPath("ThemeDark.css"))
            }
            if (coreManager.configManager.get("theme") == "Dark") {
                externStage.scene.stylesheets.add(coreManager.configManager.getCssPath("DarkHighlighting.css"))
            } else {
                externStage.scene.stylesheets.add(coreManager.configManager.getCssPath("HighlightingLight.css"))
            }
            externStage.setOnCloseRequest {
                toggleExclude()
            }
            externStage.show()
            isExluded = true
        }
    }
}

enum class EditorMode {
    NORMAL,
    SIDE_SPLIT,
    DOWN_SPLIT
}