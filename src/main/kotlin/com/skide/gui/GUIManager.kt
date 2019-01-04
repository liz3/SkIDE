package com.skide.gui

import com.skide.Info
import com.skide.core.management.ConfigManager
import com.skide.gui.controllers.AboutController
import com.skide.include.ActiveWindow
import com.skide.utils.setIcon
import javafx.application.Application
import javafx.application.Platform
import javafx.concurrent.Worker.State
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.layout.BorderPane
import javafx.scene.web.WebView
import javafx.stage.Stage
import javafx.stage.StageStyle
import java.awt.Desktop
import java.net.URI
import java.util.*


object GUIManager {

    lateinit var settings: ConfigManager

    val closingHooks = Vector<() -> Unit>()


    val activeGuis: HashMap<Int, ActiveWindow> = HashMap()
    var idCounter = 0

    fun getWindow(fxFilePath: String, name: String, show: Boolean, stage: Stage = Stage(), w: Double = -1.0, h: Double = -1.0): ActiveWindow {
        idCounter++
        stage.title = name
        val loader = FXMLLoader()
        val rootNode: Parent = loader.load<Parent>(javaClass.getResourceAsStream("/$fxFilePath"))
        val controller = loader.getController<Any>()
        stage.icons.add(Image(javaClass.getResource("/images/icon.png").toExternalForm()))
        val scene = if (w != -1.0)
            Scene(rootNode, w, h)
        else
            Scene(rootNode)


        scene.stylesheets.add(settings.getCssPath("Reset.css"))
        if (settings.get("theme") == "Dark") scene.stylesheets.add(settings.getCssPath("ThemeDark.css"))
        if(GUIManager.settings.get("global_font_size").toString().isNotEmpty())
            rootNode.style = "-fx-font-size: ${GUIManager.settings.get("global_font_size")}px"

        stage.scene = scene
        stage.sizeToScene()
        if (show) stage.show()
        val window = ActiveWindow(stage, scene, loader, controller, idCounter)
        activeGuis[idCounter] = window
        return window
    }

    fun getScene(fxFilePath: String): Pair<Parent, Any> {
        val loader = FXMLLoader()
        val rootNode: Parent = loader.load<Parent>(javaClass.getResourceAsStream("/$fxFilePath"))
        rootNode.stylesheets.add(settings.getCssPath("Reset.css"))
        if (GUIManager.settings.get("theme") == "Dark")
            rootNode.stylesheets.add(GUIManager.settings.getCssPath("ThemeDark.css"))
        val controller = loader.getController<Any>()
        if(GUIManager.settings.get("global_font_size").toString().isNotEmpty())
          rootNode.style = "-fx-font-size: ${GUIManager.settings.get("global_font_size")}px"


        return Pair(rootNode, controller)
    }

    var bootstrapCallback: (Stage) -> Unit = { }

    fun showAbout() {

        val win = getWindow("fxml/About.fxml", "About", false)
        win.stage.isResizable = false
        val controller = win.controller as AboutController

        controller.discordBtn.setIcon("discord", 35.0, 35.0)
        controller.gitlabBtn.setIcon("gitlab", 35.0, 35.0, false)
        controller.donateBtn.setIcon("donate", 35.0, 35.0)
        controller.imageView.image = Image(javaClass.getResource("/images/icon.png").toExternalForm())
        controller.versionLabel.text = Info.version
        controller.okBtn.setOnAction {
            win.stage.close()
        }
        controller.discordBtn.setOnAction { Desktop.getDesktop().browse(URI("https://discord.gg/Ud2WdVU")) }
        controller.gitlabBtn.setOnAction { Desktop.getDesktop().browse(URI("https://gitlab.com/sk-ide/SkIDE/issues")) }
        controller.donateBtn.setOnAction { Desktop.getDesktop().browse(URI("https://paypal.me/liz3de")) }
        controller.infoTextLabel.text = "Developed and maintained by Liz3 aka 21 Xayah\nContributors: NickAc, 4rno, BaeFell, NanoDankster, Nicofisi, Scrumplex"
        win.stage.show()
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

        Desktop.getDesktop().browse(URI(link.toString()))
    }
}

class JavaFXBootstrapper : Application() {
    //Call the method with the primary created stage
    override fun start(primaryStage: Stage) {

        GUIManager.bootstrapCallback(primaryStage)

    }

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