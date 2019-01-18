package com.skide.gui.controllers

import com.skide.CoreManager
import com.skide.Info
import com.skide.core.management.PointerHolder
import com.skide.gui.GUIManager
import com.skide.gui.Prompts
import com.skide.gui.settings.SettingsGUIHandler
import com.skide.include.ActiveWindow
import com.skide.utils.setIcon
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.geometry.Pos
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority


class ProjectListEntry(val name: String, val holder: PointerHolder, val action: () -> Unit) : HBox() {

    private val labelLeft = Label()
    val deleteBtn = Button()

    init {
        deleteBtn.setOnAction {
            action()
        }
        deleteBtn.alignment = Pos.CENTER
        deleteBtn.setIcon("delete", false)
        val pane = Pane()
        labelLeft.text = name
        HBox.setHgrow(pane, Priority.ALWAYS)
        this.children.addAll(labelLeft, pane, deleteBtn)

    }

}

class StartGUIController {


    @FXML
    private lateinit var projectsList: ListView<ProjectListEntry>

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

        projectsList.setOnMouseClicked { ev ->
            val selection = projectsList.selectionModel.selectedItem

            if (selection != null) {
                manager.configManager.projects.values.forEach {
                    if (it == selection.holder) {
                        manager.projectManager.openProject(it)
                        thisWindow.close()
                    }
                }
            }
        }
        aboutLabel.setOnMouseReleased {
            GUIManager.showAbout()
        }
        versionLabel.text = "SkIDE Ultimate Ver: ${Info.version} Copyright 21Xayah.com ${if (!Info.prodMode) "dev-mode" else ""}"
        settings.setOnMouseReleased {

            val window = GUIManager.getWindow("fxml/GeneralSettingsGui.fxml", "Settings", false)
            SettingsGUIHandler(window.controller as GeneralSettingsGUIController, manager, window).init()
            window.stage.isResizable = false
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
                projectsList.items.add(ProjectListEntry("${it.name}\n${it.path}", it) {
                    val check = Prompts.infoCheck("Remove", "Remove from list", "Remove ${it.name} from list?", Alert.AlertType.CONFIRMATION)
                    if (check) {
                        manager.projectManager.deleteProject(it, Prompts.infoCheck("Remove", "Remove files", "Also delete the files? not reversible!", Alert.AlertType.CONFIRMATION))
                        for (item in projectsList.items) {
                            Platform.runLater {
                                if (item.holder == it) projectsList.items.remove(item)
                            }
                            break
                        }
                    }
                })
            }
        }
    }


}