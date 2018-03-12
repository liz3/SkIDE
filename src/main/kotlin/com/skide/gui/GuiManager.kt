package com.skide.gui

import com.skide.include.ActiveWindow
import javafx.application.Application
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.layout.BorderPane
import javafx.scene.web.WebView
import javafx.stage.Stage
import java.awt.Desktop
import java.net.URI
import javafx.concurrent.Worker.State
import javafx.scene.control.Alert
import netscape.javascript.JSObject


object GuiManager {

    val activeGuis: HashMap<Int, ActiveWindow> = HashMap()
    var idCounter = 0

    fun getWindow(fxFilePath: String, name: String, show: Boolean, stage: Stage = Stage()): ActiveWindow {

        idCounter++
        stage.title = name


        val loader = FXMLLoader()
        val rootNode: Parent = loader.load<Parent>(javaClass.getResourceAsStream("/$fxFilePath"))
        val controller = loader.getController<Any>()

        val scene = Scene(rootNode)
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
        view.engine.loadWorker.stateProperty().addListener { observable, oldValue, newValue ->

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
    override fun start(primaryStage: Stage) = GuiManager.bootstrapCallback(primaryStage)

    //Companion Object is required to kick off JavaFX with Kotlin
    companion object {
        fun bootstrap() = launch(JavaFXBootstrapper::class.java)
    }
}