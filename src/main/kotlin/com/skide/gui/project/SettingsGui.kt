package com.skide.gui.project

import com.skide.CoreManager
import com.skide.gui.GuiManager
import com.skide.gui.controllers.ProjectSettingsGuiController
import com.skide.include.ActiveWindow
import com.skide.include.Addon
import java.util.*
import kotlin.collections.HashMap

class SettingsGui(val coreManager: CoreManager, val projGuiManager: OpenProjectGuiManager) {

    val project = projGuiManager.openProject.project

    var loaded = false
    val window: ActiveWindow by lazy {
        loaded = true
        GuiManager.getWindow("ProjectSettingsGui.fxml", "Settings for ${project.name}", false)
    }


    fun show() {
        if (!loaded) SettingsGuiEventListener(this, window.controller as ProjectSettingsGuiController).init()

        if (window.stage.isShowing) return
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

class SettingsGuiEventListener(val gui: SettingsGui, val ctrl: ProjectSettingsGuiController) {


    val resourceManager = gui.coreManager.resourceManager

    val currentInstalled = gui.projGuiManager.openProject.project.fileManager.addons

    val changes = Vector<Pair<Addon,SettingsChangeType>>()

    val alterValues = HashMap<Addon, String>()

    var loaded = false

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
            ctrl.plListView.selectionModel.selectedItemProperty().addListener { observable, oldValue, newValue ->

                if (currItem() != null) {

                    val item = currItem()
                    ctrl.plNameLabel.text = "Name: ${item.name}"
                    ctrl.plAuthorLabel.text = "Author ${item.author}"
                    ctrl.enableSupportCheckBox.isSelected = {
                        var endVal = false
                        var found = false
                        changes.forEach {
                            if(it.first == currItem() && it.second != SettingsChangeType.ADDON_ALTER) {
                                found = true
                                endVal = it.second == SettingsChangeType.ADDON_ADD
                            }
                        }
                        if(!found) {
                            endVal = currentInstalled.containsKey(item.name)
                        }
                        endVal
                    }.invoke()
                    ctrl.plVersionsComboBox.items.clear()
                    item.versions.keys.forEach {
                        ctrl.plVersionsComboBox.items.add(it)
                    }
                    if(alterValues.containsKey(currItem())) {
                        ctrl.plVersionsComboBox.selectionModel.select(alterValues[currItem()])
                    }
                    if (currentInstalled.containsKey(item.name)) ctrl.plVersionsComboBox.selectionModel.select(currentInstalled[item.name])

                    if(ctrl.plVersionsComboBox.selectionModel.selectedItem == null) {
                        ctrl.enableSupportCheckBox.isDisable = true
                    }
                }
            }
            ctrl.enableSupportCheckBox.setOnAction {
                val selected = ctrl.enableSupportCheckBox.isSelected
                if(selected) {
                    var toRemove:Pair<Addon, SettingsChangeType>? = null
                    changes.forEach {
                        if(it.first == currItem()) {
                            if(it.second == SettingsChangeType.ADDON_REMOVE ||it.second == SettingsChangeType.ADDON_ADD) toRemove = it
                        }
                    }
                    if(toRemove != null) changes.remove(toRemove)
                    changes.addElement(Pair(currItem(), SettingsChangeType.ADDON_ADD))
                } else {
                    var toRemove:Pair<Addon, SettingsChangeType>? = null
                    changes.forEach {
                        if(it.first == currItem()) {
                            if(it.second == SettingsChangeType.ADDON_REMOVE || it.second == SettingsChangeType.ADDON_ADD) toRemove = it
                        }
                    }
                    if(toRemove != null) changes.remove(toRemove)
                    changes.addElement(Pair(currItem(), SettingsChangeType.ADDON_REMOVE))                }
            }
            ctrl.plVersionsComboBox.selectionModel.selectedItemProperty().addListener { observable, oldValue, newValue ->

                ctrl.enableSupportCheckBox.isDisable = false
                if(ctrl.plVersionsComboBox.selectionModel.selectedItem == null) return@addListener
                var toRemove:Pair<Addon, SettingsChangeType>? = null
                changes.forEach {
                    if(it.first == currItem()) {
                        if(it.second == SettingsChangeType.ADDON_ALTER) toRemove = it
                    }
                }
                if(toRemove != null) changes.remove(toRemove)
                val elem = Pair(currItem(), SettingsChangeType.ADDON_ALTER)
                changes.addElement(elem)
                alterValues[currItem()] = ctrl.plVersionsComboBox.selectionModel.selectedItem


            }
            ctrl.applyBtn.setOnAction {
                performChanges()
            }
            ctrl.okBtn.setOnAction {
                performChanges()
            }
        }

    }

    private fun performChanges() {

        alterValues.forEach {
            currentInstalled[it.key.name] = it.value
        }
        changes.forEach {
            if(it.second == SettingsChangeType.ADDON_REMOVE) {
               if(currentInstalled.containsKey(it.first.name)) currentInstalled.remove(it.first.name)
            }
            if(it.second == SettingsChangeType.ADDON_ADD) {
               if(currentInstalled.containsKey(it.first.name)) {
                   currentInstalled.remove(it.first.name)
                   val alterValue = alterValues[it.first]
                   currentInstalled[it.first.name] = alterValue!!
                   alterValues.remove(it.first)
               }
            }
        }


        if(ctrl.prNameTextField.text != gui.projGuiManager.openProject.project.name) {
            gui.projGuiManager.openProject.renameProject(ctrl.prNameTextField.text)
        }
        gui.projGuiManager.openProject.project.fileManager.rewriteConfig()
        changes.clear()
        alterValues.clear()
        gui.projGuiManager.openProject.updateAddons()
    }
    private fun currItem() = ctrl.plListView.selectionModel.selectedItem
}