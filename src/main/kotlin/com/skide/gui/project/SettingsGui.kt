package com.skide.gui.project

import com.skide.CoreManager
import com.skide.gui.GUIManager
import com.skide.gui.controllers.ProjectSettingsGUIController
import com.skide.include.ActiveWindow
import com.skide.include.Addon
import javafx.stage.Modality
import java.util.*
import kotlin.collections.HashMap

class SettingsGui(val coreManager: CoreManager, val projGuiManager: OpenProjectGuiManager) {

    val project = projGuiManager.openProject.project

    var loaded = false
    val window: ActiveWindow by lazy {
        loaded = true
        GUIManager.getWindow("fxml/ProjectSettingsGui.fxml", "Settings for ${project.name}", false)
    }


    fun show() {
        if (!loaded) {
            val co = CompileOptionsGUI(projGuiManager.openProject, window.controller as ProjectSettingsGUIController)
            co.init()
            val dOpts = DeployOptionsGUI(projGuiManager.openProject, window.controller as ProjectSettingsGUIController)
            dOpts.initDeployModule()
            window.stage.initModality(Modality.WINDOW_MODAL)
            window.stage.initOwner(projGuiManager.window.stage)
            SettingsGuiEventListener(this, window.controller as ProjectSettingsGUIController, co, dOpts).init()
        }

        if (window.stage.isShowing) return
        window.stage.isResizable = false
        window.stage.show()
    }

}

enum class SettingsChangeType {
    NAME_CHANGE,
    SKRIPT_VERSION_CHANGE,
    ADDON_ADD,
    ADDON_REMOVE,
    ADDON_ALTER
}

class SettingsGuiEventListener(val gui: SettingsGui, val ctrl: ProjectSettingsGUIController, val co: CompileOptionsGUI, val dOpts: DeployOptionsGUI) {


    private val resourceManager = gui.coreManager.resourceManager
    private val currentInstalled = gui.projGuiManager.openProject.project.fileManager.addons
    val changes = Vector<Pair<Addon, SettingsChangeType>>()
    private val alterValues = HashMap<Addon, String>()

    var loaded = false

    private fun updateState() {
        if (currItem() != null) {
            ctrl.enableSupportCheckBox.isDisable = false
            val item = currItem()
            ctrl.plNameLabel.text = "Name: ${item.name}"
            ctrl.plAuthorLabel.text = "Author: ${item.author}"
            ctrl.enableSupportCheckBox.isSelected = {
                var endVal = false
                var found = false
                changes.forEach {
                    if (it.first == currItem() && it.second != SettingsChangeType.ADDON_ALTER) {
                        found = true
                        endVal = it.second == SettingsChangeType.ADDON_ADD
                    }
                }
                if (!found) {
                    endVal = currentInstalled.containsKey(item.name)
                }
                endVal
            }.invoke()
        }
    }

    fun init() {

        ctrl.plListView.items.clear()
        resourceManager.addons.forEach {
            ctrl.plListView.items.add(it.value)
        }
        ctrl.prNameTextField.text = gui.project.name
        ctrl.plNameLabel.text = ""
        ctrl.plDescriptionLabel.text = ""
        ctrl.plAuthorLabel.text = ""
        if (!loaded) {
            loaded = true

            ctrl.skriptVersionComboBox.isDisable = true
            if (ctrl.skriptVersionComboBox.items.contains(gui.project.skriptVersion))
                ctrl.skriptVersionComboBox.selectionModel.select(gui.project.skriptVersion)

            ctrl.plListView.selectionModel.selectedItemProperty().addListener { _, _, _ ->

                updateState()
            }
            ctrl.enableSupportCheckBox.setOnAction {
                val selected = ctrl.enableSupportCheckBox.isSelected
                if (selected) {
                    var toRemove: Pair<Addon, SettingsChangeType>? = null
                    changes.forEach {
                        if (it.first == currItem()) {
                            if (it.second == SettingsChangeType.ADDON_REMOVE || it.second == SettingsChangeType.ADDON_ADD) toRemove = it
                        }
                    }
                    if (toRemove != null) changes.remove(toRemove)
                    changes.addElement(Pair(currItem(), SettingsChangeType.ADDON_ADD))
                    alterValues[currItem()] = "default"
                } else {
                    var toRemove: Pair<Addon, SettingsChangeType>? = null
                    changes.forEach {
                        if (it.first == currItem()) {
                            if (it.second == SettingsChangeType.ADDON_REMOVE || it.second == SettingsChangeType.ADDON_ADD) toRemove = it
                        }
                    }
                    if (toRemove != null) changes.remove(toRemove)
                    changes.addElement(Pair(currItem(), SettingsChangeType.ADDON_REMOVE))
                }
            }
            ctrl.plVersionsComboBox.isDisable = true
            ctrl.enableSupportCheckBox.isDisable = true
            ctrl.applyBtn.setOnAction {
                performChanges()
            }
            ctrl.okBtn.setOnAction {
                performChanges()
                gui.window.close()
                gui.window.stage.close()
            }
            ctrl.cancelBtn.setOnAction {
                gui.window.close()
                gui.window.stage.close()
            }
        }

    }

    private fun performChanges() {

        val selectedSkriptVersion = ctrl.skriptVersionComboBox.selectionModel.selectedItem
        if (selectedSkriptVersion != null && selectedSkriptVersion.isNotEmpty())
            gui.project.skriptVersion = selectedSkriptVersion

        alterValues.forEach {
            currentInstalled[it.key.name] = it.value
        }
        changes.forEach {
            if (it.second == SettingsChangeType.ADDON_REMOVE) {
                if (currentInstalled.containsKey(it.first.name)) currentInstalled.remove(it.first.name)
            }
            if (it.second == SettingsChangeType.ADDON_ADD) {
                if (currentInstalled.containsKey(it.first.name)) {
                    currentInstalled.remove(it.first.name)
                    val alterValue = alterValues[it.first]

                    currentInstalled[it.first.name] = alterValue!!
                    alterValues.remove(it.first)

                }
            }
        }
        if (ctrl.prNameTextField.text != gui.projGuiManager.openProject.project.name) {
            gui.projGuiManager.openProject.renameProject(ctrl.prNameTextField.text)
        }
        gui.projGuiManager.openProject.project.fileManager.rewriteConfig()
        changes.clear()
        alterValues.clear()
        gui.projGuiManager.openProject.updateAddons()
        co.applyCurr()
        dOpts.save()

        updateState()
    }

    private fun currItem() = ctrl.plListView.selectionModel.selectedItem
}