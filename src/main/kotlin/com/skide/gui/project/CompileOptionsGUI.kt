package com.skide.gui.project

import com.skide.core.management.OpenProject
import com.skide.gui.controllers.ProjectSettingsGUIController
import com.skide.include.CompileOption
import com.skide.include.CompileOptionType
import javafx.stage.DirectoryChooser
import javafx.stage.Stage
import java.io.File

class CompileOptionsGUI(val project: OpenProject, val ctrl: ProjectSettingsGUIController) {


    val allOptions = project.project.fileManager.compileOptions

    fun applyCurr(old: CompileOption = current()): Boolean {

        if (!old.outputDir.exists()) return false
        project.project.fileManager.writeCompileOptions()
        return true
    }

    private fun updateActive(value:Boolean) {

        ctrl.compileOutPutPathField.isDisable = value
        ctrl.compileMethodComboBox.isDisable = value
        ctrl.removeCommentsCheck.isDisable = value
        ctrl.removeEmptyLinesCheck.isDisable = value
        ctrl.compileDelConfBtn.isDisable = value
        ctrl.compileOutPutBtn.isDisable = value
        ctrl.compileIncludedFileList.isDisable = value
        ctrl.compileExcludedFileList.isDisable = value

    }
    fun init() {

        ctrl.compileMethodComboBox.items.addAll(CompileOptionType.CONCATENATE, CompileOptionType.PER_FILE, CompileOptionType.JAR)
        allOptions.values.forEach {
            ctrl.compileConfListView.items.add(it)
        }

        ctrl.compileConfListView.selectionModel.selectedItemProperty().addListener { _, oldValue, newVal ->



                updateActive(newVal == null)


            if (oldValue != null && !applyCurr(oldValue)) return@addListener
            insertCurrentValues()
        }
        ctrl.compileNewConfBtn.setOnAction {
            val name = ctrl.compileNewOptionsNameField.text
            if (allOptions.containsKey(name) || name.isEmpty() || name.isBlank()) return@setOnAction


            val option = CompileOption(name, project.project.folder, CompileOptionType.CONCATENATE, true, true, false, 0)
            option.includedFiles += project.project.fileManager.projectFiles.values

            project.project.fileManager.addCompileOption(option)

            ctrl.compileConfListView.items.add(option)

            ctrl.compileConfListView.selectionModel.select(option)

            ctrl.compileDelConfBtn.isDisable = false
        }
        ctrl.compileMethodComboBox.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            current().method = newValue
        }
        ctrl.compileIncludedFileList.setOnMouseClicked { ev ->
            if (ev.clickCount == 2) {
                if (ctrl.compileIncludedFileList.selectionModel.selectedItem != null) {

                    val currentItem = ctrl.compileIncludedFileList.selectionModel.selectedItem as File

                    current().includedFiles.remove(currentItem)
                    current().excludedFiles.add(currentItem)
                    ctrl.compileIncludedFileList.items.remove(currentItem)
                    ctrl.compileExcludedFileList.items.add(currentItem)
                }
            }
        }
        ctrl.compileOutPutBtn.setOnAction {
            val fileChooserWindow = Stage()
            val dirChooser = DirectoryChooser()
            dirChooser.title = "Choose output folder"
            val dir = dirChooser.showDialog(fileChooserWindow)
            if (dir != null) {
                current().outputDir = dir
                ctrl.compileOutPutPathField.text = dir.absolutePath

            }

        }
        ctrl.compileExcludedFileList.setOnMouseClicked { ev ->
            if (ev.clickCount == 2) {
                if (ctrl.compileExcludedFileList.selectionModel.selectedItem != null) {

                    val currentItem = ctrl.compileExcludedFileList.selectionModel.selectedItem as File

                    current().excludedFiles.remove(currentItem)
                    current().includedFiles.add(currentItem)
                    ctrl.compileIncludedFileList.items.add(currentItem)
                    ctrl.compileExcludedFileList.items.remove(currentItem)
                }
            }
        }
        ctrl.removeCommentsCheck.setOnAction {
            current().remComments = ctrl.removeCommentsCheck.isSelected
        }
        ctrl.removeEmptyLinesCheck.setOnAction {
            current().remEmptyLines = ctrl.removeEmptyLinesCheck.isSelected
        }
        ctrl.compileObfuscateCheck.setOnAction {
            current().obsfuscate = ctrl.compileObfuscateCheck.isSelected
            ctrl.obsfsucateLevelComboBox.isDisable = current().obsfuscate
        }
        ctrl.compileDelConfBtn.setOnAction {
            project.project.fileManager.delCompileOption(current().name)
            ctrl.compileConfListView.items.remove(current())
            if (allOptions.size == 1) ctrl.compileDelConfBtn.isDisable = true
        }


        ctrl.compileConfListView.selectionModel.select(0)
        insertCurrentValues()

    }


    private fun insertCurrentValues() {
        ctrl.compileIncludedFileList.items.clear()
        ctrl.compileExcludedFileList.items.clear()
        ctrl.compileOutPutPathField.text = current().outputDir.absolutePath
        ctrl.compileMethodComboBox.selectionModel.select(current().method)
        ctrl.removeCommentsCheck.isSelected = current().remComments
        ctrl.removeEmptyLinesCheck.isSelected = current().remEmptyLines
        ctrl.compileObfuscateCheck.isSelected = current().obsfuscate


        current().includedFiles.forEach {
            ctrl.compileIncludedFileList.items.add(it)
        }
        current().excludedFiles.forEach {
            ctrl.compileExcludedFileList.items.add(it)
        }
    }

    private fun current() = ctrl.compileConfListView.selectionModel.selectedItem as CompileOption
}