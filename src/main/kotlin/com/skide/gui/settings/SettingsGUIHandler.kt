package com.skide.gui.settings

import com.skide.CoreManager
import com.skide.Info
import com.skide.gui.Prompts
import com.skide.gui.controllers.GeneralSettingsGUIController
import com.skide.include.ActiveWindow
import com.skide.include.Server
import com.skide.include.ServerAddon
import com.skide.include.ServerConfiguration
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
    val deleted = Vector<Server>()


    private fun updateSettings() {

        coreManager.configManager.set("highlighting", "${ctrl.settingsHighlightingCheck.isSelected}")
        coreManager.configManager.set("theme", ctrl.settingsTheneComboBox.selectionModel.selectedItem)
        coreManager.configManager.set("auto_complete", "${ctrl.settingsAutoCompleteCheck.isSelected}")
        coreManager.configManager.set("cross_auto_complete", "${ctrl.crossFileAutoComplete.isSelected}")
        coreManager.configManager.set("font", ctrl.settingsFontTextField.text)
        coreManager.configManager.set("font_size", ctrl.settingsFontSizeTextField.text)
        coreManager.configManager.set("generate_meta_data", "${ctrl.metaDataGenerateCheck.isSelected}")
        coreManager.configManager.set("meta_update", "${ctrl.settingsUpdateDataCheck.isSelected}")
        coreManager.configManager.set("webview_debug", "${ctrl.webViewDebuggerCheck.isSelected}")
        coreManager.configManager.set("analytics", "${ctrl.analyiticsCheck.isSelected}")
        coreManager.configManager.set("global_font_size", ctrl.globalFontSize.text)


        if (Info.prodMode) coreManager.configManager.writeUpdateFile(ctrl.updateCheck.isSelected, ctrl.betaUpdateCheck.isSelected)
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

            currentSelected()?.configuration?.startArgs = newValue
        }
        ctrl.jvmStartAgsTextField.textProperty().addListener { _, _, newValue ->

            currentSelected()?.configuration?.jvmArgs = newValue
        }
        ctrl.serverServerList.selectionModel.selectedItemProperty().addListener { _, oldValue, newValue ->

            updateFocusAllow(newValue == null)

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
            if (serverManager.servers.containsKey(name) || name.isEmpty() || name.isBlank()) return@setOnAction
            newServerAdded = true
            val server = Server(ServerConfiguration(name, "", File(""), File(""), "", ""), File(""), false, System.currentTimeMillis())
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
        ctrl.metaDataGenerateCheck.isSelected = coreManager.configManager.get("generate_meta_data") == "true"
        ctrl.settingsUpdateDataCheck.isSelected = coreManager.configManager.get("meta_update") == "true"
        ctrl.webViewDebuggerCheck.isSelected = coreManager.configManager.get("webview_debug") == "true"
        ctrl.analyiticsCheck.isSelected = coreManager.configManager.get("analytics") == "true"
        if (coreManager.configManager.get("global_font_size").toString().isNotEmpty())
            ctrl.globalFontSize.text = coreManager.configManager.get("global_font_size").toString()


        if (Info.prodMode) {
            ctrl.updateCheck.isSelected = coreManager.configManager.update
            ctrl.betaUpdateCheck.isSelected = coreManager.configManager.betaChannel
        }
        updateFocusAllow(true)
    }

    private fun updateFocusAllow(v:Boolean) {

        ctrl.jvmStartAgsTextField.isDisable = v
        ctrl.serverStartAgsTextField.isDisable = v
        ctrl.serverSkriptVersionComboBox.isDisable = v
        ctrl.serverAddAddonFromFileBtn.isDisable = v
        ctrl.serverServerFolderPathChooseBtn.isDisable = v
        ctrl.serverServertPathChooseBtn.isDisable = v
        ctrl.serverAddonDeleteBtn.isDisable = v
        ctrl.serverAddAddonFromFileChooseBtn.isDisable = v
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
            return null
        }
        return ctrl.serverServerList.selectionModel.selectedItem
    }
}