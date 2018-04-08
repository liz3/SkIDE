package com.skide

import com.skide.core.debugger.Debugger
import com.skide.core.management.*
import com.skide.gui.GUIManager
import com.skide.gui.JavaFXBootstrapper
import com.skide.gui.Prompts
import com.skide.gui.controllers.SplashGuiController
import com.skide.gui.controllers.StartGUIController
import com.skide.skriptinsight.client.SkriptInsightClient
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
    lateinit var debugger: Debugger
    lateinit var configManager: ConfigManager
    lateinit var projectManager: ProjectManager
    lateinit var serverManager: ServerManager
    lateinit var insightClient: SkriptInsightClient
    lateinit var insightsManager: InsightsManager
    lateinit var resourceManager: ResourceManager
    lateinit var saver: AutoSaver
    lateinit var sockServer: SocketManager
    lateinit var skUnity: SkUnity

    private var debugLevel = DebugLevel.INFORMATION

    fun bootstrap(args: Array<String>) {

        debugger = Debugger()

        val me = this
        guiManager.bootstrapCallback = { stage ->
            val loader = FXMLLoader()
            val parent = loader.load<Pane>(javaClass.getResourceAsStream("/LoadingGui.fxml"))
            parent.background = Background.EMPTY
            val controller = loader.getController<SplashGuiController>()
            stage.scene = Scene(parent)
            stage.initStyle(StageStyle.TRANSPARENT)
            stage.icons.add(Image(javaClass.getResource("/icon.png").toExternalForm()))
            stage.scene.fill = Color.TRANSPARENT
            stage.sizeToScene()
            stage.centerOnScreen()
            stage.isResizable = false
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
                        insightsManager = InsightsManager(me)
                        sockServer = SocketManager(me)
                        insightClient = SkriptInsightClient(me)

                        sockServer.start()
                        debugger.syserr.core = me

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

                        resourceManager.loadResources({ _, current, name ->
                            val amount = current * 5;
                            updateProgress(50.0 + amount, 100.0)
                            updateMessage(name)
                        })


                        updateProgress(80.0, 100.0)
                        updateMessage("Starting insights")
                        insightsManager.setup {
                            updateProgress(96.0, 100.0)
                            updateMessage("Starting gui...")
                            Prompts.theme = configManager.get("theme") as String
                            Prompts.configManager = configManager
                            attachDebugger()
                            Platform.runLater {
                                stage.close()
                                GUIManager.discord.update("In the main menu", "Idle")
                                val window = guiManager.getWindow("StartGui.fxml", "Sk-IDE", false, Stage())
                                stage.isResizable = false
                                (window.controller as StartGUIController).initGui(me, window, configLoadResult == ConfigLoadResult.FIRST_RUN)
                                window.stage.centerOnScreen()
                                window.stage.isResizable = false
                                window.stage.show()

                            }
                        }


                    } catch (e: Exception) {
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



        handle(if (args.size >= 1) args.first() else "")

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
