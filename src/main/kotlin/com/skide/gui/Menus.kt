package com.skide.gui

import com.skide.core.management.OpenProject
import com.skide.include.EditorMode
import com.skide.include.OpenFileHolder
import com.skide.utils.copyFileToClipboard
import com.skide.utils.openInExplorer
import com.skide.utils.readFile
import javafx.scene.control.Alert
import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuItem
import javafx.scene.control.TreeView
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import java.io.File

object Menus {

    fun getMenuForRootProject(project: OpenProject, parent: TreeView<String>, x: Double, y: Double): ContextMenu {

        val menu = ContextMenu()

        val openIcon = Image(javaClass.getResource("/images/icon.png").toExternalForm())

        val openView = ImageView(openIcon)
        openView.fitWidth = 15.0
        openView.fitHeight = 15.0

        val newFileItem = MenuItem("New Skript File")
        newFileItem.graphic = openView

        newFileItem.setOnAction {
            val name = Prompts.textPrompt("New Skript File", "Enter File name Here")
            if (name.isNotEmpty()) project.createNewFile(name)
        }
        val newDefaultFile = MenuItem("New File")
        newDefaultFile.setOnAction {
            val name = Prompts.textPrompt("New File", "Enter File name Here")
            if (name.isNotEmpty()) project.createNewFile(name)

        }
        val openInExplorer = MenuItem("Open in File Manager")
        openInExplorer.setOnAction {
            com.skide.utils.openInExplorer(project.project.folder)
        }
        val copyPath = MenuItem("Copy Path")
        copyPath.setOnAction {
            val cb = Clipboard.getSystemClipboard()
            val content = ClipboardContent()
            content.putString(project.project.folder.absolutePath)
            cb.setContent(content)
        }
        menu.items.addAll(newFileItem, newDefaultFile, copyPath, openInExplorer)



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
                } catch (e: Exception) {
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

        val open = MenuItem("Open")
        val renameItem = MenuItem("Rename")
        val showInExplorer = MenuItem("Open in File Manager")
        val copyOpt = MenuItem("Copy")
        val duplicate = MenuItem("Duplicate")
        val copyPath = MenuItem("Copy Path")
        val deleteItem = MenuItem("Delete")

        open.setOnAction {
            Thread {
                project.eventManager.openFile(holder)
            }.start()
        }
        showInExplorer.setOnAction {
            openInExplorer(holder)
        }
        duplicate.setOnAction {
            val name = Prompts.textPrompt("Rename File", "Enter the new File name Here", "${holder.nameWithoutExtension}_copy.${holder.extension}")
            if (name.isNotEmpty()) {
                if (project.guiHandler.openFiles.containsKey(holder)) {
                    val text = project.guiHandler.openFiles[holder]!!.area.text
                    project.createNewFile(name, text)
                } else {
                    project.createNewFile(name, readFile(holder).second)
                }
            }
        }
        renameItem.setOnAction {
            val name = Prompts.textPrompt("Rename File", "Enter the new File name Here")
            if (name.isNotEmpty()) {
                val newName = if (name.contains(".")) name else "$name.sk"
                project.reName(holder.absolutePath.substring(project.project.folder.absolutePath.length), newName, holder.absolutePath)
            }
        }
        deleteItem.setOnAction {
            if (Prompts.infoCheck("Delete file", "Delete ${holder.name}", "Are you sure you want do delete this file?", Alert.AlertType.CONFIRMATION))
                project.delete(holder)
        }
        copyOpt.setOnAction {
            copyFileToClipboard(holder)
        }
        copyPath.setOnAction {
            val cb = Clipboard.getSystemClipboard()
            val content = ClipboardContent()
            content.putString(holder.absolutePath)
            cb.setContent(content)
        }
        menu.items.addAll(open, renameItem, duplicate, copyPath, copyOpt, showInExplorer, deleteItem)
        menu.show(parent, x, y)
        return menu

    }


}
