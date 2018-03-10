package com.skide.include

import com.skide.CoreManager
import com.skide.core.code.CodeManager
import com.skide.core.management.OpenProject
import com.skide.gui.GuiManager
import com.skide.utils.writeFile
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import org.fxmisc.richtext.CodeArea
import java.io.File

class ActiveWindow(val stage:Stage, val scene:Scene, val loader:FXMLLoader, val controller:Any, val id:Int) {
    fun close() = GuiManager.closeGui(id)

    var closeListener = {}


    init {
        stage.setOnCloseRequest {
            closeListener()
        }

    }
}

class OpenFileHolder(val openProject: OpenProject, val f: File, val name:String, val tab:Tab, val tabPane: TabPane, val borderPane:BorderPane, val area:CodeArea, val coreManager: CoreManager,  val codeManager:CodeManager = CodeManager()) {




    fun saveCode() {
        Thread {
            writeFile(area.text.toByteArray(), f)
        }.start()
    }
}