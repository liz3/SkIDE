package com.skide.gui

import com.skide.core.management.ConfigManager
import com.skide.include.ActiveWindow
import com.skide.utils.Discord
import javafx.application.Application
import javafx.concurrent.Worker.State
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.image.Image
import javafx.scene.layout.BorderPane
import javafx.scene.web.WebView
import javafx.stage.Stage
import netscape.javascript.JSObject
import java.awt.Desktop
import java.net.URI
import java.util.*


object GUIManager {

    lateinit var settings: ConfigManager

    val closingHooks = Vector<() -> Unit>()

    val discord = Discord()

    val activeGuis: HashMap<Int, ActiveWindow> = HashMap()
    var idCounter = 0

    fun getWindow(fxFilePath: String, name: String, show: Boolean, stage: Stage = Stage()): ActiveWindow {

        idCounter++
        stage.title = name


        val loader = FXMLLoader()
        val rootNode: Parent = loader.load<Parent>(javaClass.getResourceAsStream("/$fxFilePath"))
        val controller = loader.getController<Any>()
        stage.icons.add(Image(javaClass.getResource("/icon.png").toExternalForm()))
        val scene = Scene(rootNode)
        scene.stylesheets.add(settings.getCssPath("Reset.css"))
        if (settings.get("theme") == "Dark") {
            scene.stylesheets.add(settings.getCssPath("ThemeDark.css"))
        }
        stage.scene = scene
        stage.sizeToScene()

        if (show) stage.show()

        val window = ActiveWindow(stage, scene, loader, controller, idCounter)
        activeGuis.put(idCounter, window)
        return window
    }

    fun getScene(fxFilePath: String): Pair<Parent, Any> {

        val loader = FXMLLoader()
        val rootNode: Parent = loader.load<Parent>(javaClass.getResourceAsStream("/$fxFilePath"))
        val controller = loader.getController<Any>()


        return Pair(rootNode, controller)
    }

    var bootstrapCallback: (Stage) -> Unit = { _ ->


    }

    fun showAbout() {


        val stage = Stage()
        val pane = BorderPane()
        val view = WebView()
        pane.center = view
        stage.isResizable = false
        stage.title = "About"
        stage.scene = Scene(pane, 800.0, 450.0)

        view.setOnMouseReleased {

        }
        view.engine.loadWorker.stateProperty().addListener { _, _, newValue ->

            if (newValue === State.SUCCEEDED) {
                val win = view.engine.executeScript("window") as JSObject
                val instance = LinkOpener()
                win.setMember("skide", instance)
                stage.show()
                Prompts.infoCheck("Attention", "reopen if links dont work!", "If you want to open a link, but it does not work, restart about!", Alert.AlertType.INFORMATION)

            }
        }
        view.engine.load("https://liz3.net/sk/about/")


    }

    fun closeGui(id: Int) {
        val window = activeGuis[id]

        if (window != null) {
            window.stage.close()
            activeGuis.remove(id)
        }
    }

}

class LinkOpener {

    fun open(link: Any) {

        println("called")
        Desktop.getDesktop().browse(URI(link.toString()))
    }
}

class JavaFXBootstrapper : Application() {
    //Call the method with the primary created stage
    override fun start(primaryStage: Stage) = GUIManager.bootstrapCallback(primaryStage)

    override fun stop() {

        GUIManager.closingHooks.forEach {
            it()
        }
        System.exit(0)
    }

    //Companion Object is required to kick off JavaFX with Kotlin
    companion object {
        fun bootstrap() = launch(JavaFXBootstrapper::class.java)
    }
}