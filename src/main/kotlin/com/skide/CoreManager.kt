package com.skide

import com.skide.core.debugger.Debugger
import com.skide.core.management.*
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
import javafx.util.Duration
import org.controlsfx.control.Notifications


class CoreManager {

    val guiManager = GUIManager
    lateinit var debugger: Debugger
    lateinit var configManager: ConfigManager
    lateinit var projectManager: ProjectManager
    lateinit var serverManager: ServerManager
    lateinit var resourceManager: ResourceManager
    lateinit var googleAnalytics: GoogleAnalytics
    lateinit var saver: AutoSaver
    lateinit var sockServer: SocketManager
    lateinit var snippetManager: SnippetManager
    lateinit var schemesManager: SchemesManager
    lateinit var skUnity: SkUnity

    private var debugLevel = DebugLevel.INFORMATION

    private fun initialize(args: Array<String>) {
        args.forEach {
            //first lets set the debug level
            if (it.startsWith("--debug")) {
                debugLevel = DebugLevel.valueOf(it.split("=")[1].toUpperCase())
            }
        }
        if (System.getProperty("skide.mode") != null && System.getProperty("skide.mode") == "prod")
            Info.prodMode = true
    }

    private fun setupInstances(me: CoreManager) {
        configManager = ConfigManager(me)
        projectManager = ProjectManager(me)
        serverManager = ServerManager(me)
        resourceManager = ResourceManager(me)
        saver = AutoSaver(me)
        skUnity = SkUnity(me)
        sockServer = SocketManager(me)
        snippetManager = SnippetManager(me)
        schemesManager = SchemesManager(me)
        debugger.syserr.core = me
        sockServer.start()
    }

    fun googleAnalyticsSetup(me: CoreManager): Boolean {
        var r = false
        googleAnalytics = GoogleAnalytics(me)
        if (configManager.get("analytics") == "") {
            r = true
            configManager.set("analytics", "true")
        } else {
            if (configManager.get("analytics") == "true" && Info.prodMode) {
                googleAnalytics.start()
            }
        }
        return r
    }

    private fun welcomeWindow(me: CoreManager, configLoadResult: ConfigLoadResult) {

        val window = guiManager.getWindow("fxml/StartGui.fxml", "SkIDE ${Info.version}", false)
        (window.controller as StartGUIController).initGui(me, window, configLoadResult == ConfigLoadResult.FIRST_RUN)
        window.stage.isResizable = false
        window.stage.show()
    }

    fun bootstrap(args: Array<String>) {

        debugger = Debugger()

        val me = this
        guiManager.bootstrapCallback = { stage ->
            val loader = FXMLLoader()
            val parent = loader.load<Pane>(javaClass.getResourceAsStream("/fxml/LoadingGui.fxml"))
            parent.background = Background.EMPTY
            val controller = loader.getController<SplashGuiController>()
            stage.scene = Scene(parent)
            stage.initStyle(StageStyle.TRANSPARENT)
            stage.icons.add(Image(javaClass.getResource("/images/icon.png").toExternalForm()))
            stage.scene.fill = Color.TRANSPARENT
            stage.sizeToScene()
            stage.centerOnScreen()
            stage.isResizable = false
            controller.view.image = Image(javaClass.getResource("/images/splash.png").toExternalForm())
            controller.logoView.image = Image(javaClass.getResource("/images/21xayah.png").toExternalForm())
            stage.show()
            var analyticInf: Boolean
            val task = object : Task<Void>() {
                @Throws(Exception::class)
                override fun call(): Void? {
                    try {
                        updateMessage("Initializing...")
                        updateProgress(0.0, 100.0)

                        initialize(args)

                        setupInstances(me)
                        updateProgress(5.0, 100.0)
                        updateMessage("Loading Config...")
                        val configLoadResult = configManager.load()
                        if (configLoadResult == ConfigLoadResult.ERROR) return null
                        GUIManager.settings = configManager
                        snippetManager.prepare()
                        schemesManager.prepare()
                        updateProgress(25.0, 100.0)
                        updateMessage("Checking skUnity access...")
                        skUnity.load()
                        updateProgress(35.0, 100.0)
                        updateMessage("Initializing server manager")
                        serverManager.init()
                        updateProgress(40.0, 100.0)
                        updateMessage("Downloading latest Resources")
                        resourceManager.loadResources { _, current, name ->
                            val amount = current * 5
                            updateProgress(50.0 + amount, 100.0)
                            updateMessage(name)
                        }
                        updateProgress(75.0, 100.0)
                        updateMessage("Starting insights")

                        updateProgress(96.0, 100.0)
                        updateMessage("Starting gui...")
                        Prompts.theme = (configManager.get("theme") as String)
                        Prompts.configManager = configManager

                        Platform.runLater {
                            analyticInf = googleAnalyticsSetup(me)
                            stage.close()
                            welcomeWindow(me, configLoadResult)
                            if (analyticInf) {
                                Notifications.create()
                                        .title("Analytics")
                                        .text("Sk-IDE is collecting: When you start the IDE and when you open a Project(ANY INFORMATION ABOUT THE PROJECT IS NOT INCLUDED). I do this only for statistics. If you still don´t want it, disable it in the Settings!").darkStyle().hideAfter(Duration.INDEFINITE)
                                        .show()
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
        handle(if (args.isNotEmpty()) args.first() else "")
        if (Platform.isFxApplicationThread()) GUIManager.bootstrapCallback(Stage()) else JavaFXBootstrapper.bootstrap()
    }
}
