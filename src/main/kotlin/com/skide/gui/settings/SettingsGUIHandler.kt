package com.skide.gui.settings

import com.skide.CoreManager
import com.skide.gui.Prompts
import com.skide.gui.controllers.GeneralSettingsGUIController
import com.skide.include.ActiveWindow
import com.skide.include.Server
import com.skide.include.ServerAddon
import com.skide.include.ServerConfiguration
import com.skide.utils.OperatingSystemType
import com.skide.utils.getOS
import com.skide.utils.restart
import javafx.application.Platform
import javafx.scene.control.Alert
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
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


    private fun updateSettings() {

        coreManager.configManager.set("highlighting", "${ctrl.settingsHighlightingCheck.isSelected}")
        coreManager.configManager.set("theme", ctrl.settingsTheneComboBox.selectionModel.selectedItem)
        coreManager.configManager.set("auto_complete", "${ctrl.settingsAutoCompleteCheck.isSelected}")
        coreManager.configManager.set("cross_auto_complete", "${ctrl.crossFileAutoComplete.isSelected}")
        coreManager.configManager.set("font", ctrl.settingsFontTextField.text)
        coreManager.configManager.set("font_size", ctrl.settingsFontSizeTextField.text)

        if(coreManager.configManager.get("jre_home") == "" && getOS() == OperatingSystemType.MAC_OS) {
            Platform.runLater {
                val maybe = String(Runtime.getRuntime().exec("echo \$JAVA_HOME").inputStream.readBytes())

                coreManager.configManager.set("jre_home", Prompts.textPrompt("JRE Path", "SK-IDE needs a JRE/JDK for Servers, please enter the Path to the java home here", maybe))
            }
        }
    }

    private fun setShortcut(ev: KeyEvent, field: TextField, key: String) {


        if (ev.code == KeyCode.SHIFT || ev.code == KeyCode.CONTROL || ev.code == KeyCode.ALT || ev.code == KeyCode.ALT_GRAPH) return

        var content = ev.code.toString()

        if (ev.isShiftDown) content = "SHIFT+$content"
        if (ev.isAltDown) content = "ALT+$content"
        if (ev.isControlDown) content = "CONTROL+$content"

        coreManager.configManager.set(key, content)

        Platform.runLater {
            field.text = content
        }
    }

    fun init() {


        ctrl.keyBracketField.text = coreManager.configManager.get("bracket_cut").toString()
        ctrl.keyBracketField.setOnKeyPressed {
            setShortcut(it, ctrl.keyBracketField, "bracket_cut")
        }
        ctrl.keyCurlyBracket.text = coreManager.configManager.get("curly_cut").toString()
        ctrl.keyCurlyBracket.setOnKeyPressed {
            setShortcut(it, ctrl.keyCurlyBracket, "curly_cut")
        }

        ctrl.keyParenField.text = coreManager.configManager.get("paren_cut").toString()
        ctrl.keyParenField.setOnKeyPressed {
            setShortcut(it, ctrl.keyParenField, "paren_cut")
        }

        ctrl.keyQuoteField.text = coreManager.configManager.get("quote_cut").toString()
        ctrl.keyQuoteField.setOnKeyPressed {
            setShortcut(it, ctrl.keyQuoteField, "quote_cut")
        }

        ctrl.autoCompleteCutField.text = coreManager.configManager.get("ac_cut").toString()
        ctrl.autoCompleteCutField.setOnKeyPressed {
            setShortcut(it, ctrl.autoCompleteCutField, "ac_cut")
        }

        ctrl.fixesCutField.text = coreManager.configManager.get("fx_cut").toString()
        ctrl.fixesCutField.setOnKeyPressed {
            setShortcut(it, ctrl.fixesCutField, "fx_cut")
        }



        ctrl.okBtn.setOnAction {
            deleted.forEach {
                serverManager.deleteServer(it)
            }
            if (currentSelected() != null) {
                if (newServerAdded) {
                    newServerAdded = false
                }
                if (currentSelected() != null) serverManager.saveServerConfigution(currentSelected()!!)
            }

            deleted.clear()
            updateSettings()
            window.stage.close()
            if (Prompts.infoCheck("Restart", "Sk-IDE restart", "In order to perform all changes, SkIde needs to be restarted!", Alert.AlertType.CONFIRMATION)) {
                restart()
            }
        }

        ctrl.applyBtn.setOnAction {
            deleted.forEach {
                serverManager.deleteServer(it)
            }
            if (currentSelected() != null) {
                if (newServerAdded) {
                    newServerAdded = false
                }
                if (currentSelected() != null) serverManager.saveServerConfigution(currentSelected()!!)
            }

            deleted.clear()
            updateSettings()
        }
        ctrl.cancelBtn.setOnAction {
            window.stage.close()

        }
        ctrl.serverStartAgsTextField.textProperty().addListener { _, _, newValue ->

            currentSelected()?.configuration?.startAgrs = newValue
        }
        ctrl.serverServerList.selectionModel.selectedItemProperty().addListener { _, oldValue, newValue ->

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
                currentSelected()?.configuration?.apiPath = file
                ctrl.serverServerPathTextField.text = file.absolutePath
            }
        }
        ctrl.serverServerFolderPathChooseBtn.setOnAction {
            val file = getDir("Choose the Folder path")
            if (file != null) {
                currentSelected()?.configuration?.folder = file
                currentSelected()?.confFile = File(file, ".server.skide")
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

                currentSelected()?.configuration?.addons?.forEach {
                    if (it.file.absolutePath == file.absolutePath) return@setOnAction
                }
                val item = ServerAddon(file.name, file, false)
                currentSelected()?.configuration?.addons?.addElement(item)
                ctrl.serverAddonList.items.add(item)
            }
        }
        ctrl.serverSkriptVersionComboBox.setOnAction {
            if (currentSelected() != null) currentSelected()?.configuration?.skriptVersion = ctrl.serverSkriptVersionComboBox.selectionModel.selectedItem as String
        }
        ctrl.serverServerDeleteBtn.setOnAction {
            deleted.add(currentSelected())
            ctrl.serverServerList.items.remove(currentSelected())

        }
        ctrl.serverAddonDeleteBtn.setOnAction {
            if (ctrl.serverAddonList.selectionModel.selectedItem != null) {
                val item = ctrl.serverAddonList.selectionModel.selectedItem
                ctrl.serverAddonList.items.remove(item)
                currentSelected()?.configuration?.addons?.remove(item)
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

        ctrl.settingsTheneComboBox.items.addAll("Light", "Dark")
        serverManager.servers.forEach {
            ctrl.serverServerList.items.add(it.value)
        }
        ctrl.serverSkriptVersionComboBox.items.addAll(coreManager.resourceManager.skriptVersions)

        ctrl.settingsAutoCompleteCheck.isSelected = coreManager.configManager.get("auto_complete") == "true"
        ctrl.crossFileAutoComplete.isSelected = coreManager.configManager.get("cross_auto_complete") == "true"
        ctrl.settingsHighlightingCheck.isSelected = coreManager.configManager.get("highlighting") == "true"
        ctrl.settingsTheneComboBox.selectionModel.select(coreManager.configManager.get("theme").toString())
        ctrl.settingsFontTextField.text = coreManager.configManager.get("font").toString()
        ctrl.settingsFontSizeTextField.text = coreManager.configManager.get("font_size").toString()
    }

    private fun setNewValues() {

        ctrl.serverServerNameTextField.text = currentSelected()?.configuration?.name
        ctrl.serverServerFolderPathTextField.text = currentSelected()?.configuration?.folder?.absolutePath
        ctrl.serverServerPathTextField.text = currentSelected()?.configuration?.apiPath?.absolutePath
        ctrl.serverSkriptVersionComboBox.selectionModel.select(currentSelected()?.configuration?.skriptVersion)
        ctrl.serverAddonList.items.clear()
        currentSelected()?.configuration?.addons?.forEach {
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

    private fun currentSelected(): Server? {

        if (ctrl.serverServerList.selectionModel.selectedItem == null) {
            Prompts.infoCheck("Error", "Create a Serer first", "Please create or select a Server first before assigning things!", Alert.AlertType.ERROR)
            return null
        }
        return ctrl.serverServerList.selectionModel.selectedItem
    }
}