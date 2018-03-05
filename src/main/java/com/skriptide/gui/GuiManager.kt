package com.skriptide.gui

import com.skriptide.include.ActiveWindow
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage

object GuiManager {

    val activeGuis: HashMap<Int, ActiveWindow> = HashMap()
    var idCounter = 0

    fun getWindow(fxFilePath:String, name:String, show:Boolean,  stage:Stage = Stage()): ActiveWindow {

        idCounter++
        stage.title = name

        val loader = FXMLLoader()
        val rootNode:Parent = loader.load<Parent>(javaClass.getResourceAsStream("/$fxFilePath"))
        val controller = loader.getController<Any>()

        val scene = Scene(rootNode)
        stage.scene = scene
        stage.sizeToScene()

        if(show)stage.show()

        val window = ActiveWindow(stage, scene, loader, controller, idCounter)
        activeGuis.put(idCounter, window)
        return window
    }

    var bootstrapCallback : (Stage) -> Unit = { stage ->


    }

    fun closeGui(id:Int) {
        val window = activeGuis[id]

        if(window != null) {
            window.stage.close()
            activeGuis.remove(id)
        }
    }

}
class JavaFXBootstrapper : Application(){
    //Call the method with the primary created stage
    override fun start(primaryStage: Stage) = GuiManager.bootstrapCallback(primaryStage)

    //Companion Object is required to kick off JavaFX with Kotlin
    companion object { fun bootstrap() = launch(JavaFXBootstrapper::class.java) }
}