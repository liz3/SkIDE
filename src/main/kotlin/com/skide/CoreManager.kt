package com.skide

import com.skide.core.management.ConfigLoadResult
import com.skide.core.management.ConfigManager
import com.skide.core.management.ProjectManager
import com.skide.core.management.ServerManager
import com.skide.gui.GUIManager
import com.skide.gui.JavaFXBootstrapper
import com.skide.gui.controllers.StartGUIController
import com.skide.utils.*


class CoreManager {

    val configManager = ConfigManager(this)
    val projectManager = ProjectManager(this)
    val serverManager = ServerManager(this)
    val resourceManager = ResourceManager(this)
    val guiManager = GUIManager
    val saver = AutoSaver(this)
    val skUnity = SkUnity(this)
    private var debugLevel = DebugLevel.INFORMATION


    fun bootstrap(args: Array<String>) {
        //Bootstrap everything
        args.forEach {
            //first lets set the debug level
            if (it.startsWith("--debug")) {
                debugLevel = DebugLevel.valueOf(it.split("=")[1].toUpperCase())
            }
        }
        //load config
        val configLoadResult = configManager.load()
        skUnity.load()
        //Check if config load failed
        if (configLoadResult == ConfigLoadResult.ERROR)
        //TODO handle ERROR
            return

        resourceManager.loadResources()
        serverManager.init()

        guiManager.bootstrapCallback = { stage ->

            GUIManager.discord.update("In the main menu", "Idle")

            val window = guiManager.getWindow("StartGui.fxml", "Sk-IDE", false, stage)
            stage.isResizable = false
            val controller = window.controller as StartGUIController
            controller.initGui(this, window, configLoadResult == ConfigLoadResult.FIRST_RUN)
            window.stage.show()


        }

        //Launch the JavaFX Process Thread
        JavaFXBootstrapper.bootstrap()

    }
}
