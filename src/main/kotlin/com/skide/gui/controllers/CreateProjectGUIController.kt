package com.skide.gui.controllers

import com.skide.CoreManager
import com.skide.include.ActiveWindow
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.scene.control.TextField
import javafx.stage.DirectoryChooser
import javafx.stage.Stage
import java.io.File


class CreateProjectGUIController {


    @FXML
    private lateinit var projectNameField: TextField

    @FXML
    private lateinit var projectPathField: TextField

    @FXML
    private lateinit var choosePathButton: Button

    @FXML
    private lateinit var createButton: Button

    @FXML
    private lateinit var cancelButton: Button

    @FXML
    private lateinit var skriptVersionComboBox: ComboBox<String>

    @FXML
    private lateinit var openAfterCreation: CheckBox

    var rootProjectFolder = ""

    fun initGui(manager: CoreManager, thisWindow: ActiveWindow, returnWindow: ActiveWindow? = null) {

        openAfterCreation.isSelected = true
        openAfterCreation.isDisable = true
        createButton.isDisable = true
        rootProjectFolder = manager.configManager.defaultProjectPath.absolutePath
        skriptVersionComboBox.items.add("Latest")
        skriptVersionComboBox.selectionModel.select(0)

        projectNameField.setOnKeyReleased { _ ->
            projectPathField.text = File(rootProjectFolder, projectNameField.text).absolutePath

            if (projectNameField.text == "") {
                if (!createButton.isDisabled) createButton.isDisable = true
            } else {
                if (createButton.isDisabled) createButton.isDisable = false

                //Check existing projects for possible duplications
                var found = false
                manager.configManager.projects.values.forEach {
                    if (it.path == projectPathField.text) {
                        found = true
                    }
                }
                createButton.isDisable = found
            }


        }

        choosePathButton.setOnAction {
            val fileChooserWindow = Stage()
            val dirChooser = DirectoryChooser()
            dirChooser.title = "Choose save path for the Project"
            val dir = dirChooser.showDialog(fileChooserWindow)
            if (dir != null) {
                rootProjectFolder = dir.absolutePath
                projectPathField.text = File(rootProjectFolder, projectNameField.text).absolutePath

            }

        }
        projectPathField.text = manager.configManager.defaultProjectPath.absolutePath


        createButton.setOnAction {
            if (!projectPathField.text.contains(rootProjectFolder)) return@setOnAction
            manager.configManager.defaultProjectPath = File(rootProjectFolder)
            manager.projectManager.createNewProject(projectNameField.text, projectPathField.text, skriptVersionComboBox.selectionModel.selectedItem, openAfterCreation.isSelected)

            thisWindow.close()
            if (!openAfterCreation.isSelected) {
                returnWindow?.stage?.show()
            }
        }

        cancelButton.setOnAction {
            thisWindow.close()
            returnWindow?.stage?.show()
        }
    }
}