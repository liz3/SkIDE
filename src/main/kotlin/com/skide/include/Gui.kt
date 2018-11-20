package com.skide.include

import com.skide.CoreManager
import com.skide.core.code.CodeArea
import com.skide.core.code.CodeManager
import com.skide.core.management.OpenProject
import com.skide.core.management.OpenProjectManager
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
import javafx.stage.Stage
import org.controlsfx.control.BreadCrumbBar
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

    val currentStackBox = BreadCrumbBar<Node>()
    val manager = OpenProjectManager(this)
}

enum class EditorMode {
    NORMAL,
    SIDE_SPLIT,
    DOWN_SPLIT
}