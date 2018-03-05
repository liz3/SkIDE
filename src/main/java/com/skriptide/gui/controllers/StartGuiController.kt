package com.skriptide.gui.controllers

import com.skriptide.CoreManager
import com.skriptide.gui.GuiManager
import com.skriptide.include.ActiveWindow
import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.Pane


class StartGuiController {


    @FXML
    private var projectsList: ListView<String>? = null

    @FXML
    private var createNewProject: Label? = null

    @FXML
    private var iconImage: ImageView? = null

    @FXML
    private var importProject: Label? = null

    fun initGui(manager: CoreManager, thisWindow: ActiveWindow, firstRun:Boolean) {

        iconImage?.image = Image(javaClass.getResource("/icon.png").toExternalForm())

        createNewProject?.setOnMouseClicked {

            val window = GuiManager.getWindow("NewProjectGui.fxml", "Create new Project", false)
            window.controller as CreateProjectGuiController
            window.controller.initGui(manager, window, thisWindow)
            window.stage.isResizable = false
            window.closeListener = {
            thisWindow.stage.show()
            }
            thisWindow.stage.hide()
            window.stage.show()

        }
        projectsList!!.setOnMouseReleased {
            val selection = projectsList!!.selectionModel.selectedItem

            if(selection != null) {
                manager.configManager.projects.values.forEach {
                    if((it.name + "\n" + it.path) == selection) {
                        manager.projectManager.openProject(it)
                        thisWindow.close()
                    }

                }
            }
        }
        importProject?.setOnMouseClicked {

            val window = GuiManager.getWindow("ImportProjectGui.fxml", "Import Project", false)
            window.controller as ImportProjectGuiController
            window.controller.initGui(manager, window, thisWindow)
            window.stage.isResizable = false
            window.closeListener = {
            thisWindow.stage.show()
            }
            thisWindow.stage.hide()
            window.stage.show()

        }
        if(!firstRun) {

            manager.configManager.projects.values.forEach {
                projectsList!!.items.add("${it.name}\n${it.path}")
            }
        }
    }


}