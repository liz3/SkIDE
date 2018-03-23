package com.skide.gui.settings

import com.skide.CoreManager
import com.skide.gui.controllers.GeneralSettingsGUIController
import com.skide.include.ActiveWindow
import com.skide.include.Server
import com.skide.include.ServerAddon
import com.skide.include.ServerConfiguration
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Stage
import java.io.File
import java.util.*

class SettingsGUIHandler(val ctrl: GeneralSettingsGUIController, val coreManager: CoreManager, val window: ActiveWindow) {

    var newServerAdded = false

    val serverManager = coreManager.serverManager
    val resourceManager = coreManager.resourceManager
    val deleted = Vector<Server>()


    fun init() {


        ctrl.okBtn.setOnAction {
            deleted.forEach {
                serverManager.deleteServer(it)
            }
            if (currentSelected() != null) {
                if (newServerAdded) {
                    newServerAdded = false
                }
                serverManager.saveServerConfigution(currentSelected())
            }

            deleted.clear()
            window.stage.close()
        }
        ctrl.applyBtn.setOnAction {
            deleted.forEach {
                serverManager.deleteServer(it)
            }
            if (currentSelected() != null) {
                if (newServerAdded) {
                    newServerAdded = false
                }
                serverManager.saveServerConfigution(currentSelected())
            }

            deleted.clear()
        }
        ctrl.cancelBtn.setOnAction {
            window.stage.close()

        }
        ctrl.serverStartAgsTextField.textProperty().addListener { observable, oldValue, newValue ->

            currentSelected().configuration.startAgrs = newValue
        }
        ctrl.serverServerList.selectionModel.selectedItemProperty().addListener { observable, oldValue, newValue ->

            if (oldValue == null) {
                if (!newServerAdded) {
                    setNewValues()
                }
                return@addListener
            }
            if (newServerAdded) {
                serverManager.createServer(oldValue)
                newServerAdded = false
            } else {
                if (!deleted.contains(oldValue)) serverManager.saveServerConfigution(oldValue)
            }
            if (!newServerAdded && newValue != null) setNewValues()
        }
        ctrl.serverServertPathChooseBtn.setOnAction {
            val file = getFile("Choose the Bukkit/Spigot Jar File")
            if (file != null) {
                currentSelected().configuration.apiPath = file
                ctrl.serverServerPathTextField.text = file.absolutePath
            }
        }
        ctrl.serverServerFolderPathChooseBtn.setOnAction {
            val file = getDir("Choose the Folder path")
            if (file != null) {
                currentSelected().configuration.folder = file
                currentSelected().confFile = File(file, ".server.Sk-IDE")
                ctrl.serverServerFolderPathTextField.text = file.absolutePath
            }
        }
        ctrl.serverAddAddonFromFileChooseBtn.setOnAction {
            val file = getFile("Choose the Addon File")
            if (file != null) {
                ctrl.serverAddAddonFromFileTextField.text = file.absolutePath
            }
        }
        ctrl.serverAddAddonFromFileBtn.setOnAction {
            val file = File(ctrl.serverAddAddonFromFileTextField.text)

            if (file.exists()) {
                currentSelected().configuration.addons.forEach {
                    if (it.file.absolutePath == file.absolutePath) return@setOnAction
                    println("Returning")
                }
                val item = ServerAddon(file.name, file, false)
                currentSelected().configuration.addons.addElement(item)
                ctrl.serverAddonList.items.add(item)
            }
        }
        ctrl.serverSkriptVersionComboBox.setOnAction {
            if (currentSelected() != null) currentSelected().configuration.skriptVersion = ctrl.serverSkriptVersionComboBox.selectionModel.selectedItem as String
        }
        ctrl.serverServerDeleteBtn.setOnAction {
            deleted.add(currentSelected())
            ctrl.serverServerList.items.remove(currentSelected())

        }
        ctrl.serverAddonDeleteBtn.setOnAction {
            if (ctrl.serverAddonList.selectionModel.selectedItem != null) {
                val item = ctrl.serverAddonList.selectionModel.selectedItem
                ctrl.serverAddonList.items.remove(item)
                currentSelected().configuration.addons.remove(item)
                println()
            }
        }
        ctrl.serverNewServerCreateBtn.setOnAction {
            val name = ctrl.serverNewServerNameTextField.text
            if (serverManager.servers.containsKey(name)) return@setOnAction
            newServerAdded = true
            val server = Server(ServerConfiguration(name, "", File(""), File(""), ""), File(""), false, System.currentTimeMillis())
            ctrl.serverServerList.items.add(server)
            ctrl.serverServerList.selectionModel.select(server)
            clearValues()
            ctrl.serverServerNameTextField.text = name
        }

        serverManager.servers.forEach {
            ctrl.serverServerList.items.add(it.value)
        }
        ctrl.serverSkriptVersionComboBox.items.addAll(coreManager.resourceManager.skriptVersions)
    }

    private fun setNewValues() {

        ctrl.serverServerNameTextField.text = currentSelected().configuration.name
        ctrl.serverServerFolderPathTextField.text = currentSelected().configuration.folder.absolutePath
        ctrl.serverServerPathTextField.text = currentSelected().configuration.apiPath.absolutePath
        ctrl.serverSkriptVersionComboBox.selectionModel.select(currentSelected().configuration.skriptVersion)
        ctrl.serverAddonList.items.clear()
        currentSelected().configuration.addons.forEach {
            ctrl.serverAddonList.items.add(it)
        }
    }

    private fun clearValues() {


        ctrl.serverServerNameTextField.text = ""
        ctrl.serverServerFolderPathTextField.text = ""
        ctrl.serverServerPathTextField.text = ""
        ctrl.serverSkriptVersionComboBox.selectionModel.select(0)
        ctrl.serverAddonList.items.clear()
    }


    private fun getFile(name: String): File? {

        val fileChooserWindow = Stage()
        val dirChooser = FileChooser()
        dirChooser.title = name
        dirChooser.extensionFilters.addAll(
                FileChooser.ExtensionFilter("JAR", "*.jar")
        )
        return dirChooser.showOpenDialog(fileChooserWindow)
    }

    fun getDir(name: String): File? {

        val fileChooserWindow = Stage()
        val dirChooser = DirectoryChooser()
        dirChooser.title = name
        return dirChooser.showDialog(fileChooserWindow)
    }

    private fun currentSelected() = ctrl.serverServerList.selectionModel.selectedItem
}