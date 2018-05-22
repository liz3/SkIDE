package com.skide.gui.project

import com.skide.core.management.OpenProject
import com.skide.gui.controllers.ProjectSettingsGUIController
import com.skide.include.RemoteHost
import com.skide.include.RemoteHostType
import javafx.stage.FileChooser
import javafx.stage.Stage
import java.io.File

class DeployOptionsGUI(val project: OpenProject, val ctrl: ProjectSettingsGUIController) {


    fun initDeployModule() {


        ctrl.deployList.selectionModel.selectedItemProperty().addListener { observable, oldValue, newValue ->
            if(newValue == null) return@addListener
            ctrl.deployHostTextField.text = newValue.host
            ctrl.deployMethodComboBox.selectionModel.select(newValue.type.toString())
            ctrl.deployPasswordField.text = if (newValue.passwordSaved) newValue.password else ""
            ctrl.deployFolderPathTextField.text = newValue.folderPath
            ctrl.deployPortTextField.text = newValue.port.toString()
            if(newValue.isPrivateKey) {
                ctrl.deployPassphraseLabel.text = newValue.privateKeyPath
            }
        }
        ctrl.deployHostTextField.textProperty().addListener { observable, oldValue, newValue ->
            if (current() == null) return@addListener
            current().host = newValue
        }
        ctrl.deployMethodComboBox.selectionModel.selectedItemProperty().addListener { observable, oldValue, newValue ->
            if (current() == null) return@addListener
            current().type = RemoteHostType.valueOf(newValue)
        }

        ctrl.deployFolderPathTextField.textProperty().addListener { observable, oldValue, newValue ->
            if (current() == null) return@addListener

            current().folderPath = newValue
        }
        ctrl.deployUsernameTextField.textProperty().addListener { observable, oldValue, newValue ->
            if (current() == null) return@addListener

            current().username = newValue
        }
        ctrl.deployPassphraseButton.setOnAction {
            if (current() == null) return@setOnAction
            val f = getFile("Private ssh key file")
            if (f != null) {
                current().isPrivateKey = true
                current().privateKeyPath = f.absolutePath
                ctrl.deployPassphraseLabel.text = f.absolutePath
            } else {
                current().isPrivateKey = false
                current().privateKeyPath = ""
                ctrl.deployPassphraseLabel.text = ""

            }
        }
        ctrl.deployPortTextField.textProperty().addListener { observable, oldValue, newValue ->
            if (current() == null) return@addListener

            try {
                current().port = newValue.toInt()
            } catch (e: Exception) {

            }
        }
        ctrl.deployPasswordField.textProperty().addListener { observable, oldValue, newValue ->
            if (current() == null) return@addListener

            if (newValue == null || newValue.isEmpty()) {
                current().passwordSaved = false
                current().password = ""
            } else {
                current().passwordSaved = true
                current().password = newValue

            }
        }

        ctrl.deployMethodComboBox.items.addAll("SFTP", "FTP")
        ctrl.deployMethodComboBox.selectionModel.select(0)
        ctrl.deployDeleteBtn.setOnAction {
            if (current() == null) return@setOnAction
            ctrl.deployList.items.remove(current())
        }
        ctrl.deployNewButton.setOnAction {
            var nameFound = false
            if (ctrl.deployNewTextField.text == null || ctrl.deployNewTextField.text.isEmpty()) nameFound = true
            ctrl.deployList.items.forEach {
                if (it.name == ctrl.deployNewTextField.text) nameFound = true
            }

            if (!nameFound) {
                val name = ctrl.deployNewTextField.text
                val opt = RemoteHost(name, RemoteHostType.SFTP, "", 22, false, false, "", "", "", "")

                    ctrl.deployList.items.add(opt)
                    ctrl.deployList.selectionModel.select(opt)
                    ctrl.deployNewTextField.text = ""
                    resetValues()

            }
        }
        project.project.fileManager.hosts.forEach {
            ctrl.deployList.items.add(it)
        }

        resetValues()
    }

    private fun getFile(name: String): File? {

        val fileChooserWindow = Stage()
        val dirChooser = FileChooser()
        dirChooser.title = name
        return dirChooser.showOpenDialog(fileChooserWindow)
    }

    private fun validateCurrent(): Boolean {

        if (ctrl.deployHostTextField.text.isEmpty()) return false
        if (ctrl.deployFolderPathTextField.text.isEmpty()) return false
        try {
            Integer.valueOf(ctrl.deployPortTextField.text)
        } catch (e: Exception) {
            return false
        }
        return true
    }

    private fun resetValues() {
        ctrl.deployPassphraseLabel.text = ""
        ctrl.deployPortTextField.text = ""
        ctrl.deployFolderPathTextField.text = ""
        ctrl.deployPasswordField.text = ""
        ctrl.deployUsernameTextField.text = ""
    }

    fun save() {

        project.project.fileManager.hosts.clear()
        ctrl.deployList.items.forEach {
            project.project.fileManager.hosts.addElement(it)
        }
        project.project.fileManager.writeHosts()
    }

    private fun current() = ctrl.deployList.selectionModel.selectedItem
}