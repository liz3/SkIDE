package com.skide

import com.skide.core.management.ConfigLoadResult
import com.skide.core.management.ConfigManager
import com.skide.core.management.ProjectManager
import com.skide.core.management.ServerManager
import com.skide.gui.GUIManager
import com.skide.gui.JavaFXBootstrapper
import com.skide.gui.Prompts
import com.skide.gui.controllers.SplashGuiController
import com.skide.gui.controllers.StartGUIController
import com.skide.utils.*
import javafx.application.Platform
import javafx.concurrent.Task
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.layout.Background
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.stage.Stage
import javafx.stage.StageStyle
import java.lang.management.ManagementFactory


class CoreManager {


    val guiManager = GUIManager
    lateinit var configManager: ConfigManager
    lateinit var projectManager: ProjectManager
    lateinit var serverManager: ServerManager
    lateinit var resourceManager: ResourceManager
    lateinit var saver: AutoSaver
    lateinit var skUnity: SkUnity

    private var debugLevel = DebugLevel.INFORMATION

    fun bootstrap(args: Array<String>) {
        val me = this
        guiManager.bootstrapCallback = { stage ->
            val loader = FXMLLoader()
            val parent = loader.load<Pane>(javaClass.getResourceAsStream("/LoadingGui.fxml"))
            parent.background = Background.EMPTY
            val controller = loader.getController<SplashGuiController>()
            stage.scene = Scene(parent)
            stage.initStyle(StageStyle.TRANSPARENT)
            stage.scene.fill = Color.TRANSPARENT
            stage.sizeToScene()
            stage.isResizable = false
            stage.isAlwaysOnTop = true
            controller.view.image = Image(javaClass.getResource("/splash.png").toExternalForm())
            stage.show()
            val task = object : Task<Void>() {
                @Throws(Exception::class)
                override fun call(): Void? {
                    try {
                        Thread.sleep(250)
                        updateMessage("Initializing...")
                        updateProgress(0.0, 100.0)
                        args.forEach {
                            //first lets set the debug level
                            if (it.startsWith("--debug")) {
                                debugLevel = DebugLevel.valueOf(it.split("=")[1].toUpperCase())
                            }
                        }
                        configManager = ConfigManager(me)
                        projectManager = ProjectManager(me)
                        serverManager = ServerManager(me)
                        resourceManager = ResourceManager(me)
                        saver = AutoSaver(me)
                        skUnity = SkUnity(me)
                        updateProgress(5.0, 100.0)
                        updateMessage("Loading Config...")
                        GUIManager.settings = configManager
                        val configLoadResult = configManager.load()
                        if (configLoadResult == ConfigLoadResult.ERROR) return null
                        updateProgress(25.0, 100.0)
                        updateMessage("Checking skUnity access...")
                        skUnity.load()
                        updateProgress(35.0, 100.0)
                        updateMessage("Initializing server manager")
                        serverManager.init()
                        updateProgress(50.0, 100.0)
                        updateMessage("Downloading latest Resources")

                        resourceManager.loadResources({ total, current, name ->
                            val amount = current * 5;
                            updateProgress(50.0 + amount, 100.0)
                            updateMessage(name)
                        })
                        updateProgress(90.0, 100.0)
                        updateMessage("Starting gui...")
                        Prompts.theme = configManager.get("theme") as String
                        attachDebugger()
                        Platform.runLater {
                            stage.close()
                            GUIManager.discord.update("In the main menu", "Idle")
                            val window = guiManager.getWindow("StartGui.fxml", "Sk-IDE", false, Stage())
                            stage.isResizable = false
                            (window.controller as StartGUIController).initGui(me, window, configLoadResult == ConfigLoadResult.FIRST_RUN)
                            window.stage.show()
                        }
                    }catch (e:Exception) {
                        e.printStackTrace()
                    }
                    return null
                }
            }
            controller.label.textProperty().bind(task.messageProperty())
            controller.progressBar.progressProperty().bind(task.progressProperty())

            val thread = Thread(task)
            thread.isDaemon = true
            thread.name = "Sk-IDE loader task"
            thread.start()
        }
        if (Platform.isFxApplicationThread()) {
            GUIManager.bootstrapCallback(Stage())
        } else {
            JavaFXBootstrapper.bootstrap()
        }
    }

    fun attachDebugger() {
        val id = ManagementFactory.getRuntimeMXBean().name.split("@").first()
        println(id)
    }
}
