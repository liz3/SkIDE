package com.skide.include

import com.skide.CoreManager
import com.skide.core.code.CodeManager
import com.skide.core.management.OpenProject
import com.skide.gui.GUIManager
import com.skide.utils.writeFile
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.layout.Border
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.stage.Stage
import org.fxmisc.richtext.CodeArea
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
    lateinit var externPane:BorderPane

    fun saveCode() {
        Thread {
            writeFile(area.text.toByteArray(), f)
        }.start()
    }

    fun toggleExlude() {

        if (isExluded) {

            externStage.close()
            tabPane.tabs.add(tab)
            borderPane = BorderPane()
            borderPane.center = externPane.center
            borderPane.right = externPane.right
            borderPane.left = externPane.left
            borderPane.bottom = externPane.bottom
            borderPane.top = externPane.top
            tab.content = borderPane
            isExluded = false
        } else {

            externStage = Stage()
            externStage.title = f.name

            externStage.setOnCloseRequest {
                toggleExlude()
            }
            tabPane.tabs.remove(tab)

            externPane = BorderPane()
            val pane = externPane
            pane.center = borderPane.center
            pane.top = borderPane.top
            pane.left = borderPane.left
            pane.right = borderPane.right
            pane.bottom = borderPane.bottom
            borderPane = pane
            externStage.scene = Scene(pane, 800.0, 600.0)
                externStage.scene.stylesheets.add(coreManager.configManager.getCssPath("Reset.css"))
            if(openProject.coreManager.configManager.get("theme") == "Dark") {
                externStage.scene.stylesheets.add(coreManager.configManager.getCssPath("ThemeDark.css"))

            }
            if (coreManager.configManager.get("theme") == "Dark") {
                externStage.scene.stylesheets.add(coreManager.configManager.getCssPath("DarkHighlighting.css"))
            } else {
                externStage.scene.stylesheets.add(coreManager.configManager.getCssPath("HighlightingLight.css"))
            }
            externStage.show()
            isExluded = true
        }
    }
}