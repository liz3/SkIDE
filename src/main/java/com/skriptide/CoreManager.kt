package com.skriptide

import com.skriptide.core.management.ConfigLoadResult
import com.skriptide.core.management.ConfigManager
import com.skriptide.core.management.ProjectManager
import com.skriptide.core.management.ServerManager
import com.skriptide.gui.GuiManager
import com.skriptide.gui.JavaFXBootstrapper
import com.skriptide.gui.controllers.StartGuiController
import com.skriptide.utils.DebugLevel
import org.fxmisc.richtext.CodeArea


class CoreManager {

    val configManager = ConfigManager(this)
    val projectManager = ProjectManager(this)
    val serverManager = ServerManager(this)
    val guiManager = GuiManager
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

        //Check if config load failed
        if(configLoadResult == ConfigLoadResult.ERROR) {
            //TODO handle ERROR
            return
        }



         guiManager.bootstrapCallback = {stage ->


            val window = guiManager.getWindow("StartGui.fxml", "Welcome to SkIde", false, stage)
            val controller = window.controller as StartGuiController
            controller.initGui(this, window, configLoadResult == ConfigLoadResult.FIRST_RUN)
            window.stage.show()


        }


        //Launch the JavaFX Process Thread
        JavaFXBootstrapper.bootstrap()

    }
}
