package com.skriptide.gui

import com.skriptide.core.management.OpenProject
import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuItem
import javafx.scene.control.TreeView
import java.io.File

object Menus {

    fun getMenuForRootProject(project:OpenProject, parent: TreeView<String>, x:Double, y:Double): ContextMenu {

        val menu = ContextMenu()

        val newFileItem = MenuItem("Create new File")

        newFileItem.setOnAction {

            val name = Prompts.textPrompt("Create new File", "Enter File name Here")

            if(name.isNotEmpty()) project.createNewFile(name)

        }
        menu.items.addAll(newFileItem)



        menu.show(parent, x, y)

        return menu
    }
    fun getMenuForProjectFile(project:OpenProject, holder: File, parent:TreeView<String>, x:Double, y:Double): ContextMenu {

        val menu = ContextMenu()

        val renameItem = MenuItem("Rename")
        val deleteItem = MenuItem("Delete")


        menu.items.addAll(renameItem, deleteItem)



        menu.show(parent, x, y)

        return menu

    }


}