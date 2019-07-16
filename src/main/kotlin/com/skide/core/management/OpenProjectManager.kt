package com.skide.core.management

import com.skide.gui.MouseDragHandler
import com.skide.include.OpenFileHolder
import com.skide.utils.writeFile
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.control.TabPane
import javafx.scene.image.Image
import javafx.stage.Stage

class OpenProjectManager(val openProject: OpenFileHolder) {

    var isExluded = false
    lateinit var externStage: Stage
    lateinit var externTabPane:TabPane
    var edited = false

    init {

        openProject.currentStackBox.style = "-fx-color: -fx-base; -fx-background: -fx-base"
        openProject.currentStackBox.setOnCrumbAction {
            val line = it.selectedCrumb.value.linenumber
            openProject.area.moveLineToCenter(line)
            openProject.area.setSelection(line, 1, line, openProject.area.getColumnLineAmount(line))


        }
    }

    fun updateEdited(edited: Boolean) {
        if (edited && !this.edited) {
            openProject.tab.text = "${openProject.tab.text}*"
        }
        if(!edited && this.edited) {
            openProject.tab.text = openProject.tab.text.substring(0, openProject.tab.text.length - 1)
        }
        this.edited = edited
    }


    fun toggleExclude() {

        if (isExluded) {
            isExluded = false

           Platform.runLater {
               if(externTabPane.tabs.size == 0) {
                   (externStage.scene.root as TabPane).tabs.forEach {
                       it.onCloseRequest.handle(null)
                   }
                   externStage.close()
               }
           }

        } else {

            externTabPane = TabPane()
            externStage = Stage()
            val tabPane = externTabPane
            tabPane.selectionModel.selectedItemProperty().addListener { _, _, newValue ->

                if (newValue == null) {
                    Platform.runLater {
                        if(externTabPane.tabs.size == 0) {
                            (externStage.scene.root as TabPane).tabs.forEach {
                                it.onCloseRequest.handle(null)
                            }
                            externStage.close()
                        }
                    }
                }
            }
            MouseDragHandler(tabPane, openProject.openProject.guiHandler).setup()
            val oldPane = openProject.tab.tabPane
            oldPane?.tabs?.remove(openProject.tab)
            if(openProject.tabPane.tabs.contains(openProject.tab))
                openProject.tabPane.tabs.remove(openProject.tab)
            tabPane.tabs.add(openProject.tab)
            externStage.title = openProject.name
            externStage.icons.add(Image(javaClass.getResource("/images/icon.png").toExternalForm()))
            externStage.scene = Scene(tabPane, 800.0, 600.0)
            externStage.scene.stylesheets.add(openProject.coreManager.configManager.getCssPath("Reset.css"))
            if (openProject.coreManager.configManager.get("theme") == "Dark") {
                externStage.scene.stylesheets.add(openProject.coreManager.configManager.getCssPath("ThemeDark.css"))
            }
            externStage.setOnCloseRequest {
                toggleExclude()
            }
            externStage.show()
            isExluded = true
        }
    }

    fun saveCode() {
        Thread {
            Platform.runLater {
                writeFile(openProject.area.text.toByteArray(), openProject.f)
                updateEdited(false)
            }
        }.start()
    }
}