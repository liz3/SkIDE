package com.skide.gui.controllers

import com.skide.CoreManager
import com.skide.Info
import com.skide.gui.GUIManager
import com.skide.gui.settings.SettingsGUIHandler
import com.skide.include.ActiveWindow
import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.image.Image
import javafx.scene.image.ImageView


class StartGUIController {


    @FXML
    private lateinit var projectsList: ListView<String>

    @FXML
    private lateinit var createNewProject: Label

    @FXML
    private lateinit var iconImage: ImageView

    @FXML
    private lateinit var importProject: Label

    @FXML
    private lateinit var settings: Label

    @FXML
    private lateinit var aboutLabel: Label
    @FXML
    private lateinit var versionLabel: Label

    fun initGui(manager: CoreManager, thisWindow: ActiveWindow, firstRun: Boolean) {

        iconImage.image = Image(javaClass.getResource("/images/icon.png").toExternalForm())

        createNewProject.setOnMouseEntered { createNewProject.styleClass.add("menu-entry") }
        createNewProject.setOnMouseExited { createNewProject.styleClass.remove("menu-entry") }
        importProject.setOnMouseEntered { importProject.styleClass.add("menu-entry") }
        importProject.setOnMouseExited { importProject.styleClass.remove("menu-entry") }
        settings.setOnMouseEntered { settings.styleClass.add("menu-entry") }
        settings.setOnMouseExited { settings.styleClass.remove("menu-entry") }
        aboutLabel.setOnMouseEntered { aboutLabel.styleClass.add("menu-entry") }
        aboutLabel.setOnMouseExited { aboutLabel.styleClass.remove("menu-entry") }
        createNewProject.setOnMouseClicked {

            val window = GUIManager.getWindow("fxml/NewProjectGui.fxml", "Create new Project", false)
            window.controller as CreateProjectGUIController
            window.controller.initGui(manager, window, thisWindow)
            window.stage.isResizable = false
            window.closeListener = {
                thisWindow.stage.show()
            }
            thisWindow.stage.hide()
            window.stage.show()

        }

        projectsList.setOnMouseReleased {
            val selection = projectsList.selectionModel.selectedItem

            if (selection != null) {
                manager.configManager.projects.values.forEach {
                    if ((it.name + "\n" + it.path) == selection) {
                        manager.projectManager.openProject(it)
                        thisWindow.close()
                    }

                }
            }
        }
        aboutLabel.setOnMouseReleased {
            GUIManager.showAbout()
        }
        versionLabel.text = "SK-IDE Version: ${Info.version} Copyright 21Xayah.com - GPL v2"
        settings.setOnMouseReleased {

            val window = GUIManager.getWindow("fxml/GeneralSettingsGui.fxml", "Settings", false)
            SettingsGUIHandler(window.controller as GeneralSettingsGUIController, manager, window).init()

            window.stage.show()
        }
        importProject.setOnMouseClicked {

            val window = GUIManager.getWindow("fxml/ImportProjectGui.fxml", "Import Project", false)
            window.controller as ImportProjectGUIController
            window.controller.initGui(manager, window, thisWindow)
            window.stage.isResizable = false
            window.closeListener = {
                thisWindow.stage.show()
            }
            thisWindow.stage.hide()
            window.stage.show()

        }
        if (!firstRun) {

            manager.configManager.projects.values.forEach {
                projectsList.items.add("${it.name}\n${it.path}")
            }
        }
    }


}