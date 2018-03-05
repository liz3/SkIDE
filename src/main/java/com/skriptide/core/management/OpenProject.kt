package com.skriptide.core.management

import com.skriptide.CoreManager
import com.skriptide.gui.project.OpenProjectGuiManager
import com.skriptide.include.OpenFileHolder
import com.skriptide.include.Project
import java.io.File
import java.util.*

class OpenProject(val project: Project, val coreManager: CoreManager) {

    val guiHandler = OpenProjectGuiManager(this, coreManager)
    val eventManager = guiHandler.startGui()


    //TODO missing param for which file
    fun runFile() {

    }

    fun createNewFile(name: String) {
        project.fileManager.addFile(name)
        eventManager.updateProjectFilesTreeView()
    }

    fun reName(oldName: String, newName: String, path: String) {

        project.fileManager.reNameFile(oldName, newName)

        val toReplace = Vector<OpenFileHolder>()
        for ((file, holder) in guiHandler.openFiles) {
            if (file.absolutePath == path) {
                toReplace.addElement(holder)
            }
        }
        toReplace.forEach {
            guiHandler.openFiles.remove(it.f)

            it.tab.text = newName
            val holder = OpenFileHolder(project.fileManager.projectFiles[newName]!!, it.name, it.tab, it.tabPane, it.borderPane, it.area,  coreManager, it.codeManager)
            guiHandler.openFiles.put(project.fileManager.projectFiles[newName]!!, holder)
            eventManager.registerEventsForNewFile(holder)
        }
        toReplace.clear()
        System.gc()
        eventManager.updateProjectFilesTreeView()
    }

    fun delete(f: File) {

        val toReplace = Vector<File>()
        for ((file, _) in guiHandler.openFiles) {
            if (file.absolutePath == f.absolutePath)
                toReplace.addElement(file)
        }
        toReplace.forEach {
            val value = guiHandler.openFiles.remove(it)
            value?.tabPane?.tabs?.remove(value.tab)
        }
        project.fileManager.deleteFile(f.name)
        eventManager.updateProjectFilesTreeView()
        System.gc()
    }

}