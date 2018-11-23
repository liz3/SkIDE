package com.skide.gui

import com.skide.core.management.OpenProject
import com.skide.include.EditorMode
import com.skide.include.OpenFileHolder
import javafx.scene.control.Alert
import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuItem
import javafx.scene.control.TreeView
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import java.io.File

object Menus {

    fun getMenuForRootProject(project: OpenProject, parent: TreeView<String>, x: Double, y: Double): ContextMenu {

        val menu = ContextMenu()

        val openIcon = Image(javaClass.getResource("/images/icon.png").toExternalForm())
        val yaml = Image(javaClass.getResource("/images/yaml.png").toExternalForm())

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
            project.manager.toggleExclude()

        }
        val splitSide = MenuItem("Split vertically")
        splitSide.setOnAction {
            if (project.tab.tabPane != null && project.tab.tabPane.tabs.size > 1) {
                try {
                    project.openProject.guiHandler.switchMode(EditorMode.SIDE_SPLIT)
                    project.openProject.guiHandler.addTabPane(project.tab)
                }catch (e:Exception) {
                    e.printStackTrace()
                }
            }

        }
        val splitDown = MenuItem("Split Horizontally")
        splitDown.setOnAction {
            if (project.tab.tabPane != null && project.tab.tabPane.tabs.size > 1) {
                project.openProject.guiHandler.switchMode(EditorMode.DOWN_SPLIT)
                project.openProject.guiHandler.addTabPane(project.tab)
            }
        }


        menu.items.addAll(newWindowItem, splitSide, splitDown)
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