package com.skriptide.core.management

import com.skriptide.CoreManager
import com.skriptide.gui.project.OpenProjectGuiManager
import com.skriptide.include.Project

class OpenProject(val project:Project, val coreManager:CoreManager) {

    val guiHandler = OpenProjectGuiManager(this)
    val eventManager = guiHandler.startGui()


    //TODO missing param for which file
    fun runFile() {

    }

    fun createNewFile(name:String) {
        project.fileManager.addFile(name)
        eventManager.updateProjectFilesTreeView()
    }
}