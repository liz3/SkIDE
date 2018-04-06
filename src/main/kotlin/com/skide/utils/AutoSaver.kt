package com.skide.utils

import com.skide.CoreManager

class AutoSaver(coreManager: CoreManager) {

    init {
        val thread = Thread {
            Thread.sleep(25000)
            coreManager.projectManager.openProjects.forEach {
                it.guiHandler.openFiles.forEach { f ->
                    f.value.saveCode()
                }
            }

        }
        thread.name = "Sk-IDE Auto Save Thread"
        thread.start()
    }
}