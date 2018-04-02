package com.skide.gui

import com.skide.core.code.CodeManager
import com.skide.core.management.OpenProject
import com.skide.gui.controllers.SkunityQuestionFameController
import com.skide.include.OpenFileHolder
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.text.Font
import javafx.stage.Popup
import java.io.File

object Menus {

    fun getMenuForRootProject(project: OpenProject, parent: TreeView<String>, x: Double, y: Double): ContextMenu {

        val menu = ContextMenu()

        val openIcon = Image(javaClass.getResource("/icon.png").toExternalForm())
        val yaml = Image(javaClass.getResource("/yaml.png").toExternalForm())

        val yamlView = ImageView(yaml)
        yamlView.fitWidth = 25.0
        yamlView.fitHeight = 15.0

        val openView = ImageView(openIcon)
        openView.fitWidth = 15.0
        openView.fitHeight = 15.0

        val newFileItem = MenuItem("New Skript File")
        newFileItem.graphic = openView

        newFileItem.setOnAction {

            val name = Prompts.textPrompt("New Skript File", "Enter File name Here")

            if (name.isNotEmpty()) project.createNewFile(name)

        }
        val newYamlFile = MenuItem("New Yaml File")
        newYamlFile.graphic = yamlView
        newYamlFile.setOnAction {

            var name = Prompts.textPrompt("New Yaml File", "Enter File name Here")

            if (!name.endsWith(".yml") && !name.endsWith(".yaml")) name += ".yml"
            if (name.isNotEmpty()) project.createNewFile(name)

        }
        menu.items.addAll(newFileItem, newYamlFile)



        menu.show(parent, x, y)

        return menu
    }

    fun getMenuForRootPane(project: OpenFileHolder): ContextMenu {

        val menu = ContextMenu()
        val newWindowItem = MenuItem("Open in new Window")
        newWindowItem.setOnAction {
            project.toggleExlude()

        }

        menu.items.addAll(newWindowItem)
        return menu
    }

    fun getMenuForArea(codeManager: CodeManager, x: Double, y: Double): ContextMenu {

        val defaultFont = Font.getDefault()

        val menu = ContextMenu()

        menu.style = "-fx-font-family: ${defaultFont.name} !important; -fx-font-size: ${defaultFont.size}"
        val copyEntry = MenuItem("Copy")
        copyEntry.setOnAction {
            codeManager.area.copy()
        }
        val pasteEntry = MenuItem("Paste")
        pasteEntry.setOnAction {
            codeManager.area.paste()
        }
        val skUnityEntry = MenuItem("Ask on skUnity")

        skUnityEntry.setOnAction {

            val scene = GUIManager.getWindow("SkUnityQuestionFrame.fxml", "Ask on skUnity", true)
            val controller = scene.controller as SkunityQuestionFameController

            val popUp = Popup()

            controller.cancelBtn.setOnAction {
                popUp.hide()
            }

            controller.contentArea.text = "[CODE=SKRIPT]${codeManager.area.selectedText}[/CODE]\n"


            controller.cancelBtn.setOnAction {
                scene.stage.close()

            }
            controller.sendBtn.setOnAction {
                val title = controller.titleBar.text
                val msg = controller.contentArea.text

                if (title != "" || msg != "") {
                    codeManager.findHandler.project.coreManager.skUnity.report(title, msg)

                    scene.stage.close()
                }

            }

        }


        val compileMenu = Menu("Export/Compile")
        for ((name, opt) in codeManager.findHandler.project.openProject.project.fileManager.compileOptions) {

            val compileEntry = MenuItem(name)
            compileEntry.setOnAction {

                codeManager.findHandler.project.openProject.guiHandler.openFiles.forEach { it.value.saveCode() }
                codeManager.findHandler.project.openProject.compiler.compile(codeManager.findHandler.project.openProject.project, opt,
                        codeManager.findHandler.project.openProject.guiHandler.lowerTabPaneEventManager.setupBuildLogTabForInput())
            }
            compileMenu.items.add(compileEntry)
        }
        if (codeManager.area.selectedText.isNotEmpty()) menu.items.add(copyEntry)
        menu.items.add(pasteEntry)
        if (codeManager.findHandler.project.coreManager.skUnity.loggedIn) menu.items.add(skUnityEntry)
        menu.items.add(compileMenu)
        val runFileMenu = Menu("Run this File")
        codeManager.findHandler.project.coreManager.serverManager.servers.forEach {

            val serverItem = MenuItem(it.value.configuration.name)
            serverItem.setOnAction { _ ->
                codeManager.findHandler.project.openProject.run(it.value, codeManager.findHandler.project)
            }
            runFileMenu.items.add(serverItem)
        }
        val runConfMenu = Menu("Run Configuration")
        for ((name, opt) in codeManager.findHandler.project.openProject.project.fileManager.compileOptions) {
            val confItem = Menu(name)
            codeManager.findHandler.project.coreManager.serverManager.servers.forEach {

                val serverItem = MenuItem(it.value.configuration.name)
                serverItem.setOnAction { _ ->
                    codeManager.findHandler.project.openProject.guiHandler.openFiles.forEach { it.value.saveCode() }
                    codeManager.findHandler.project.openProject.run(it.value, opt)
                }
                confItem.items.add(serverItem)
            }
            runConfMenu.items.add(confItem)


        }


        menu.items.add(runFileMenu)
        menu.items.add(runConfMenu)
        menu.show(codeManager.area, x, y)

        return menu
    }

    fun getMenuForProjectFile(project: OpenProject, holder: File, parent: TreeView<String>, x: Double, y: Double): ContextMenu {

        val menu = ContextMenu()

        val renameItem = MenuItem("Rename")
        val deleteItem = MenuItem("Delete")

        renameItem.setOnAction {

            val name = Prompts.textPrompt("Rename File", "Enter the new File name Here")

            if (name.isNotEmpty()) {

                val newName = if (name.contains(".")) name else name + ".sk"
                project.reName(holder.name, newName, holder.absolutePath)
            }

        }
        deleteItem.setOnAction {

            if (Prompts.infoCheck("Delete file", "Delete ${holder.name}", "Are you sure you want do delete this file?", Alert.AlertType.CONFIRMATION))
                project.delete(holder)

        }

        menu.items.addAll(renameItem, deleteItem)



        menu.show(parent, x, y)

        return menu

    }


}